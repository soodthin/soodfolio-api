package com.soodfolio.api.service.impl;

import com.soodfolio.api.dto.AnalyticsDtos.RecordViewRequestDto;
import com.soodfolio.api.dto.AnalyticsDtos.ViewCountResponseDto;
import com.soodfolio.api.dto.PortfolioResponseDto;
import com.soodfolio.api.dto.UpdatePortfolioRequestDto;
import com.soodfolio.api.entity.AnalyticsView;
import com.soodfolio.api.entity.Portfolio;
import com.soodfolio.api.entity.SocialLink;
import com.soodfolio.api.exception.ResourceNotFoundException;
import com.soodfolio.api.repository.AnalyticsViewRepository;
import com.soodfolio.api.repository.PortfolioRepository;
import com.soodfolio.api.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class PortfolioServiceImpl implements PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final AnalyticsViewRepository analyticsViewRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<PortfolioResponseDto> getPortfolioByUsername(String username) {
        return portfolioRepository.findByUsername(username)
                .map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PortfolioResponseDto> getPortfolioByUserId(UUID userId) {
        return portfolioRepository.findByUserId(userId)
                .map(this::mapToDto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortfolioResponseDto updatePublishedStatus(UUID userId, boolean isPublished) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "userId", userId));
        portfolio.setPublished(isPublished);
        Portfolio saved = portfolioRepository.save(portfolio);
        return mapToDto(saved);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortfolioResponseDto updatePortfolio(UUID userId, UpdatePortfolioRequestDto request) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "userId", userId));

        portfolio.setTitle(request.getTitle());
        portfolio.setTagline(request.getTagline());
        portfolio.setBio(request.getBio());
        portfolio.setStatusText(request.getStatusText());
        portfolio.setResumeUrl(request.getResumeUrl());
        portfolio.setThemeMode(request.getThemeMode());
        if (request.getThemeConfig() != null) {
            portfolio.setThemeConfig(request.getThemeConfig());
        }
        portfolio.setShowExperience(request.isShowExperience());
        portfolio.setShowProjects(request.isShowProjects());
        portfolio.setShowSkills(request.isShowSkills());
        portfolio.setShowEducation(request.isShowEducation());
        portfolio.setShowContact(request.isShowContact());

        if (request.getSocialLinks() != null) {
            portfolio.getSocialLinks().clear();
            for (int i = 0; i < request.getSocialLinks().size(); i++) {
                PortfolioResponseDto.SocialLinkDto s = request.getSocialLinks().get(i);
                portfolio.getSocialLinks().add(SocialLink.builder()
                        .portfolio(portfolio)
                        .platform(s.getPlatform())
                        .url(s.getUrl())
                        .label(s.getLabel())
                        .orderIndex(i)
                        .build());
            }
        }

        Portfolio saved = portfolioRepository.save(portfolio);
        return mapToDto(saved);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ViewCountResponseDto recordView(String username, RecordViewRequestDto request, String userAgent) {
        Portfolio portfolio = portfolioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "username", username));

        AnalyticsView view = AnalyticsView.builder()
                .portfolio(portfolio)
                .slideId(request != null ? request.getSlideId() : null)
                .referrer(request != null ? request.getReferrer() : null)
                .userAgent(userAgent)
                .eventType("VIEW")
                .build();

        analyticsViewRepository.save(view);

        long totalViews = analyticsViewRepository.countByPortfolioId(portfolio.getId());
        return ViewCountResponseDto.builder()
                .totalViews(totalViews)
                .build();
    }

    private PortfolioResponseDto mapToDto(Portfolio p) {

        long views = p.getId() != null ? analyticsViewRepository.countByPortfolioId(p.getId()) : 0;
        String fullName = p.getUser() != null ? p.getUser().getFullName() : null;
        String avatarUrl = p.getUser() != null ? p.getUser().getAvatarUrl() : null;

        return PortfolioResponseDto.builder()
                .id(p.getId())
                .username(p.getUsername())
                .fullName(fullName)
                .avatarUrl(avatarUrl)
                .title(p.getTitle())
                .tagline(p.getTagline())
                .bio(p.getBio())
                .statusText(p.getStatusText())
                .resumeUrl(p.getResumeUrl())
                .themeMode(p.getThemeMode())
                .themeConfig(p.getThemeConfig())
                .isPublished(p.isPublished())
                .totalViews(views)
                .showExperience(p.isShowExperience())
                .showProjects(p.isShowProjects())
                .showSkills(p.isShowSkills())
                .showEducation(p.isShowEducation())
                .showContact(p.isShowContact())
                .socialLinks(p.getSocialLinks() == null ? List.of() : p.getSocialLinks().stream().map(s -> 
                        PortfolioResponseDto.SocialLinkDto.builder()
                                .platform(s.getPlatform())
                                .url(s.getUrl())
                                .label(s.getLabel())
                                .build()
                ).toList())
                .experiences(p.getExperiences() == null ? List.of() : p.getExperiences().stream().map(e ->
                        PortfolioResponseDto.ExperienceDto.builder()
                                .id(e.getId())
                                .roleTitle(e.getRoleTitle())
                                .companyName(e.getCompanyName())
                                .companyUrl(e.getCompanyUrl())
                                .location(e.getLocation())
                                .periodText(e.getPeriodText())
                                .description(e.getDescription())
                                .highlights(e.getHighlights())
                                .skillsUsed(e.getSkillsUsed())
                                .orderIndex(e.getOrderIndex())
                                .build()
                ).toList())
                .projects(p.getProjects() == null ? List.of() : p.getProjects().stream().map(pr ->
                        PortfolioResponseDto.ProjectDto.builder()
                                .id(pr.getId())
                                .title(pr.getTitle())
                                .subtitle(pr.getSubtitle())
                                .role(pr.getRole())
                                .periodText(pr.getPeriodText())
                                .description(pr.getDescription())
                                .highlights(pr.getHighlights())
                                .techStack(pr.getTechStack())
                                .liveUrl(pr.getLiveUrl())
                                .githubUrl(pr.getGithubUrl())
                                .imageUrl(pr.getImageUrl())
                                .orderIndex(pr.getOrderIndex())
                                .build()
                ).toList())
                .skills(p.getSkills() == null ? List.of() : p.getSkills().stream().map(sk ->
                        PortfolioResponseDto.SkillDto.builder()
                                .id(sk.getId())
                                .category(sk.getCategory())
                                .name(sk.getName())
                                .orderIndex(sk.getOrderIndex())
                                .build()
                ).toList())
                .educations(p.getEducations() == null ? List.of() : p.getEducations().stream().map(ed ->
                        PortfolioResponseDto.EducationDto.builder()
                                .id(ed.getId())
                                .institution(ed.getInstitution())
                                .degree(ed.getDegree())
                                .fieldOfStudy(ed.getFieldOfStudy())
                                .periodText(ed.getPeriodText())
                                .highlights(ed.getHighlights())
                                .orderIndex(ed.getOrderIndex())
                                .build()
                ).toList())
                .build();
    }
}
