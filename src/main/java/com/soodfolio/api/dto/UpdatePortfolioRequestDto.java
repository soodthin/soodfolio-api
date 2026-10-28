package com.soodfolio.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePortfolioRequestDto {

    @NotBlank(message = "{validation.portfolio.title.not_blank}")
    private String title;

    private String tagline;
    private String bio;
    private String statusText;
    private String resumeUrl;
    private String themeMode;
    private String themeConfig;

    @Builder.Default
    private boolean showExperience = true;

    @Builder.Default
    private boolean showProjects = true;

    @Builder.Default
    private boolean showSkills = true;

    @Builder.Default
    private boolean showEducation = true;

    @Builder.Default
    private boolean showContact = true;

    private List<PortfolioResponseDto.SocialLinkDto> socialLinks;
}
