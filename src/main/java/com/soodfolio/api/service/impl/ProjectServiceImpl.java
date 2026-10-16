package com.soodfolio.api.service.impl;

import com.soodfolio.api.dto.PortfolioResponseDto.ProjectDto;
import com.soodfolio.api.dto.ProjectDtos.ProjectRequestDto;
import com.soodfolio.api.entity.Portfolio;
import com.soodfolio.api.entity.Project;
import com.soodfolio.api.exception.ResourceNotFoundException;
import com.soodfolio.api.repository.PortfolioRepository;
import com.soodfolio.api.repository.ProjectRepository;
import com.soodfolio.api.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final PortfolioRepository portfolioRepository;
    private final ProjectRepository projectRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDto> getProjectsByUserId(UUID userId) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "userId", userId));

        return projectRepository.findByPortfolioIdOrderByOrderIndexAsc(portfolio.getId())
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectDto getProjectById(UUID userId, UUID projectId) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "userId", userId));

        Project project = projectRepository.findByIdAndPortfolioId(projectId, portfolio.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        return mapToDto(project);
    }

    @Override
    @Transactional
    public ProjectDto createProject(UUID userId, ProjectRequestDto request) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "userId", userId));

        int currentCount = projectRepository.countByPortfolioId(portfolio.getId());

        Project project = Project.builder()
                .portfolio(portfolio)
                .title(request.getTitle())
                .subtitle(request.getSubtitle())
                .role(request.getRole())
                .periodText(request.getPeriodText())
                .description(request.getDescription())
                .highlights(request.getHighlights() != null ? new ArrayList<>(request.getHighlights()) : new ArrayList<>())
                .techStack(request.getTechStack() != null ? new ArrayList<>(request.getTechStack()) : new ArrayList<>())
                .liveUrl(request.getLiveUrl())
                .githubUrl(request.getGithubUrl())
                .imageUrl(request.getImageUrl())
                .orderIndex(currentCount)
                .build();

        Project saved = projectRepository.save(project);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public ProjectDto updateProject(UUID userId, UUID projectId, ProjectRequestDto request) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "userId", userId));

        Project project = projectRepository.findByIdAndPortfolioId(projectId, portfolio.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        project.setTitle(request.getTitle());
        project.setSubtitle(request.getSubtitle());
        project.setRole(request.getRole());
        project.setPeriodText(request.getPeriodText());
        project.setDescription(request.getDescription());
        if (request.getHighlights() != null) {
            project.setHighlights(new ArrayList<>(request.getHighlights()));
        }
        if (request.getTechStack() != null) {
            project.setTechStack(new ArrayList<>(request.getTechStack()));
        }
        project.setLiveUrl(request.getLiveUrl());
        project.setGithubUrl(request.getGithubUrl());
        project.setImageUrl(request.getImageUrl());

        Project saved = projectRepository.save(project);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public void deleteProject(UUID userId, UUID projectId) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "userId", userId));

        Project project = projectRepository.findByIdAndPortfolioId(projectId, portfolio.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        projectRepository.delete(project);
    }

    @Override
    @Transactional
    public List<ProjectDto> reorderProjects(UUID userId, List<UUID> projectIds) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "userId", userId));

        List<Project> existingProjects = projectRepository.findByPortfolioIdOrderByOrderIndexAsc(portfolio.getId());
        Map<UUID, Project> map = existingProjects.stream()
                .collect(Collectors.toMap(Project::getId, p -> p));

        for (int i = 0; i < projectIds.size(); i++) {
            Project p = map.get(projectIds.get(i));
            if (p != null) {
                p.setOrderIndex(i);
            }
        }
        projectRepository.saveAll(existingProjects);

        return projectRepository.findByPortfolioIdOrderByOrderIndexAsc(portfolio.getId())
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    private ProjectDto mapToDto(Project p) {
        return ProjectDto.builder()
                .id(p.getId())
                .title(p.getTitle())
                .subtitle(p.getSubtitle())
                .role(p.getRole())
                .periodText(p.getPeriodText())
                .description(p.getDescription())
                .highlights(p.getHighlights() != null ? p.getHighlights() : List.of())
                .techStack(p.getTechStack() != null ? p.getTechStack() : List.of())
                .liveUrl(p.getLiveUrl())
                .githubUrl(p.getGithubUrl())
                .imageUrl(p.getImageUrl())
                .orderIndex(p.getOrderIndex())
                .build();
    }
}
