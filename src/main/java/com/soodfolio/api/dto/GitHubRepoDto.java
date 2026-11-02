package com.soodfolio.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "GitHub Repository Information DTO")
public class GitHubRepoDto {

    @Schema(description = "Repository name", example = "soodfolio")
    private String name;

    @JsonProperty("full_name")
    @Schema(description = "Full repository name including owner", example = "octocat/soodfolio")
    private String fullName;

    @Schema(description = "Repository description", example = "Interactive slide-deck developer portfolio")
    private String description;

    @JsonProperty("html_url")
    @Schema(description = "GitHub repository web URL", example = "https://github.com/octocat/soodfolio")
    private String htmlUrl;

    @Schema(description = "Repository homepage or live demo URL", example = "https://soodfolio.dev")
    private String homepage;

    @Schema(description = "Primary programming language", example = "Java")
    private String language;

    @Builder.Default
    @Schema(description = "List of repository topics/tags", example = "[\"spring-boot\", \"nextjs\", \"postgresql\"]")
    private List<String> topics = new ArrayList<>();

    @JsonProperty("stargazers_count")
    @Schema(description = "Number of stargazers", example = "42")
    private int stargazersCount;

    @JsonProperty("forks_count")
    @Schema(description = "Number of forks", example = "10")
    private int forksCount;

    @JsonProperty("updated_at")
    @Schema(description = "ISO-8601 timestamp of last update", example = "2026-03-15T12:00:00Z")
    private String updatedAt;
}
