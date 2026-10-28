package com.soodfolio.api.controller;

import com.soodfolio.api.dto.GitHubImportProjectRequest;
import com.soodfolio.api.dto.GitHubRepoDto;
import com.soodfolio.api.dto.PortfolioResponseDto.ProjectDto;
import com.soodfolio.api.entity.Portfolio;
import com.soodfolio.api.entity.SocialLink;
import com.soodfolio.api.exception.BadRequestException;
import com.soodfolio.api.exception.InvalidCredentialsException;
import com.soodfolio.api.exception.ResourceNotFoundException;

import com.soodfolio.api.repository.PortfolioRepository;
import com.soodfolio.api.repository.SocialLinkRepository;
import com.soodfolio.api.security.UserPrincipal;
import com.soodfolio.api.service.GitHubService;
import com.soodfolio.api.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/portfolios/me/github")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "GitHub Integration", description = "APIs for GitHub integration and 1-click project repository import")
@SecurityRequirement(name = "bearerAuth")
public class GitHubProjectController {

    private final GitHubService gitHubService;
    private final ProjectService projectService;
    private final PortfolioRepository portfolioRepository;
    private final SocialLinkRepository socialLinkRepository;

    @GetMapping("/repos")
    @Operation(
            summary = "Get public repositories from GitHub",
            description = "Fetches public repos of the provided username. If omitted, automatically detects username from creator's GitHub SocialLink."
    )
    public ResponseEntity<List<GitHubRepoDto>> getGitHubRepos(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "Optional GitHub username. If omitted, fetched from portfolio social link")
            @RequestParam(required = false) String username
    ) {
        validateAuthenticated(userPrincipal);

        String targetUsername = username;
        if (targetUsername == null || targetUsername.trim().isEmpty()) {
            Portfolio portfolio = portfolioRepository.findByUserId(userPrincipal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "userId", userPrincipal.getId()));

            Optional<SocialLink> githubLink = socialLinkRepository.findFirstByPortfolioIdAndPlatformIgnoreCase(portfolio.getId(), "GITHUB");

            if (githubLink.isPresent() && githubLink.get().getUrl() != null && !githubLink.get().getUrl().isBlank()) {
                targetUsername = extractGitHubUsername(githubLink.get().getUrl());
            }

            if (targetUsername == null || targetUsername.isBlank()) {
                throw new BadRequestException("Không tìm thấy liên kết GitHub trong hồ sơ. Vui lòng cung cấp tham số username.");
            }

        }

        List<GitHubRepoDto> repos = gitHubService.getUserRepos(targetUsername.trim());
        return ResponseEntity.ok(repos);
    }

    @PostMapping("/import")
    @Operation(summary = "1-Click import a public GitHub repository as a new project slide")
    public ResponseEntity<ProjectDto> importProject(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody GitHubImportProjectRequest request
    ) {
        validateAuthenticated(userPrincipal);
        ProjectDto imported = projectService.importFromGitHub(userPrincipal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(imported);
    }

    private void validateAuthenticated(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new InvalidCredentialsException("Yêu cầu đăng nhập để thực hiện thao tác này.");
        }
    }

    private String extractGitHubUsername(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String cleaned = input.trim();
        while (cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        if (cleaned.startsWith("https://")) {
            cleaned = cleaned.substring(8);
        } else if (cleaned.startsWith("http://")) {
            cleaned = cleaned.substring(7);
        }
        if (cleaned.startsWith("www.github.com/")) {
            cleaned = cleaned.substring(15);
        } else if (cleaned.startsWith("github.com/")) {
            cleaned = cleaned.substring(11);
        }
        int slashIndex = cleaned.indexOf('/');
        if (slashIndex > 0) {
            cleaned = cleaned.substring(0, slashIndex);
        }
        return cleaned.isBlank() ? null : cleaned;
    }
}
