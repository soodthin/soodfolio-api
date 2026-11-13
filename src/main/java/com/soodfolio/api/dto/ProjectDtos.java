package com.soodfolio.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
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

        @Pattern(regexp = "^$|^https?://.*", message = "{validation.url.invalid}")
        private String liveUrl;

        @Pattern(regexp = "^$|^https?://.*", message = "{validation.url.invalid}")
        private String githubUrl;

        @Pattern(regexp = "^$|^(https?://|/api/v1/uploads/).*", message = "{validation.url.invalid}")
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
