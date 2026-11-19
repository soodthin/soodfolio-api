package com.soodfolio.api.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soodfolio.api.dto.PortfolioResponseDto;
import com.soodfolio.api.dto.ShareLinkDtos.*;
import com.soodfolio.api.entity.Portfolio;
import com.soodfolio.api.entity.PortfolioSnapshot;
import com.soodfolio.api.entity.ShareLink;
import com.soodfolio.api.entity.ShareLinkTelemetry;
import com.soodfolio.api.exception.BadRequestException;
import com.soodfolio.api.exception.ForbiddenException;
import com.soodfolio.api.exception.ResourceNotFoundException;
import com.soodfolio.api.exception.ShareLinkExpiredException;
import com.soodfolio.api.repository.PortfolioRepository;
import com.soodfolio.api.repository.PortfolioSnapshotRepository;
import com.soodfolio.api.repository.ShareLinkRepository;
import com.soodfolio.api.repository.ShareLinkTelemetryRepository;
import com.soodfolio.api.service.ShareLinkService;
import com.soodfolio.api.service.SnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShareLinkServiceImpl implements ShareLinkService {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioSnapshotRepository snapshotRepository;
    private final ShareLinkRepository shareLinkRepository;
    private final ShareLinkTelemetryRepository telemetryRepository;
    private final SnapshotService snapshotService;
    private final ObjectMapper objectMapper;

    private static final String TOKEN_PREFIX = "st_live_";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShareLinkResponse createShareLink(UUID userId, CreateShareLinkRequest request) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "userId", userId));

        // Get latest snapshot or auto-create version 1 if none exists
        PortfolioSnapshot snapshot = snapshotRepository.findTopByPortfolioIdOrderByVersionDesc(portfolio.getId())
                .orElseGet(() -> {
                    log.info("No snapshots found for portfolio {}, auto-generating initial snapshot v1", portfolio.getId());
                    snapshotService.publishSnapshot(userId, "Initial auto-generated snapshot for targeted link");
                    return snapshotRepository.findTopByPortfolioIdOrderByVersionDesc(portfolio.getId())
                            .orElseThrow(() -> new IllegalStateException("Failed to retrieve created snapshot"));
                });

        // Generate cryptographically secure token
        byte[] randomBytes = new byte[24];
        new SecureRandom().nextBytes(randomBytes);
        String rawToken = TOKEN_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String tokenHash = hashToken(rawToken);

        ShareLink shareLink = ShareLink.builder()
                .portfolio(portfolio)
                .snapshot(snapshot)
                .tokenHash(tokenHash)
                .companyName(request.getCompanyName().trim())
                .slugAlias(request.getSlugAlias() != null && !request.getSlugAlias().isBlank() ? request.getSlugAlias().trim() : null)
                .customLayout(request.getCustomLayout() != null && !request.getCustomLayout().isBlank() ? request.getCustomLayout() : "CINEMATIC_DECK")
                .allowedSlideIds(request.getAllowedSlideIds() != null ? new ArrayList<>(request.getAllowedSlideIds()) : new ArrayList<>())
                .expiresAt(request.getExpiresAt())
                .maxViews(request.getMaxViews())
                .viewCount(0)
                .isRevoked(false)
                .build();

        ShareLink saved = shareLinkRepository.save(shareLink);

        return ShareLinkResponse.builder()
                .id(saved.getId())
                .companyName(saved.getCompanyName())
                .slugAlias(saved.getSlugAlias())
                .customLayout(saved.getCustomLayout())
                .allowedSlideIds(saved.getAllowedSlideIds())
                .expiresAt(saved.getExpiresAt())
                .maxViews(saved.getMaxViews())
                .viewCount(saved.getViewCount())
                .isRevoked(saved.isRevoked())
                .createdAt(saved.getCreatedAt())
                .rawToken(rawToken)
                .shareUrl("/p/" + rawToken)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShareLinkSummaryResponse> getShareLinks(UUID userId) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "userId", userId));

        return shareLinkRepository.findByPortfolioIdOrderByCreatedAtDesc(portfolio.getId())
                .stream()
                .map(link -> ShareLinkSummaryResponse.builder()
                        .id(link.getId())
                        .companyName(link.getCompanyName())
                        .slugAlias(link.getSlugAlias())
                        .customLayout(link.getCustomLayout())
                        .allowedSlideIds(link.getAllowedSlideIds())
                        .expiresAt(link.getExpiresAt())
                        .maxViews(link.getMaxViews())
                        .viewCount(link.getViewCount())
                        .isRevoked(link.isRevoked())
                        .createdAt(link.getCreatedAt())
                        .build())
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeShareLink(UUID userId, UUID linkId) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "userId", userId));

        ShareLink link = shareLinkRepository.findByIdAndPortfolioId(linkId, portfolio.getId())
                .orElseThrow(() -> new ResourceNotFoundException("ShareLink", "id", linkId));

        link.setRevoked(true);
        shareLinkRepository.save(link);
    }

    @Override
    @Transactional(readOnly = true)
    public TelemetryAnalyticsResponse getLinkAnalytics(UUID userId, UUID linkId) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "userId", userId));

        ShareLink link = shareLinkRepository.findByIdAndPortfolioId(linkId, portfolio.getId())
                .orElseThrow(() -> new ResourceNotFoundException("ShareLink", "id", linkId));

        List<ShareLinkTelemetry> telemetries = telemetryRepository.findByShareLinkId(link.getId());

        long totalDwellSeconds = telemetries.stream()
                .mapToLong(ShareLinkTelemetry::getDwellSeconds)
                .sum();

        // Group by slideKey to compute stats
        Map<String, List<ShareLinkTelemetry>> bySlide = telemetries.stream()
                .collect(Collectors.groupingBy(ShareLinkTelemetry::getSlideKey));

        List<SlideTelemetryStatDto> slideStats = bySlide.entrySet().stream()
                .map(entry -> {
                    String slideKey = entry.getKey();
                    List<ShareLinkTelemetry> items = entry.getValue();
                    long sumDwell = items.stream().mapToLong(ShareLinkTelemetry::getDwellSeconds).sum();
                    long count = items.size();
                    double avg = count > 0 ? (double) sumDwell / count : 0.0;
                    return SlideTelemetryStatDto.builder()
                            .slideKey(slideKey)
                            .totalDwellSeconds(sumDwell)
                            .viewCount(count)
                            .avgDwellSeconds(Math.round(avg * 10.0) / 10.0)
                            .build();
                })
                .sorted(Comparator.comparingLong(SlideTelemetryStatDto::getTotalDwellSeconds).reversed())
                .toList();

        // Device breakdown
        Map<String, Long> deviceBreakdown = telemetries.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getDeviceType() != null ? t.getDeviceType() : "UNKNOWN",
                        Collectors.counting()
                ));

        LocalDateTime lastViewedAt = telemetryRepository
                .findTopByShareLinkIdOrderByCreatedAtDesc(link.getId())
                .map(ShareLinkTelemetry::getCreatedAt)
                .orElse(null);

        return TelemetryAnalyticsResponse.builder()
                .shareLinkId(link.getId())
                .companyName(link.getCompanyName())
                .totalViews(link.getViewCount())
                .totalDwellSeconds(totalDwellSeconds)
                .lastViewedAt(lastViewedAt)
                .slideStats(slideStats)
                .deviceBreakdown(deviceBreakdown)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PublicDeckResponse getPublicDeck(String rawToken) {
        String tokenHash = hashToken(rawToken);

        ShareLink link = shareLinkRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResourceNotFoundException("ShareLink", "token", "invalid"));

        if (link.isRevoked()) {
            throw new ForbiddenException("Link truy cập đã bị thu hồi bởi ứng viên.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (link.getExpiresAt() != null && link.getExpiresAt().isBefore(now)) {
            throw new ShareLinkExpiredException("Link ứng tuyển đã hết hạn truy cập.");
        }

        if (link.getMaxViews() != null && link.getViewCount() >= link.getMaxViews()) {
            throw new ShareLinkExpiredException("Link ứng tuyển đã đạt giới hạn lượt xem tối đa.");
        }

        // Increment view count atomically
        int updated = shareLinkRepository.incrementViewCountIfAllowed(link.getId(), now);
        if (updated == 0) {
            throw new ShareLinkExpiredException("Link ứng tuyển đã hết hạn hoặc đạt giới hạn lượt xem tối đa.");
        }

        PortfolioResponseDto dto = deserializeSnapshot(link.getSnapshot().getSnapshotData());

        // Apply slide filtering if allowedSlideIds are defined
        List<String> allowedSlideIds = link.getAllowedSlideIds();
        if (allowedSlideIds != null && !allowedSlideIds.isEmpty()) {
            filterAllowedSlides(dto, allowedSlideIds);
        }

        return PublicDeckResponse.builder()
                .companyName(link.getCompanyName())
                .customLayout(link.getCustomLayout())
                .snapshotVersion(link.getSnapshot().getVersion())
                .portfolio(dto)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordTelemetry(String rawToken, TelemetryBatchRequest request, String userAgent) {
        String tokenHash = hashToken(rawToken);

        ShareLink link = shareLinkRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResourceNotFoundException("ShareLink", "token", "invalid"));

        if (link.isRevoked() || (link.getExpiresAt() != null && link.getExpiresAt().isBefore(LocalDateTime.now()))) {
            log.warn("Attempt to record telemetry on revoked or expired share link {}", link.getId());
            return;
        }

        if (request.getEvents() == null || request.getEvents().isEmpty()) {
            return;
        }

        Set<String> allowedDevices = Set.of("DESKTOP", "TABLET", "MOBILE");

        List<ShareLinkTelemetry> telemetries = request.getEvents().stream()
                .map(event -> {
                    int clampedDwellSeconds = Math.min(3600, Math.max(0, event.getDwellSeconds()));
                    String rawDevice = event.getDeviceType() != null ? event.getDeviceType().trim().toUpperCase() : "UNKNOWN";
                    String deviceType = allowedDevices.contains(rawDevice) ? rawDevice : "UNKNOWN";

                    return ShareLinkTelemetry.builder()
                            .shareLink(link)
                            .sessionId(request.getSessionId())
                            .slideKey(event.getSlideKey())
                            .dwellSeconds(clampedDwellSeconds)
                            .deviceType(deviceType)
                            .userAgent(event.getUserAgent() != null ? event.getUserAgent() : userAgent)
                            .build();
                })
                .toList();

        telemetryRepository.saveAll(telemetries);
    }

    private void filterAllowedSlides(PortfolioResponseDto dto, List<String> allowedSlideIds) {
        boolean specifiesGeneralSlides = allowedSlideIds.stream()
                .anyMatch(s -> s.equalsIgnoreCase("experience") || s.equalsIgnoreCase("education")
                        || s.equalsIgnoreCase("skills") || s.equalsIgnoreCase("contact") || s.equalsIgnoreCase("projects")
                        || s.startsWith("project-") || s.startsWith("exp-"));

        if (specifiesGeneralSlides) {
            dto.setShowExperience(allowedSlideIds.stream().anyMatch(s -> s.equalsIgnoreCase("experience") || s.startsWith("exp-")));
            dto.setShowEducation(allowedSlideIds.stream().anyMatch(s -> s.equalsIgnoreCase("education")));
            dto.setShowSkills(allowedSlideIds.stream().anyMatch(s -> s.equalsIgnoreCase("skills")));
            dto.setShowContact(allowedSlideIds.stream().anyMatch(s -> s.equalsIgnoreCase("contact")));
            dto.setShowProjects(allowedSlideIds.stream().anyMatch(s -> s.equalsIgnoreCase("projects") || s.startsWith("project-")));
        }

        // Filter projects by ID if project IDs are specified in allowedSlideIds
        List<String> targetProjectIds = allowedSlideIds.stream()
                .map(s -> s.startsWith("project-") ? s.substring("project-".length()) : s)
                .toList();

        if (dto.getProjects() != null && !dto.getProjects().isEmpty()) {
            boolean hasSpecificMatch = dto.getProjects().stream()
                    .anyMatch(p -> p.getId() != null && targetProjectIds.contains(p.getId().toString()));

            if (hasSpecificMatch) {
                dto.setProjects(dto.getProjects().stream()
                        .filter(p -> p.getId() != null && targetProjectIds.contains(p.getId().toString()))
                        .toList());
            }
        }

        // Filter experiences by ID if experience IDs are specified in allowedSlideIds
        List<String> targetExperienceIds = allowedSlideIds.stream()
                .map(s -> s.startsWith("exp-") ? s.substring("exp-".length()) : s)
                .toList();

        if (dto.getExperiences() != null && !dto.getExperiences().isEmpty()) {
            boolean hasSpecificMatch = dto.getExperiences().stream()
                    .anyMatch(e -> e.getId() != null && targetExperienceIds.contains(e.getId().toString()));

            if (hasSpecificMatch) {
                dto.setExperiences(dto.getExperiences().stream()
                        .filter(e -> e.getId() != null && targetExperienceIds.contains(e.getId().toString()))
                        .toList());
            }
        }
    }

    private String hashToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadRequestException("Token không hợp lệ.");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private PortfolioResponseDto deserializeSnapshot(String json) {
        try {
            return objectMapper.readValue(json, PortfolioResponseDto.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse snapshot JSON: {}", e.getMessage(), e);
            throw new BadRequestException("Không thể đọc dữ liệu snapshot.");
        }
    }
}
