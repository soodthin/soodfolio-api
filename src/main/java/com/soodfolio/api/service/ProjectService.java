package com.soodfolio.api.service;

import com.soodfolio.api.dto.GitHubImportProjectRequest;
import com.soodfolio.api.dto.PortfolioResponseDto.ProjectDto;
import com.soodfolio.api.dto.ProjectDtos.ProjectRequestDto;

import java.util.List;
import java.util.UUID;

public interface ProjectService {
    List<ProjectDto> getProjectsByUserId(UUID userId);
    ProjectDto getProjectById(UUID userId, UUID projectId);
    ProjectDto createProject(UUID userId, ProjectRequestDto request);
    ProjectDto updateProject(UUID userId, UUID projectId, ProjectRequestDto request);
    void deleteProject(UUID userId, UUID projectId);
    List<ProjectDto> reorderProjects(UUID userId, List<UUID> projectIds);
    ProjectDto importFromGitHub(UUID userId, GitHubImportProjectRequest request);
}


