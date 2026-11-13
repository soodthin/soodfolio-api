package com.soodfolio.api.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soodfolio.api.dto.PortfolioResponseDto;
import com.soodfolio.api.dto.SnapshotDtos.RollbackResponseDto;
import com.soodfolio.api.dto.SnapshotDtos.SnapshotResponseDto;
import com.soodfolio.api.dto.SnapshotDtos.SnapshotSummaryDto;
import com.soodfolio.api.entity.*;
import com.soodfolio.api.exception.BadRequestException;
import com.soodfolio.api.exception.ResourceNotFoundException;
import com.soodfolio.api.repository.PortfolioRepository;
import com.soodfolio.api.repository.PortfolioSnapshotRepository;
import com.soodfolio.api.repository.UserRepository;
import com.soodfolio.api.service.PortfolioService;
import com.soodfolio.api.service.SnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SnapshotServiceImpl implements SnapshotService {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioSnapshotRepository snapshotRepository;
    private final PortfolioService portfolioService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SnapshotResponseDto publishSnapshot(UUID userId, String changelog) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "userId", userId));

        PortfolioResponseDto currentDto = portfolioService.getPortfolioByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "userId", userId));

        int nextVersion = snapshotRepository.findTopByPortfolioIdOrderByVersionDesc(portfolio.getId())
                .map(s -> s.getVersion() + 1)
                .orElse(1);

        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(currentDto);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize portfolio to JSON for snapshot: {}", e.getMessage(), e);
            throw new BadRequestException("Không thể tạo snapshot do lỗi dữ liệu.");
        }

        PortfolioSnapshot snapshot = PortfolioSnapshot.builder()
                .portfolio(portfolio)
                .version(nextVersion)
                .snapshotData(jsonPayload)
                .changelog(changelog != null && !changelog.isBlank() ? changelog.trim() : "Version " + nextVersion)
                .build();

        PortfolioSnapshot saved = snapshotRepository.save(snapshot);

        return SnapshotResponseDto.builder()
                .id(saved.getId())
                .portfolioId(portfolio.getId())
                .version(saved.getVersion())
                .changelog(saved.getChangelog())
                .createdAt(saved.getCreatedAt())
                .data(currentDto)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SnapshotSummaryDto> getSnapshots(UUID userId) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "userId", userId));

        return snapshotRepository.findByPortfolioIdOrderByVersionDesc(portfolio.getId())
                .stream()
                .map(s -> SnapshotSummaryDto.builder()
                        .id(s.getId())
                        .version(s.getVersion())
                        .changelog(s.getChangelog())
                        .createdAt(s.getCreatedAt())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SnapshotResponseDto getSnapshot(UUID userId, int version) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "userId", userId));

        PortfolioSnapshot snapshot = snapshotRepository.findByPortfolioIdAndVersion(portfolio.getId(), version)
                .orElseThrow(() -> new ResourceNotFoundException("Snapshot", "version", version));

        PortfolioResponseDto dto = deserializeSnapshot(snapshot.getSnapshotData());

        return SnapshotResponseDto.builder()
                .id(snapshot.getId())
                .portfolioId(portfolio.getId())
                .version(snapshot.getVersion())
                .changelog(snapshot.getChangelog())
                .createdAt(snapshot.getCreatedAt())
                .data(dto)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RollbackResponseDto rollbackToSnapshot(UUID userId, int version) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "userId", userId));

        PortfolioSnapshot snapshot = snapshotRepository.findByPortfolioIdAndVersion(portfolio.getId(), version)
                .orElseThrow(() -> new ResourceNotFoundException("Snapshot", "version", version));

        PortfolioResponseDto data = deserializeSnapshot(snapshot.getSnapshotData());

        if (portfolio.getUser() != null) {
            if (data.getFullName() != null && !data.getFullName().isBlank()) {
                portfolio.getUser().setFullName(data.getFullName());
            }
            if (data.getAvatarUrl() != null) {
                portfolio.getUser().setAvatarUrl(data.getAvatarUrl());
            }
            userRepository.save(portfolio.getUser());
        }

        portfolio.setTitle(data.getTitle());
        portfolio.setTagline(data.getTagline());
        portfolio.setBio(data.getBio());
        portfolio.setStatusText(data.getStatusText());
        portfolio.setResumeUrl(data.getResumeUrl());
        portfolio.setThemeMode(data.getThemeMode());
        portfolio.setThemeConfig(data.getThemeConfig());
        portfolio.setShowExperience(data.isShowExperience());
        portfolio.setShowProjects(data.isShowProjects());
        portfolio.setShowSkills(data.isShowSkills());
        portfolio.setShowEducation(data.isShowEducation());
        portfolio.setShowContact(data.isShowContact());
        portfolio.setPublished(data.isPublished());

        portfolio.getSocialLinks().clear();
        if (data.getSocialLinks() != null) {
            for (int i = 0; i < data.getSocialLinks().size(); i++) {
                PortfolioResponseDto.SocialLinkDto s = data.getSocialLinks().get(i);
                portfolio.getSocialLinks().add(SocialLink.builder()
                        .portfolio(portfolio)
                        .platform(s.getPlatform())
                        .url(s.getUrl())
                        .label(s.getLabel())
                        .orderIndex(i)
                        .build());
            }
        }

        portfolio.getExperiences().clear();
        if (data.getExperiences() != null) {
            for (int i = 0; i < data.getExperiences().size(); i++) {
                PortfolioResponseDto.ExperienceDto e = data.getExperiences().get(i);
                portfolio.getExperiences().add(Experience.builder()
                        .portfolio(portfolio)
                        .roleTitle(e.getRoleTitle())
                        .companyName(e.getCompanyName())
                        .companyUrl(e.getCompanyUrl())
                        .location(e.getLocation())
                        .periodText(e.getPeriodText())
                        .description(e.getDescription())
                        .highlights(e.getHighlights() != null ? new ArrayList<>(e.getHighlights()) : new ArrayList<>())
                        .skillsUsed(e.getSkillsUsed() != null ? new ArrayList<>(e.getSkillsUsed()) : new ArrayList<>())
                        .orderIndex(e.getOrderIndex() != 0 ? e.getOrderIndex() : i)
                        .build());
            }
        }

        portfolio.getProjects().clear();
        if (data.getProjects() != null) {
            for (int i = 0; i < data.getProjects().size(); i++) {
                PortfolioResponseDto.ProjectDto p = data.getProjects().get(i);
                portfolio.getProjects().add(Project.builder()
                        .portfolio(portfolio)
                        .title(p.getTitle())
                        .subtitle(p.getSubtitle())
                        .role(p.getRole())
                        .periodText(p.getPeriodText())
                        .description(p.getDescription())
                        .highlights(p.getHighlights() != null ? new ArrayList<>(p.getHighlights()) : new ArrayList<>())
                        .techStack(p.getTechStack() != null ? new ArrayList<>(p.getTechStack()) : new ArrayList<>())
                        .liveUrl(p.getLiveUrl())
                        .githubUrl(p.getGithubUrl())
                        .imageUrl(p.getImageUrl())
                        .orderIndex(p.getOrderIndex() != 0 ? p.getOrderIndex() : i)
                        .build());
            }
        }

        portfolio.getSkills().clear();
        if (data.getSkills() != null) {
            for (int i = 0; i < data.getSkills().size(); i++) {
                PortfolioResponseDto.SkillDto sk = data.getSkills().get(i);
                portfolio.getSkills().add(Skill.builder()
                        .portfolio(portfolio)
                        .category(sk.getCategory())
                        .name(sk.getName())
                        .orderIndex(sk.getOrderIndex() != 0 ? sk.getOrderIndex() : i)
                        .build());
            }
        }

        portfolio.getEducations().clear();
        if (data.getEducations() != null) {
            for (int i = 0; i < data.getEducations().size(); i++) {
                PortfolioResponseDto.EducationDto ed = data.getEducations().get(i);
                portfolio.getEducations().add(Education.builder()
                        .portfolio(portfolio)
                        .institution(ed.getInstitution())
                        .degree(ed.getDegree())
                        .fieldOfStudy(ed.getFieldOfStudy())
                        .periodText(ed.getPeriodText())
                        .highlights(ed.getHighlights())
                        .orderIndex(ed.getOrderIndex() != 0 ? ed.getOrderIndex() : i)
                        .build());
            }
        }

        Portfolio saved = portfolioRepository.save(portfolio);
        PortfolioResponseDto updatedDto = portfolioService.getPortfolioByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "userId", userId));

        return RollbackResponseDto.builder()
                .message("Khôi phục thành công về phiên bản " + version)
                .restoredVersion(version)
                .portfolio(updatedDto)
                .build();
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
