package com.soodfolio.api.service;

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
import com.soodfolio.api.service.impl.ShareLinkServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShareLinkServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private PortfolioSnapshotRepository snapshotRepository;

    @Mock
    private ShareLinkRepository shareLinkRepository;

    @Mock
    private ShareLinkTelemetryRepository telemetryRepository;

    @Mock
    private SnapshotService snapshotService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ShareLinkServiceImpl shareLinkService;

    private UUID userId;
    private UUID portfolioId;
    private Portfolio portfolio;
    private PortfolioSnapshot sampleSnapshot;
    private PortfolioResponseDto samplePortfolioDto;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        portfolioId = UUID.randomUUID();

        portfolio = Portfolio.builder()
                .id(portfolioId)
                .username("alex")
                .title("Alex Nguyen")
                .build();

        sampleSnapshot = PortfolioSnapshot.builder()
                .id(UUID.randomUUID())
                .portfolio(portfolio)
                .version(1)
                .snapshotData("{\"username\":\"alex\",\"title\":\"Alex Nguyen\"}")
                .changelog("v1")
                .createdAt(LocalDateTime.now())
                .build();

        samplePortfolioDto = PortfolioResponseDto.builder()
                .id(portfolioId)
                .username("alex")
                .title("Alex Nguyen")
                .showProjects(true)
                .showExperience(true)
                .showSkills(true)
                .showEducation(true)
                .showContact(true)
                .projects(new ArrayList<>(List.of(
                        PortfolioResponseDto.ProjectDto.builder()
                                .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                                .title("Project One")
                                .build(),
                        PortfolioResponseDto.ProjectDto.builder()
                                .id(UUID.fromString("00000000-0000-0000-0000-000000000002"))
                                .title("Project Two")
                                .build()
                )))
                .experiences(new ArrayList<>(List.of(
                        PortfolioResponseDto.ExperienceDto.builder()
                                .id(UUID.fromString("00000000-0000-0000-0000-000000000010"))
                                .roleTitle("Role Ten")
                                .build(),
                        PortfolioResponseDto.ExperienceDto.builder()
                                .id(UUID.fromString("00000000-0000-0000-0000-000000000020"))
                                .roleTitle("Role Twenty")
                                .build()
                )))
                .build();
    }

    // Helper function to calculate expected SHA-256 hex string for verification
    private String computeSha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) sb.append('0');
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    // ==========================================
    // 1. createShareLink Tests (Token Hash SHA-256)
    // ==========================================

    @Test
    @DisplayName("Should create targeted share link with SHA-256 hashed token and rawToken prefix 'st_live_'")
    void shouldCreateShareLinkWithHashedToken() {
        CreateShareLinkRequest request = CreateShareLinkRequest.builder()
                .companyName("Shopee Singapore")
                .slugAlias("shopee-backend")
                .customLayout("CINEMATIC_DECK")
                .allowedSlideIds(List.of("experience", "projects"))
                .expiresAt(LocalDateTime.now().plusDays(14))
                .maxViews(20)
                .build();

        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(snapshotRepository.findTopByPortfolioIdOrderByVersionDesc(portfolioId)).thenReturn(Optional.of(sampleSnapshot));

        when(shareLinkRepository.save(any(ShareLink.class))).thenAnswer(invocation -> {
            ShareLink link = invocation.getArgument(0);
            link.setId(UUID.randomUUID());
            link.setCreatedAt(LocalDateTime.now());
            return link;
        });

        ShareLinkResponse response = shareLinkService.createShareLink(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getCompanyName()).isEqualTo("Shopee Singapore");
        assertThat(response.getSlugAlias()).isEqualTo("shopee-backend");
        assertThat(response.getCustomLayout()).isEqualTo("CINEMATIC_DECK");
        assertThat(response.getMaxViews()).isEqualTo(20);
        assertThat(response.getViewCount()).isEqualTo(0);
        assertThat(response.isRevoked()).isFalse();

        // 1. Raw token MUST start with st_live_
        assertThat(response.getRawToken()).startsWith("st_live_");
        assertThat(response.getShareUrl()).isEqualTo("/p/" + response.getRawToken());

        // 2. Token stored in database MUST be SHA-256 hash (64 hex characters)
        ArgumentCaptor<ShareLink> captor = ArgumentCaptor.forClass(ShareLink.class);
        verify(shareLinkRepository).save(captor.capture());
        ShareLink saved = captor.getValue();

        assertThat(saved.getTokenHash()).isNotEqualTo(response.getRawToken());
        assertThat(saved.getTokenHash()).hasSize(64);
        assertThat(saved.getTokenHash()).isEqualTo(computeSha256(response.getRawToken()));
        assertThat(saved.getPortfolio()).isEqualTo(portfolio);
        assertThat(saved.getSnapshot()).isEqualTo(sampleSnapshot);
    }

    @Test
    @DisplayName("Should auto-generate initial snapshot v1 if portfolio has no snapshots when creating share link")
    void shouldAutoGenerateSnapshotWhenNoneExists() {
        CreateShareLinkRequest request = CreateShareLinkRequest.builder()
                .companyName("Grab VN")
                .build();

        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        // First lookup returns empty (triggers auto-generation)
        when(snapshotRepository.findTopByPortfolioIdOrderByVersionDesc(portfolioId))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(sampleSnapshot));

        when(shareLinkRepository.save(any(ShareLink.class))).thenAnswer(inv -> inv.getArgument(0));

        ShareLinkResponse response = shareLinkService.createShareLink(userId, request);

        assertThat(response).isNotNull();
        // Verify snapshotService.publishSnapshot was invoked
        verify(snapshotService).publishSnapshot(eq(userId), contains("Initial auto-generated snapshot"));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when creating share link for non-existent portfolio")
    void shouldThrowNotFoundWhenCreatingLinkForUnknownUser() {
        CreateShareLinkRequest request = CreateShareLinkRequest.builder()
                .companyName("Shopee")
                .build();

        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shareLinkService.createShareLink(userId, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(shareLinkRepository, never()).save(any());
    }

    // ==========================================
    // 2. getShareLinks & revokeShareLink Tests
    // ==========================================

    @Test
    @DisplayName("Should return list of share links for current creator")
    void shouldGetShareLinksSuccessfully() {
        ShareLink link1 = ShareLink.builder()
                .id(UUID.randomUUID())
                .companyName("VNG Corp")
                .viewCount(5)
                .isRevoked(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(shareLinkRepository.findByPortfolioIdOrderByCreatedAtDesc(portfolioId)).thenReturn(List.of(link1));

        List<ShareLinkSummaryResponse> list = shareLinkService.getShareLinks(userId);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getCompanyName()).isEqualTo("VNG Corp");
        assertThat(list.get(0).getViewCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should successfully revoke active share link (setting isRevoked = true)")
    void shouldRevokeShareLinkSuccessfully() {
        UUID linkId = UUID.randomUUID();
        ShareLink activeLink = ShareLink.builder()
                .id(linkId)
                .portfolio(portfolio)
                .companyName("VNG Corp")
                .isRevoked(false)
                .build();

        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(shareLinkRepository.findByIdAndPortfolioId(linkId, portfolioId)).thenReturn(Optional.of(activeLink));

        shareLinkService.revokeShareLink(userId, linkId);

        assertThat(activeLink.isRevoked()).isTrue();
        verify(shareLinkRepository).save(activeLink);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when revoking non-existent share link")
    void shouldThrowNotFoundWhenRevokingNonExistentLink() {
        UUID linkId = UUID.randomUUID();
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(shareLinkRepository.findByIdAndPortfolioId(linkId, portfolioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shareLinkService.revokeShareLink(userId, linkId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ==========================================
    // 3. getPublicDeck Tests (403 Forbidden, 410 Gone, Filtering)
    // ==========================================

    @Test
    @DisplayName("Should successfully retrieve public deck and increment view count by 1")
    void shouldGetPublicDeckSuccessfully() throws JsonProcessingException {
        String rawToken = "st_live_ValidToken1234567890";
        String tokenHash = computeSha256(rawToken);

        ShareLink validLink = ShareLink.builder()
                .id(UUID.randomUUID())
                .portfolio(portfolio)
                .snapshot(sampleSnapshot)
                .tokenHash(tokenHash)
                .companyName("FPT Software")
                .customLayout("CINEMATIC_DECK")
                .viewCount(3)
                .maxViews(10)
                .isRevoked(false)
                .expiresAt(LocalDateTime.now().plusDays(5))
                .build();

        when(shareLinkRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(validLink));
        when(shareLinkRepository.incrementViewCountIfAllowed(eq(validLink.getId()), any(LocalDateTime.class)))
                .thenReturn(1);
        when(objectMapper.readValue(sampleSnapshot.getSnapshotData(), PortfolioResponseDto.class))
                .thenReturn(samplePortfolioDto);

        PublicDeckResponse response = shareLinkService.getPublicDeck(rawToken);

        assertThat(response).isNotNull();
        assertThat(response.getCompanyName()).isEqualTo("FPT Software");
        assertThat(response.getCustomLayout()).isEqualTo("CINEMATIC_DECK");
        assertThat(response.getSnapshotVersion()).isEqualTo(1);
        assertThat(response.getPortfolio()).isEqualTo(samplePortfolioDto);

        verify(shareLinkRepository).incrementViewCountIfAllowed(eq(validLink.getId()), any(LocalDateTime.class));
        verify(shareLinkRepository, never()).save(validLink);
    }

    @Test
    @DisplayName("Should throw 403 ForbiddenException when public link has been revoked")
    void shouldThrow403ForbiddenWhenLinkIsRevoked() {
        String rawToken = "st_live_RevokedToken123";
        String tokenHash = computeSha256(rawToken);

        ShareLink revokedLink = ShareLink.builder()
                .id(UUID.randomUUID())
                .tokenHash(tokenHash)
                .isRevoked(true)
                .build();

        when(shareLinkRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(revokedLink));

        assertThatThrownBy(() -> shareLinkService.getPublicDeck(rawToken))
                .isInstanceOf(ForbiddenException.class)
                .satisfies(ex -> {
                    ForbiddenException fe = (ForbiddenException) ex;
                    assertThat(fe.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(fe.getMessage()).contains("thu hồi");
                });

        verify(shareLinkRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ShareLinkExpiredException (410 Gone) when incrementViewCountIfAllowed returns 0 due to concurrency slot loss")
    void shouldThrowExpiredWhenConcurrentSlotLost() {
        String rawToken = "st_live_SlotLossToken";
        String tokenHash = computeSha256(rawToken);

        ShareLink link = ShareLink.builder()
                .id(UUID.randomUUID())
                .tokenHash(tokenHash)
                .isRevoked(false)
                .viewCount(9)
                .maxViews(10)
                .build();

        when(shareLinkRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(link));
        when(shareLinkRepository.incrementViewCountIfAllowed(eq(link.getId()), any(LocalDateTime.class)))
                .thenReturn(0);

        assertThatThrownBy(() -> shareLinkService.getPublicDeck(rawToken))
                .isInstanceOf(ShareLinkExpiredException.class)
                .hasMessageContaining("hết hạn hoặc đạt giới hạn lượt xem");

        verify(shareLinkRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw 410 ShareLinkExpiredException (HttpStatus.GONE) when expiresAt is in the past")
    void shouldThrow410GoneWhenLinkIsPastExpiresAt() {
        String rawToken = "st_live_ExpiredToken123";
        String tokenHash = computeSha256(rawToken);

        ShareLink expiredLink = ShareLink.builder()
                .id(UUID.randomUUID())
                .tokenHash(tokenHash)
                .isRevoked(false)
                .expiresAt(LocalDateTime.now().minusMinutes(5)) // Expired 5 mins ago
                .viewCount(1)
                .maxViews(10)
                .build();

        when(shareLinkRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(expiredLink));

        assertThatThrownBy(() -> shareLinkService.getPublicDeck(rawToken))
                .isInstanceOf(ShareLinkExpiredException.class)
                .satisfies(ex -> {
                    ShareLinkExpiredException se = (ShareLinkExpiredException) ex;
                    assertThat(se.getStatus()).isEqualTo(HttpStatus.GONE);
                    assertThat(se.getMessage()).contains("hết hạn");
                });

        verify(shareLinkRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw 410 ShareLinkExpiredException (HttpStatus.GONE) when view count reaches maxViews limit")
    void shouldThrow410GoneWhenMaxViewsReached() {
        String rawToken = "st_live_MaxViewsToken123";
        String tokenHash = computeSha256(rawToken);

        ShareLink maxedLink = ShareLink.builder()
                .id(UUID.randomUUID())
                .tokenHash(tokenHash)
                .isRevoked(false)
                .viewCount(5)
                .maxViews(5) // Limit reached
                .build();

        when(shareLinkRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(maxedLink));

        assertThatThrownBy(() -> shareLinkService.getPublicDeck(rawToken))
                .isInstanceOf(ShareLinkExpiredException.class)
                .satisfies(ex -> {
                    ShareLinkExpiredException se = (ShareLinkExpiredException) ex;
                    assertThat(se.getStatus()).isEqualTo(HttpStatus.GONE);
                    assertThat(se.getMessage()).contains("giới hạn lượt xem");
                });

        verify(shareLinkRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when token hash is not found in database")
    void shouldThrowNotFoundWhenTokenHashDoesNotExist() {
        String rawToken = "st_live_UnknownToken";
        String tokenHash = computeSha256(rawToken);

        when(shareLinkRepository.findByTokenHash(tokenHash)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shareLinkService.getPublicDeck(rawToken))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ShareLink not found with token: 'invalid'");
    }

    @Test
    @DisplayName("Should filter allowed slides and project list based on allowedSlideIds configuration")
    void shouldFilterAllowedSlidesCorrectly() throws JsonProcessingException {
        String rawToken = "st_live_FilterToken123";
        String tokenHash = computeSha256(rawToken);

        // Allow only "projects" section and specific project "00000000-0000-0000-0000-000000000001"
        ShareLink filteredLink = ShareLink.builder()
                .id(UUID.randomUUID())
                .snapshot(sampleSnapshot)
                .tokenHash(tokenHash)
                .allowedSlideIds(List.of("projects", "project-00000000-0000-0000-0000-000000000001"))
                .isRevoked(false)
                .viewCount(0)
                .build();

        when(shareLinkRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(filteredLink));
        when(shareLinkRepository.incrementViewCountIfAllowed(eq(filteredLink.getId()), any(LocalDateTime.class)))
                .thenReturn(1);
        when(objectMapper.readValue(sampleSnapshot.getSnapshotData(), PortfolioResponseDto.class))
                .thenReturn(samplePortfolioDto);

        PublicDeckResponse response = shareLinkService.getPublicDeck(rawToken);

        assertThat(response).isNotNull();
        PortfolioResponseDto dto = response.getPortfolio();

        // General slide visibility flags
        assertThat(dto.isShowProjects()).isTrue();
        assertThat(dto.isShowExperience()).isFalse();
        assertThat(dto.isShowSkills()).isFalse();
        assertThat(dto.isShowEducation()).isFalse();
        assertThat(dto.isShowContact()).isFalse();

        // Project filtering: Project Two should be removed, leaving only Project One
        assertThat(dto.getProjects()).hasSize(1);
        assertThat(dto.getProjects().get(0).getId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    }

    @Test
    @DisplayName("Should filter allowed slides and experience list based on allowedSlideIds configuration")
    void shouldFilterExperiencesCorrectlyBasedOnAllowedSlideIds() throws JsonProcessingException {
        String rawToken = "st_live_FilterExpToken123";
        String tokenHash = computeSha256(rawToken);

        // Allow only "experience" section and specific experience "00000000-0000-0000-0000-000000000010"
        ShareLink filteredLink = ShareLink.builder()
                .id(UUID.randomUUID())
                .snapshot(sampleSnapshot)
                .tokenHash(tokenHash)
                .allowedSlideIds(List.of("experience", "exp-00000000-0000-0000-0000-000000000010"))
                .isRevoked(false)
                .viewCount(0)
                .build();

        when(shareLinkRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(filteredLink));
        when(shareLinkRepository.incrementViewCountIfAllowed(eq(filteredLink.getId()), any(LocalDateTime.class)))
                .thenReturn(1);
        when(objectMapper.readValue(sampleSnapshot.getSnapshotData(), PortfolioResponseDto.class))
                .thenReturn(samplePortfolioDto);

        PublicDeckResponse response = shareLinkService.getPublicDeck(rawToken);

        assertThat(response).isNotNull();
        PortfolioResponseDto dto = response.getPortfolio();

        assertThat(dto.isShowExperience()).isTrue();
        assertThat(dto.isShowProjects()).isFalse();

        // Experience filtering: Role Twenty should be removed, leaving only Role Ten
        assertThat(dto.getExperiences()).hasSize(1);
        assertThat(dto.getExperiences().get(0).getId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000010"));
    }

    // ==========================================
    // 4. recordTelemetry & Analytics Tests
    // ==========================================

    @Test
    @DisplayName("Should successfully record telemetry dwell-time events, clamp negative seconds, and uppercase device type")
    void shouldRecordTelemetrySuccessfully() {
        String rawToken = "st_live_TelemetryToken";
        String tokenHash = computeSha256(rawToken);

        ShareLink link = ShareLink.builder()
                .id(UUID.randomUUID())
                .tokenHash(tokenHash)
                .isRevoked(false)
                .build();

        when(shareLinkRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(link));

        TelemetryBatchRequest request = TelemetryBatchRequest.builder()
                .sessionId("sess_abc123")
                .events(List.of(
                        TelemetryEventDto.builder()
                                .slideKey("hero")
                                .dwellSeconds(30)
                                .deviceType("desktop")
                                .build(),
                        TelemetryEventDto.builder()
                                .slideKey("project-1")
                                .dwellSeconds(-10) // Negative dwell time should be clamped to 0
                                .deviceType("mobile")
                                .userAgent("Mobile Safari")
                                .build()
                ))
                .build();

        shareLinkService.recordTelemetry(rawToken, request, "Default User Agent");

        ArgumentCaptor<List<ShareLinkTelemetry>> captor = ArgumentCaptor.forClass(List.class);
        verify(telemetryRepository).saveAll(captor.capture());

        List<ShareLinkTelemetry> saved = captor.getValue();
        assertThat(saved).hasSize(2);

        ShareLinkTelemetry t1 = saved.get(0);
        assertThat(t1.getSlideKey()).isEqualTo("hero");
        assertThat(t1.getDwellSeconds()).isEqualTo(30);
        assertThat(t1.getDeviceType()).isEqualTo("DESKTOP");
        assertThat(t1.getUserAgent()).isEqualTo("Default User Agent");

        ShareLinkTelemetry t2 = saved.get(1);
        assertThat(t2.getSlideKey()).isEqualTo("project-1");
        assertThat(t2.getDwellSeconds()).isEqualTo(0); // Clamped
        assertThat(t2.getDeviceType()).isEqualTo("MOBILE");
        assertThat(t2.getUserAgent()).isEqualTo("Mobile Safari");
    }

    @Test
    @DisplayName("Should ignore telemetry recording when share link is revoked")
    void shouldIgnoreTelemetryWhenLinkIsRevoked() {
        String rawToken = "st_live_RevokedToken";
        String tokenHash = computeSha256(rawToken);

        ShareLink revokedLink = ShareLink.builder()
                .id(UUID.randomUUID())
                .tokenHash(tokenHash)
                .isRevoked(true)
                .build();

        when(shareLinkRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(revokedLink));

        TelemetryBatchRequest request = TelemetryBatchRequest.builder()
                .sessionId("sess_123")
                .events(List.of(TelemetryEventDto.builder().slideKey("hero").dwellSeconds(10).build()))
                .build();

        shareLinkService.recordTelemetry(rawToken, request, "UA");

        verify(telemetryRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Should ignore telemetry recording when share link is expired")
    void shouldIgnoreTelemetryWhenLinkIsExpired() {
        String rawToken = "st_live_ExpiredTeleToken";
        String tokenHash = computeSha256(rawToken);

        ShareLink expiredLink = ShareLink.builder()
                .id(UUID.randomUUID())
                .tokenHash(tokenHash)
                .isRevoked(false)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();

        when(shareLinkRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(expiredLink));

        TelemetryBatchRequest request = TelemetryBatchRequest.builder()
                .sessionId("sess_123")
                .events(List.of(TelemetryEventDto.builder().slideKey("hero").dwellSeconds(10).build()))
                .build();

        shareLinkService.recordTelemetry(rawToken, request, "Agent");

        verify(telemetryRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Should clamp dwellSeconds to [0, 3600] and normalize unknown deviceType to UNKNOWN")
    void shouldClampDwellSecondsAndNormalizeDeviceType() {
        String rawToken = "st_live_NormalizeTeleToken";
        String tokenHash = computeSha256(rawToken);

        ShareLink link = ShareLink.builder()
                .id(UUID.randomUUID())
                .tokenHash(tokenHash)
                .isRevoked(false)
                .build();

        when(shareLinkRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(link));

        TelemetryBatchRequest request = TelemetryBatchRequest.builder()
                .sessionId("sess_clamp")
                .events(List.of(
                        TelemetryEventDto.builder()
                                .slideKey("slide-huge")
                                .dwellSeconds(999999) // Exceeds 3600
                                .deviceType("SMART_FRIDGE") // Invalid device
                                .build(),
                        TelemetryEventDto.builder()
                                .slideKey("slide-negative")
                                .dwellSeconds(-500) // Negative
                                .deviceType("TABLET") // Valid tablet
                                .build()
                ))
                .build();

        shareLinkService.recordTelemetry(rawToken, request, "UserAgent");

        ArgumentCaptor<List<ShareLinkTelemetry>> captor = ArgumentCaptor.forClass(List.class);
        verify(telemetryRepository).saveAll(captor.capture());

        List<ShareLinkTelemetry> saved = captor.getValue();
        assertThat(saved).hasSize(2);

        assertThat(saved.get(0).getDwellSeconds()).isEqualTo(3600);
        assertThat(saved.get(0).getDeviceType()).isEqualTo("UNKNOWN");

        assertThat(saved.get(1).getDwellSeconds()).isEqualTo(0);
        assertThat(saved.get(1).getDeviceType()).isEqualTo("TABLET");
    }

    @Test
    @DisplayName("Should ignore telemetry recording when events list is empty")
    void shouldIgnoreTelemetryWhenEventsListIsEmpty() {
        String rawToken = "st_live_EmptyEventsToken";
        String tokenHash = computeSha256(rawToken);

        ShareLink link = ShareLink.builder()
                .id(UUID.randomUUID())
                .tokenHash(tokenHash)
                .isRevoked(false)
                .build();

        when(shareLinkRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(link));

        TelemetryBatchRequest request = TelemetryBatchRequest.builder()
                .sessionId("sess_123")
                .events(Collections.emptyList())
                .build();

        shareLinkService.recordTelemetry(rawToken, request, "UA");

        verify(telemetryRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Should compute aggregated link analytics, average dwell-time, and device breakdown correctly")
    void shouldComputeLinkAnalyticsCorrectly() {
        UUID linkId = UUID.randomUUID();
        ShareLink link = ShareLink.builder()
                .id(linkId)
                .portfolio(portfolio)
                .companyName("Grab Singapore")
                .viewCount(15)
                .build();

        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(shareLinkRepository.findByIdAndPortfolioId(linkId, portfolioId)).thenReturn(Optional.of(link));

        List<ShareLinkTelemetry> telemetryList = List.of(
                ShareLinkTelemetry.builder().slideKey("hero").dwellSeconds(20).deviceType("DESKTOP").build(),
                ShareLinkTelemetry.builder().slideKey("hero").dwellSeconds(40).deviceType("DESKTOP").build(),
                ShareLinkTelemetry.builder().slideKey("projects").dwellSeconds(60).deviceType("MOBILE").build(),
                ShareLinkTelemetry.builder().slideKey("experience").dwellSeconds(15).deviceType("DESKTOP").build()
        );

        when(telemetryRepository.findByShareLinkId(linkId)).thenReturn(telemetryList);

        LocalDateTime expectedLastViewed = LocalDateTime.of(2026, 11, 13, 10, 0);
        when(telemetryRepository.findTopByShareLinkIdOrderByCreatedAtDesc(linkId))
                .thenReturn(Optional.of(ShareLinkTelemetry.builder().createdAt(expectedLastViewed).build()));

        TelemetryAnalyticsResponse response = shareLinkService.getLinkAnalytics(userId, linkId);

        assertThat(response).isNotNull();
        assertThat(response.getShareLinkId()).isEqualTo(linkId);
        assertThat(response.getCompanyName()).isEqualTo("Grab Singapore");
        assertThat(response.getTotalViews()).isEqualTo(15);
        assertThat(response.getTotalDwellSeconds()).isEqualTo(135L); // 20 + 40 + 60 + 15
        assertThat(response.getLastViewedAt()).isEqualTo(expectedLastViewed);

        // Slide stats breakdown (sorted by totalDwellSeconds DESC)
        assertThat(response.getSlideStats()).hasSize(3);

        // Top 1: projects (60s)
        SlideTelemetryStatDto topStat = response.getSlideStats().get(0);
        assertThat(topStat.getSlideKey()).isEqualTo("projects");
        assertThat(topStat.getTotalDwellSeconds()).isEqualTo(60L);
        assertThat(topStat.getViewCount()).isEqualTo(1L);
        assertThat(topStat.getAvgDwellSeconds()).isEqualTo(60.0);

        // Top 2: hero (60s, 2 views, avg 30.0)
        SlideTelemetryStatDto heroStat = response.getSlideStats().get(1);
        assertThat(heroStat.getSlideKey()).isEqualTo("hero");
        assertThat(heroStat.getTotalDwellSeconds()).isEqualTo(60L);
        assertThat(heroStat.getViewCount()).isEqualTo(2L);
        assertThat(heroStat.getAvgDwellSeconds()).isEqualTo(30.0);

        // Top 3: experience (15s)
        SlideTelemetryStatDto expStat = response.getSlideStats().get(2);
        assertThat(expStat.getSlideKey()).isEqualTo("experience");
        assertThat(expStat.getTotalDwellSeconds()).isEqualTo(15L);

        // Device breakdown
        assertThat(response.getDeviceBreakdown()).containsEntry("DESKTOP", 3L);
        assertThat(response.getDeviceBreakdown()).containsEntry("MOBILE", 1L);
    }

    @Test
    @DisplayName("Should throw BadRequestException when raw token is null or blank during hashToken")
    void shouldThrowBadRequestWhenHashingNullOrBlankToken() {
        assertThatThrownBy(() -> shareLinkService.getPublicDeck(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Token không hợp lệ.");

        assertThatThrownBy(() -> shareLinkService.getPublicDeck("   "))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Token không hợp lệ.");
    }
}
