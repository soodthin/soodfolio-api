package com.soodfolio.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PortfolioResponseDto {
    private UUID id;
    private String username;
    private String fullName;
    private String avatarUrl;
    private String title;
    private String tagline;
    private String bio;
    private String statusText;
    private String resumeUrl;
    private String themeMode;
    @JsonProperty("isPublished")
    private boolean isPublished;
    private long totalViews;
    private boolean showExperience;
    private boolean showProjects;
    private boolean showSkills;
    private boolean showEducation;
    private boolean showContact;
    
    private List<SocialLinkDto> socialLinks;
    private List<ExperienceDto> experiences;
    private List<ProjectDto> projects;
    private List<SkillDto> skills;
    private List<EducationDto> educations;

    @Data
    @Builder
    public static class SocialLinkDto {
        private String platform;
        private String url;
        private String label;
    }

    @Data
    @Builder
    public static class ExperienceDto {
        private UUID id;
        private String roleTitle;
        private String companyName;
        private String companyUrl;
        private String location;
        private String periodText;
        private String description;
        private List<String> highlights;
        private List<String> skillsUsed;
        private int orderIndex;
    }

    @Data
    @Builder
    public static class ProjectDto {
        private UUID id;
        private String title;
        private String subtitle;
        private String role;
        private String periodText;
        private String description;
        private List<String> highlights;
        private List<String> techStack;
        private String liveUrl;
        private String githubUrl;
        private String imageUrl;
        private int orderIndex;
    }

    @Data
    @Builder
    public static class SkillDto {
        private UUID id;
        private String category;
        private String name;
        private int orderIndex;
    }

    @Data
    @Builder
    public static class EducationDto {
        private UUID id;
        private String institution;
        private String degree;
        private String fieldOfStudy;
        private String periodText;
        private String highlights;
        private int orderIndex;
    }

    @Data
    @Builder
    public static class PortfolioExportDto {
        private String exportVersion;
        private LocalDateTime exportedAt;
        private String generator;
        private PortfolioResponseDto portfolio;
    }
}
