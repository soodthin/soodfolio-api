package com.soodfolio.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

public class AiDtos {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AiPolishRequestDto {

        @NotBlank(message = "Nội dung văn bản không được để trống.")
        @Size(max = 2000, message = "Nội dung văn bản không được vượt quá 2000 ký tự.")
        private String rawText;

        @NotBlank(message = "Loại trường thông tin không được để trống.")
        @Pattern(
                regexp = "^(project_desc|exp_bullet|bio)$",
                message = "Trường 'field' phải là một trong các giá trị: project_desc, exp_bullet, bio"
        )
        private String field;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AiPolishResponseDto {
        private String metricXyz;
        private String architectural;
        private String concise;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AiThemeSuggestRequestDto {

        @NotBlank(message = "Tiêu đề chức danh không được để trống.")
        @Size(max = 200, message = "Tiêu đề không được vượt quá 200 ký tự.")
        private String title;

        @Size(max = 2000, message = "Bio không được vượt quá 2000 ký tự.")
        private String bio;

        private List<String> skills;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AiThemeSuggestResponseDto {
        private String recommendedTemplateId;
        private String reason;
    }
}
