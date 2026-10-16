package com.soodfolio.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;
import java.util.UUID;

public class ProjectDtos {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProjectRequestDto {
        @NotBlank(message = "{validation.project.title.not_blank}")
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
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProjectReorderRequestDto {
        @NotEmpty(message = "{validation.reorder.ids.not_empty}")
        private List<UUID> projectIds;
    }
}
