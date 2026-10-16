package com.soodfolio.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;
import java.util.UUID;

public class ExperienceDtos {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExperienceRequestDto {
        @NotBlank(message = "{validation.role.not_blank}")
        private String roleTitle;

        @NotBlank(message = "{validation.company.not_blank}")
        private String companyName;

        private String companyUrl;
        private String location;
        private String periodText;
        private String description;
        private List<String> highlights;
        private List<String> skillsUsed;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExperienceReorderRequestDto {
        @NotEmpty(message = "{validation.reorder.ids.not_empty}")
        private List<UUID> experienceIds;
    }
}
