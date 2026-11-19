package com.soodfolio.api.dto;

import com.soodfolio.api.dto.PortfolioResponseDto.SocialLinkDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePortfolioRequestDto {

    private String fullName;

    @Pattern(regexp = "^$|^(https?://|/api/v1/uploads/).*", message = "{validation.url.invalid}")
    private String avatarUrl;

    @NotBlank(message = "{validation.portfolio.title.not_blank}")
    private String title;

    private String tagline;
    private String bio;
    private String statusText;

    @Pattern(regexp = "^$|^(https?://|/api/v1/uploads/).*", message = "{validation.url.invalid}")
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

    private List<@Valid SocialLinkDto> socialLinks;
}
