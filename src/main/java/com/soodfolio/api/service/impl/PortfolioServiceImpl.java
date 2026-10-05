package com.soodfolio.api.service.impl;

import com.soodfolio.api.dto.PortfolioResponseDto;
import com.soodfolio.api.entity.Portfolio;
import com.soodfolio.api.repository.PortfolioRepository;
import com.soodfolio.api.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PortfolioServiceImpl implements PortfolioService {

    private final PortfolioRepository portfolioRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<PortfolioResponseDto> getPortfolioByUsername(String username) {
        return portfolioRepository.findByUsername(username)
                .map(this::mapToDto);
    }

    private PortfolioResponseDto mapToDto(Portfolio p) {
        return PortfolioResponseDto.builder()
                .id(p.getId())
                .username(p.getUsername())
                .title(p.getTitle())
                .tagline(p.getTagline())
                .bio(p.getBio())
                .statusText(p.getStatusText())
                .resumeUrl(p.getResumeUrl())
                .themeMode(p.getThemeMode())
                .showExperience(p.isShowExperience())
                .showProjects(p.isShowProjects())
                .showSkills(p.isShowSkills())
                .showEducation(p.isShowEducation())
                .showContact(p.isShowContact())
                .socialLinks(p.getSocialLinks().stream().map(s -> 
                        PortfolioResponseDto.SocialLinkDto.builder()
                                .platform(s.getPlatform())
                                .url(s.getUrl())
                                .label(s.getLabel())
                                .build()
                ).toList())
                .experiences(p.getExperiences().stream().map(e ->
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
                                .build()
                ).toList())
                .projects(p.getProjects().stream().map(pr ->
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
                                .build()
                ).toList())
                .skills(p.getSkills().stream().map(sk ->
                        PortfolioResponseDto.SkillDto.builder()
                                .id(sk.getId())
                                .category(sk.getCategory())
                                .name(sk.getName())
                                .build()
                ).toList())
                .educations(p.getEducations().stream().map(ed ->
                        PortfolioResponseDto.EducationDto.builder()
                                .id(ed.getId())
                                .institution(ed.getInstitution())
                                .degree(ed.getDegree())
                                .fieldOfStudy(ed.getFieldOfStudy())
                                .periodText(ed.getPeriodText())
                                .highlights(ed.getHighlights())
                                .build()
                ).toList())
                .build();
    }
}
