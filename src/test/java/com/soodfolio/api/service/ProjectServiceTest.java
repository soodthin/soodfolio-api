package com.soodfolio.api.service;

import com.soodfolio.api.dto.PortfolioResponseDto.ProjectDto;
import com.soodfolio.api.dto.ProjectDtos.ProjectRequestDto;
import com.soodfolio.api.entity.Portfolio;
import com.soodfolio.api.entity.Project;
import com.soodfolio.api.entity.User;
import com.soodfolio.api.exception.ResourceNotFoundException;
import com.soodfolio.api.repository.PortfolioRepository;
import com.soodfolio.api.repository.ProjectRepository;
import com.soodfolio.api.service.impl.ProjectServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private UUID userId;
    private Portfolio portfolio;
    private Project project;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        portfolio = Portfolio.builder()
                .id(UUID.randomUUID())
                .user(User.builder().id(userId).build())
                .username("soodthin")
                .build();

        project = Project.builder()
                .id(UUID.randomUUID())
                .portfolio(portfolio)
                .title("SoodFolio Interactive Deck")
                .subtitle("Next.js 16 + Spring Boot")
                .role("Lead Fullstack Engineer")
                .periodText("2026 — Present")
                .description("Card Stacking & Depth Pinning Portfolio Platform")
                .highlights(new ArrayList<>(List.of("Sub-50ms latency", "Stateless dual-token")))
                .techStack(new ArrayList<>(List.of("Java 21", "Spring Boot", "Next.js 16")))
                .liveUrl("https://soodfolio.dev")
                .githubUrl("https://github.com/soodfolio/soodfolio")
                .imageUrl("https://soodfolio.dev/preview.png")
                .orderIndex(0)
                .build();
    }

    @Test
    @DisplayName("Should return all projects ordered by orderIndex for the given user")
    void shouldReturnProjectsByUserId() {
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(projectRepository.findByPortfolioIdOrderByOrderIndexAsc(portfolio.getId()))
                .thenReturn(List.of(project));

        List<ProjectDto> results = projectService.getProjectsByUserId(userId);

        assertThat(results).hasSize(1);
        ProjectDto dto = results.get(0);
        assertThat(dto.getId()).isEqualTo(project.getId());
        assertThat(dto.getTitle()).isEqualTo("SoodFolio Interactive Deck");
        assertThat(dto.getOrderIndex()).isEqualTo(0);
        assertThat(dto.getTechStack()).contains("Java 21", "Spring Boot");
    }

    @Test
    @DisplayName("Should return project detail by project ID and user ID")
    void shouldReturnProjectById() {
        UUID projectId = project.getId();
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(projectRepository.findByIdAndPortfolioId(projectId, portfolio.getId()))
                .thenReturn(Optional.of(project));

        ProjectDto result = projectService.getProjectById(userId, projectId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(projectId);
        assertThat(result.getTitle()).isEqualTo(project.getTitle());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when project does not exist")
    void shouldThrowWhenProjectNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(projectRepository.findByIdAndPortfolioId(unknownId, portfolio.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProjectById(userId, unknownId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Project not found with id");
    }

    @Test
    @DisplayName("Should successfully create a new project slide with auto-incremented orderIndex")
    void shouldCreateProjectSuccessfully() {
        ProjectRequestDto request = ProjectRequestDto.builder()
                .title("FlexiConnect Gateway")
                .subtitle("High throughput API router")
                .role("Backend Architect")
                .periodText("2025")
                .description("Resilient microservices orchestrator")
                .highlights(List.of("100k RPS", "99.99% Uptime"))
                .techStack(List.of("Go", "PostgreSQL", "Kafka"))
                .liveUrl("https://flexi.dev")
                .githubUrl("https://github.com/flexi/gateway")
                .imageUrl("https://flexi.dev/banner.png")
                .build();

        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(projectRepository.countByPortfolioId(portfolio.getId())).thenReturn(1);
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project p = invocation.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        ProjectDto created = projectService.createProject(userId, request);

        assertThat(created).isNotNull();
        assertThat(created.getTitle()).isEqualTo("FlexiConnect Gateway");
        assertThat(created.getOrderIndex()).isEqualTo(1);
        assertThat(created.getHighlights()).contains("100k RPS");
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    @DisplayName("Should successfully update existing project slide")
    void shouldUpdateProjectSuccessfully() {
        UUID projectId = project.getId();
        ProjectRequestDto updateRequest = ProjectRequestDto.builder()
                .title("SoodFolio V2 Enterprise")
                .subtitle("Updated Subtitle")
                .role("Principal Engineer")
                .periodText("2026 — 2027")
                .description("Updated Description")
                .highlights(List.of("Optimized Neo-Kinpaku UI"))
                .techStack(List.of("Java 21", "Next.js 16", "Tailwind v4"))
                .liveUrl("https://soodfolio.dev/v2")
                .githubUrl("https://github.com/soodfolio/core")
                .imageUrl("https://soodfolio.dev/v2.png")
                .build();

        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(projectRepository.findByIdAndPortfolioId(projectId, portfolio.getId()))
                .thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectDto updated = projectService.updateProject(userId, projectId, updateRequest);

        assertThat(updated).isNotNull();
        assertThat(updated.getTitle()).isEqualTo("SoodFolio V2 Enterprise");
        assertThat(updated.getRole()).isEqualTo("Principal Engineer");
        assertThat(project.getTitle()).isEqualTo("SoodFolio V2 Enterprise");
        verify(projectRepository).save(project);
    }

    @Test
    @DisplayName("Should successfully delete project slide")
    void shouldDeleteProjectSuccessfully() {
        UUID projectId = project.getId();
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(projectRepository.findByIdAndPortfolioId(projectId, portfolio.getId()))
                .thenReturn(Optional.of(project));

        projectService.deleteProject(userId, projectId);

        verify(projectRepository).delete(project);
    }

    @Test
    @DisplayName("Should reorder projects correctly based on provided ID order")
    void shouldReorderProjectsSuccessfully() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Project p1 = Project.builder().id(id1).portfolio(portfolio).orderIndex(0).build();
        Project p2 = Project.builder().id(id2).portfolio(portfolio).orderIndex(1).build();

        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(projectRepository.findByPortfolioIdOrderByOrderIndexAsc(portfolio.getId()))
                .thenReturn(new ArrayList<>(List.of(p1, p2)));

        List<ProjectDto> reordered = projectService.reorderProjects(userId, List.of(id2, id1));

        assertThat(reordered).hasSize(2);
        assertThat(p2.getOrderIndex()).isEqualTo(0);
        assertThat(p1.getOrderIndex()).isEqualTo(1);
        verify(projectRepository).saveAll(any());
    }

    @Test
    @DisplayName("Should import project from GitHub successfully with formatted title and tech stack")
    void shouldImportProjectFromGitHubSuccessfully() {
        com.soodfolio.api.dto.GitHubImportProjectRequest importReq = com.soodfolio.api.dto.GitHubImportProjectRequest.builder()
                .repoName("microservice-order-hub")
                .description("Order processing system with event-driven architecture")
                .language("Java")
                .topics(List.of("kafka", "docker", "spring-boot"))
                .githubUrl("https://github.com/testuser/microservice-order-hub")
                .liveUrl("https://orders.example.com")
                .role("Backend Lead")
                .periodText("2024 - 2025")
                .build();

        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(projectRepository.countByPortfolioId(portfolio.getId())).thenReturn(2);
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        ProjectDto result = projectService.importFromGitHub(userId, importReq);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Microservice Order Hub");
        assertThat(result.getSubtitle()).isEqualTo("microservice-order-hub");
        assertThat(result.getRole()).isEqualTo("Backend Lead");
        assertThat(result.getPeriodText()).isEqualTo("2024 - 2025");
        assertThat(result.getTechStack()).containsExactly("Java", "kafka", "docker", "spring-boot");
        assertThat(result.getHighlights()).isNotEmpty();
        assertThat(result.getOrderIndex()).isEqualTo(2);
        assertThat(result.getGithubUrl()).isEqualTo("https://github.com/testuser/microservice-order-hub");

        verify(projectRepository).save(any(Project.class));
    }

    @Test
    @DisplayName("Should format complex kebab-case and snake_case repo names, deduplicate techStack, and fallback defaults")
    void shouldFormatComplexKebabAndSnakeCaseRepoNames() {
        com.soodfolio.api.dto.GitHubImportProjectRequest complexReq = com.soodfolio.api.dto.GitHubImportProjectRequest.builder()
                .repoName("ultra_fast-distributed_cache-v2_service")
                .description("In-memory distributed caching engine")
                .language("Rust")
                .topics(List.of("rust", "distributed-systems", "CACHE", "  grpc  "))
                .githubUrl("https://github.com/testuser/ultra_fast-distributed_cache-v2_service")
                .role(null)       // Should fallback to default role
                .periodText("  ") // Should fallback to default periodText
                .build();

        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(projectRepository.countByPortfolioId(portfolio.getId())).thenReturn(4);
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        ProjectDto result = projectService.importFromGitHub(userId, complexReq);

        assertThat(result).isNotNull();
        // Title capitalized per token from split by dash, underscore, whitespace
        assertThat(result.getTitle()).isEqualTo("Ultra Fast Distributed Cache V2 Service");
        assertThat(result.getSubtitle()).isEqualTo("ultra_fast-distributed_cache-v2_service");
        // Role & periodText defaults
        assertThat(result.getRole()).isEqualTo("Lead Developer / Creator");
        assertThat(result.getPeriodText()).isEqualTo("Present");
        // Tech stack: "Rust" added first, "rust" in topics deduplicated case-insensitively, "CACHE", "grpc" trimmed
        assertThat(result.getTechStack()).containsExactly("Rust", "distributed-systems", "CACHE", "grpc");
        // OrderIndex sequentially increments from current project count (4)
        assertThat(result.getOrderIndex()).isEqualTo(4);

        verify(projectRepository).save(any(Project.class));
    }

    @Test
    @DisplayName("Should auto-increment orderIndex to 0 when importing as the very first project")
    void shouldAutoIncrementOrderIndexToZeroWhenNoExistingProjects() {
        com.soodfolio.api.dto.GitHubImportProjectRequest importReq = com.soodfolio.api.dto.GitHubImportProjectRequest.builder()
                .repoName("first-repo")
                .githubUrl("https://github.com/testuser/first-repo")
                .build();

        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(projectRepository.countByPortfolioId(portfolio.getId())).thenReturn(0);
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        ProjectDto result = projectService.importFromGitHub(userId, importReq);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("First Repo");
        assertThat(result.getOrderIndex()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when portfolio is missing")
    void shouldThrowWhenPortfolioMissing() {
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProjectsByUserId(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Portfolio not found with userId");
    }
}

