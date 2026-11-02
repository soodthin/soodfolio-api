package com.soodfolio.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload to import a GitHub repository as a portfolio project slide")
public class GitHubImportProjectRequest {

    @NotBlank(message = "Tên repository không được để trống")
    @Schema(description = "GitHub repository name", example = "soodfolio-api", requiredMode = Schema.RequiredMode.REQUIRED)
    private String repoName;

    @Schema(description = "GitHub repository URL", example = "https://github.com/octocat/soodfolio-api")
    private String githubUrl;

    @Schema(description = "Project description or summary", example = "High-performance Spring Boot 3 API for SoodFolio")
    private String description;

    @Schema(description = "Primary language", example = "Java")
    private String language;

    @Builder.Default
    @Schema(description = "List of repository topics / technologies", example = "[\"spring-boot\", \"postgresql\", \"docker\"]")
    private List<String> topics = new ArrayList<>();

    @Schema(description = "Live demo or production URL", example = "https://api.soodfolio.dev")
    private String liveUrl;

    @Schema(description = "Role played in the project", example = "Backend Lead")
    private String role;

    @Schema(description = "Development timeframe or period text", example = "2024 - Present")
    private String periodText;
}
