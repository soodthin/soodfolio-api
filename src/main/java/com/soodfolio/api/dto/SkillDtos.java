package com.soodfolio.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;
import java.util.UUID;

public class SkillDtos {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SkillRequestDto {
        @NotBlank(message = "{validation.skill.category.not_blank}")
        private String category;

        @NotBlank(message = "{validation.skill.name.not_blank}")
        private String name;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SkillReorderRequestDto {
        @NotEmpty(message = "{validation.reorder.ids.not_empty}")
        private List<UUID> skillIds;
    }
}
