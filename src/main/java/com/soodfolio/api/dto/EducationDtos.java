package com.soodfolio.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;
import java.util.UUID;

public class EducationDtos {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EducationRequestDto {
        @NotBlank(message = "{validation.institution.not_blank}")
        private String institution;

        @NotBlank(message = "{validation.degree.not_blank}")
        private String degree;

        private String fieldOfStudy;

        @NotBlank(message = "{validation.period.not_blank}")
        private String periodText;

        private String highlights;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EducationReorderRequestDto {
        @NotEmpty(message = "{validation.reorder.ids.not_empty}")
        private List<UUID> educationIds;
    }
}
