package com.soodfolio.api.service;

import com.soodfolio.api.dto.AnalyticsDtos.RecordViewRequestDto;
import com.soodfolio.api.dto.AnalyticsDtos.ViewCountResponseDto;
import com.soodfolio.api.dto.PortfolioResponseDto;
import com.soodfolio.api.dto.UpdatePortfolioRequestDto;
import com.soodfolio.api.entity.AnalyticsView;
import com.soodfolio.api.entity.Portfolio;
import com.soodfolio.api.entity.User;
import com.soodfolio.api.exception.ResourceNotFoundException;
import com.soodfolio.api.repository.AnalyticsViewRepository;
import com.soodfolio.api.repository.PortfolioRepository;
import com.soodfolio.api.repository.UserRepository;
import com.soodfolio.api.security.UserPrincipal;
import com.soodfolio.api.service.impl.PortfolioServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private AnalyticsViewRepository analyticsViewRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PortfolioServiceImpl portfolioService;

    private UUID userId;
    private Portfolio portfolio;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .fullName("Alex Nguyen")
                .build();

        portfolio = Portfolio.builder()
                .id(UUID.randomUUID())
                .user(user)
                .username("alex")
                .title("Alex Nguyen")
                .tagline("Software Engineer")
                .bio("Original bio")
                .statusText("AVAILABLE")
                .isPublished(true)
                .socialLinks(new ArrayList<>())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should successfully update portfolio profile, flags, and replace social links")
    void shouldUpdatePortfolioSuccessfully() {
        UpdatePortfolioRequestDto request = UpdatePortfolioRequestDto.builder()
                .title("Alex Nguyen Updated")
                .tagline("Senior Fullstack Architect")
                .bio("New updated bio")
                .statusText("BUSY")
                .showProjects(false)
                .showExperience(true)
                .socialLinks(List.of(
                        PortfolioResponseDto.SocialLinkDto.builder()
                                .platform("GITHUB")
                                .url("https://github.com/alex")
                                .label("[ GitHub ]")
                                .build(),
                        PortfolioResponseDto.SocialLinkDto.builder()
                                .platform("LINKEDIN")
                                .url("https://linkedin.com/in/alex")
                                .label("[ LinkedIn ]")
                                .build()
                ))
                .build();

        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(portfolioRepository.save(any(Portfolio.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(analyticsViewRepository.countByPortfolioId(portfolio.getId())).thenReturn(42L);

        PortfolioResponseDto response = portfolioService.updatePortfolio(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Alex Nguyen Updated");
        assertThat(response.getTagline()).isEqualTo("Senior Fullstack Architect");
        assertThat(response.getBio()).isEqualTo("New updated bio");
        assertThat(response.getStatusText()).isEqualTo("BUSY");
        assertThat(response.isShowProjects()).isFalse();
        assertThat(response.isShowExperience()).isTrue();
        assertThat(response.getTotalViews()).isEqualTo(42L);
        assertThat(response.getSocialLinks()).hasSize(2);
        assertThat(response.getSocialLinks().get(0).getPlatform()).isEqualTo("GITHUB");
        assertThat(response.getSocialLinks().get(1).getPlatform()).isEqualTo("LINKEDIN");

        verify(portfolioRepository).save(portfolio);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when portfolio is not found for userId")
    void shouldThrowNotFoundWhenPortfolioDoesNotExist() {
        UpdatePortfolioRequestDto request = UpdatePortfolioRequestDto.builder()
                .title("Title")
                .build();

        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.updatePortfolio(userId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Portfolio not found with userId");

        verify(portfolioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully record view and return total view count")
    void shouldRecordViewSuccessfully() {
        RecordViewRequestDto request = RecordViewRequestDto.builder()
                .slideId("01")
                .referrer("https://google.com")
                .build();

        when(portfolioRepository.findByUsername("alex")).thenReturn(Optional.of(portfolio));
        when(analyticsViewRepository.countByPortfolioId(portfolio.getId())).thenReturn(100L);

        ViewCountResponseDto response = 
                portfolioService.recordView("alex", request, "Mozilla/5.0 (Windows NT 10.0)");

        assertThat(response).isNotNull();
        assertThat(response.getTotalViews()).isEqualTo(100L);

        ArgumentCaptor<AnalyticsView> captor = ArgumentCaptor.forClass(AnalyticsView.class);
        verify(analyticsViewRepository).save(captor.capture());

        AnalyticsView captured = captor.getValue();
        assertThat(captured.getPortfolio()).isEqualTo(portfolio);
        assertThat(captured.getSlideId()).isEqualTo("01");
        assertThat(captured.getReferrer()).isEqualTo("https://google.com");
        assertThat(captured.getUserAgent()).isEqualTo("Mozilla/5.0 (Windows NT 10.0)");
        assertThat(captured.getEventType()).isEqualTo("VIEW");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when recording view for non-existent username")
    void shouldThrowNotFoundWhenRecordingViewForUnknownUsername() {
        RecordViewRequestDto request = RecordViewRequestDto.builder()
                .slideId("01")
                .build();

        when(portfolioRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.recordView("unknown", request, "ua"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Portfolio not found with username: 'unknown'");

        verify(analyticsViewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when recording view on draft (unpublished) portfolio")
    void shouldThrowNotFoundWhenRecordingViewOnDraftPortfolio() {
        portfolio.setPublished(false);
        RecordViewRequestDto request = RecordViewRequestDto.builder()
                .slideId("01")
                .build();

        when(portfolioRepository.findByUsername("alex")).thenReturn(Optional.of(portfolio));

        assertThatThrownBy(() -> portfolioService.recordView("alex", request, "Mozilla/5.0"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Portfolio not found with username: 'alex'");

        verify(analyticsViewRepository, never()).save(any());
    }

    @Test
    @DisplayName("DRAFT visibility: Should return empty Optional when anonymous or non-owner accesses draft portfolio via getPortfolioByUsername")
    void shouldReturnEmptyWhenAccessingDraftPortfolioByUsername() {
        portfolio.setPublished(false);
        when(portfolioRepository.findByUsername("alex")).thenReturn(Optional.of(portfolio));

        // 1. getPortfolioByUsername(username) returns Optional.empty()
        Optional<PortfolioResponseDto> result = portfolioService.getPortfolioByUsername("alex");
        assertThat(result).isEmpty();

        // 2. getPortfolioByUsername(username, null) returns Optional.empty()
        Optional<PortfolioResponseDto> nullViewerResult = portfolioService.getPortfolioByUsername("alex", null);
        assertThat(nullViewerResult).isEmpty();

        // 3. getPortfolioByUsername with non-owner returns Optional.empty()
        UserPrincipal otherViewer = new UserPrincipal(
                UUID.randomUUID(),
                "intruder@soodfolio.dev",
                "secret",
                "Intruder",
                AuthorityUtils.createAuthorityList("ROLE_USER")
        );
        Optional<PortfolioResponseDto> otherViewerResult = portfolioService.getPortfolioByUsername("alex", otherViewer);
        assertThat(otherViewerResult).isEmpty();
    }

    @Test
    @DisplayName("PUBLISHED visibility: Should return DTO when accessing published portfolio via getPortfolioByUsername")
    void shouldReturnDtoWhenAccessingPublishedPortfolioByUsername() {
        portfolio.setPublished(true);
        when(portfolioRepository.findByUsername("alex")).thenReturn(Optional.of(portfolio));

        Optional<PortfolioResponseDto> result = portfolioService.getPortfolioByUsername("alex");
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("alex");
        assertThat(result.get().isPublished()).isTrue();
    }

    @Test
    @DisplayName("DRAFT visibility: Should return portfolio when owner accesses draft portfolio via getPortfolioByUsername with viewer or via getPortfolioByUserId")
    void shouldReturnPortfolioWhenOwnerAccessesDraftPortfolio() {
        portfolio.setPublished(false);
        when(portfolioRepository.findByUsername("alex")).thenReturn(Optional.of(portfolio));
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));

        UserPrincipal ownerViewer = new UserPrincipal(
                userId,
                "alex@soodfolio.dev",
                "secret",
                "Alex Nguyen",
                AuthorityUtils.createAuthorityList("ROLE_USER")
        );

        // 1. Owner accesses via getPortfolioByUsername(username, ownerViewer)
        Optional<PortfolioResponseDto> directResult = portfolioService.getPortfolioByUsername("alex", ownerViewer);
        assertThat(directResult).isPresent();
        assertThat(directResult.get().getUsername()).isEqualTo("alex");
        assertThat(directResult.get().isPublished()).isFalse();

        // 2. Owner accesses via getPortfolioByUserId(userId)
        Optional<PortfolioResponseDto> userIdResult = portfolioService.getPortfolioByUserId(userId);
        assertThat(userIdResult).isPresent();
        assertThat(userIdResult.get().getUsername()).isEqualTo("alex");
        assertThat(userIdResult.get().isPublished()).isFalse();
    }

    @Test
    @DisplayName("Should successfully toggle published status between draft and published")
    void shouldUpdatePublishedStatusSuccessfully() {
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(portfolioRepository.save(any(Portfolio.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Toggle to draft (false)
        PortfolioResponseDto unpublishedDto = portfolioService.updatePublishedStatus(userId, false);
        assertThat(unpublishedDto.isPublished()).isFalse();
        assertThat(portfolio.isPublished()).isFalse();

        // Toggle to published (true)
        PortfolioResponseDto publishedDto = portfolioService.updatePublishedStatus(userId, true);
        assertThat(publishedDto.isPublished()).isTrue();
        assertThat(portfolio.isPublished()).isTrue();

        verify(portfolioRepository, times(2)).save(portfolio);
    }

    @Test
    @DisplayName("Should update themeConfig and return it in PortfolioResponseDto")
    void shouldUpdateThemeConfigSuccessfully() {
        String newThemeConfig = "{\"preset\":\"spotlight-focus\"}";
        UpdatePortfolioRequestDto request = UpdatePortfolioRequestDto.builder()
                .title("Alex Nguyen")
                .themeConfig(newThemeConfig)
                .build();

        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(portfolioRepository.save(any(Portfolio.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PortfolioResponseDto response = portfolioService.updatePortfolio(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getThemeConfig()).isEqualTo(newThemeConfig);
        assertThat(portfolio.getThemeConfig()).isEqualTo(newThemeConfig);
        verify(portfolioRepository).save(portfolio);
    }

    @Test
    @DisplayName("Should preserve existing themeConfig when request themeConfig is null")
    void shouldPreserveExistingThemeConfigWhenRequestThemeConfigIsNull() {
        String existingThemeConfig = "{\"preset\":\"terminal-matrix\"}";
        portfolio.setThemeConfig(existingThemeConfig);

        UpdatePortfolioRequestDto request = UpdatePortfolioRequestDto.builder()
                .title("Alex Nguyen Updated")
                .themeConfig(null)
                .build();

        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(portfolioRepository.save(any(Portfolio.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PortfolioResponseDto response = portfolioService.updatePortfolio(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getThemeConfig()).isEqualTo(existingThemeConfig);
        assertThat(portfolio.getThemeConfig()).isEqualTo(existingThemeConfig);
        verify(portfolioRepository).save(portfolio);
    }

    @Test
    @DisplayName("Should handle default and null themeConfig cleanly during read and update")
    void shouldHandleDefaultAndNullThemeConfigCleanly() {
        // 1. Reading portfolio with unset (null) themeConfig
        portfolio.setThemeConfig(null);
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));

        Optional<PortfolioResponseDto> defaultResult = portfolioService.getPortfolioByUserId(userId);
        assertThat(defaultResult).isPresent();
        assertThat(defaultResult.get().getThemeConfig()).isNull();

        // 2. Updating with null themeConfig when existing themeConfig is already null
        UpdatePortfolioRequestDto updateRequest = UpdatePortfolioRequestDto.builder()
                .title("Alex Nguyen")
                .themeConfig(null)
                .build();
        when(portfolioRepository.save(any(Portfolio.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PortfolioResponseDto updatedResponse = portfolioService.updatePortfolio(userId, updateRequest);
        assertThat(updatedResponse).isNotNull();
        assertThat(updatedResponse.getThemeConfig()).isNull();
        assertThat(portfolio.getThemeConfig()).isNull();

        // 3. Reading portfolio with populated themeConfig
        String customTheme = "{\"preset\":\"editorial-clean\",\"accentColor\":\"#F59E0B\"}";
        portfolio.setThemeConfig(customTheme);
        when(portfolioRepository.findByUsername("alex")).thenReturn(Optional.of(portfolio));

        Optional<PortfolioResponseDto> customResult = portfolioService.getPortfolioByUsername("alex");
        assertThat(customResult).isPresent();
        assertThat(customResult.get().getThemeConfig()).isEqualTo(customTheme);
    }
}
