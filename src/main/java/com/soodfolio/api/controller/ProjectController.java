package com.soodfolio.api.controller;

import com.soodfolio.api.dto.GitHubImportProjectRequest;
import com.soodfolio.api.dto.PortfolioResponseDto.ProjectDto;
import com.soodfolio.api.dto.ProjectDtos.ProjectReorderRequestDto;
import com.soodfolio.api.dto.ProjectDtos.ProjectRequestDto;

import com.soodfolio.api.exception.InvalidCredentialsException;
import com.soodfolio.api.security.UserPrincipal;
import com.soodfolio.api.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/portfolios/me/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Slide Deck Project APIs for Creators")
@SecurityRequirement(name = "bearerAuth")
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    @Operation(summary = "Get all projects for current creator")
    public ResponseEntity<List<ProjectDto>> getMyProjects(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        validateAuthenticated(userPrincipal);
        return ResponseEntity.ok(projectService.getProjectsByUserId(userPrincipal.getId()));
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "Get single project by ID")
    public ResponseEntity<ProjectDto> getProjectById(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID projectId
    ) {
        validateAuthenticated(userPrincipal);
        return ResponseEntity.ok(projectService.getProjectById(userPrincipal.getId(), projectId));
    }

    @PostMapping
    @Operation(summary = "Create a new project slide")
    public ResponseEntity<ProjectDto> createProject(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ProjectRequestDto request
    ) {
        validateAuthenticated(userPrincipal);
        ProjectDto created = projectService.createProject(userPrincipal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/import-github")
    @Operation(summary = "1-Click import a public GitHub repository as a new project slide")
    public ResponseEntity<ProjectDto> importGitHubProject(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody GitHubImportProjectRequest request
    ) {
        validateAuthenticated(userPrincipal);
        ProjectDto imported = projectService.importFromGitHub(userPrincipal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(imported);
    }


    @PutMapping("/{projectId}")
    @Operation(summary = "Update an existing project slide")
    public ResponseEntity<ProjectDto> updateProject(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID projectId,
            @Valid @RequestBody ProjectRequestDto request
    ) {
        validateAuthenticated(userPrincipal);
        ProjectDto updated = projectService.updateProject(userPrincipal.getId(), projectId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{projectId}")
    @Operation(summary = "Delete a project slide (soft delete)")
    public ResponseEntity<Void> deleteProject(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID projectId
    ) {
        validateAuthenticated(userPrincipal);
        projectService.deleteProject(userPrincipal.getId(), projectId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    @Operation(summary = "Reorder project slides")
    public ResponseEntity<List<ProjectDto>> reorderProjects(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ProjectReorderRequestDto request
    ) {
        validateAuthenticated(userPrincipal);
        List<ProjectDto> reordered = projectService.reorderProjects(userPrincipal.getId(), request.getProjectIds());
        return ResponseEntity.ok(reordered);
    }

    private void validateAuthenticated(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new InvalidCredentialsException("Yêu cầu đăng nhập để thực hiện thao tác này.");
        }
    }
}
