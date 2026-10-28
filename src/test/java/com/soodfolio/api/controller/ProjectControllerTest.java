package com.soodfolio.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soodfolio.api.dto.PortfolioResponseDto.ProjectDto;
import com.soodfolio.api.dto.ProjectDtos.ProjectReorderRequestDto;
import com.soodfolio.api.dto.ProjectDtos.ProjectRequestDto;
import com.soodfolio.api.exception.GlobalExceptionHandler;
import com.soodfolio.api.exception.ResourceNotFoundException;
import com.soodfolio.api.security.UserPrincipal;
import com.soodfolio.api.service.I18nService;
import com.soodfolio.api.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProjectControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProjectService projectService;

    @Mock
    private I18nService i18nService;

    @InjectMocks
    private ProjectController projectController;

    private ObjectMapper objectMapper;
    private UserPrincipal testPrincipal;
    private UUID userId;
    private ProjectDto sampleProjectDto;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        userId = UUID.randomUUID();
        testPrincipal = new UserPrincipal(
                userId,
                "engineer@soodfolio.dev",
                "password",
                "Engineer",
                org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_USER")
        );

        sampleProjectDto = ProjectDto.builder()
                .id(UUID.randomUUID())
                .title("SoodFolio Core")
                .subtitle("Next.js 16 + Spring Boot 3.4")
                .orderIndex(0)
                .techStack(List.of("Java", "PostgreSQL"))
                .build();

        // Custom argument resolver to inject testPrincipal into @AuthenticationPrincipal
        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                // If header "X-Anonymous" is present, simulate unauthenticated request
                if ("true".equalsIgnoreCase(webRequest.getHeader("X-Anonymous"))) {
                    return null;
                }
                return testPrincipal;
            }
        };

        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler(i18nService);

        mockMvc = MockMvcBuilders.standaloneSetup(projectController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .setControllerAdvice(exceptionHandler)
                .build();

        lenient().when(i18nService.getMessage(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("GET /portfolios/me/projects - 200 OK when authenticated")
    void shouldReturnProjectsWhenAuthenticated() throws Exception {
        when(projectService.getProjectsByUserId(userId)).thenReturn(List.of(sampleProjectDto));

        mockMvc.perform(get("/portfolios/me/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("SoodFolio Core"))
                .andExpect(jsonPath("$[0].orderIndex").value(0));
    }

    @Test
    @DisplayName("GET /portfolios/me/projects - 401 Unauthorized when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/portfolios/me/projects")
                        .header("X-Anonymous", "true"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /portfolios/me/projects - 201 Created on valid request")
    void shouldCreateProjectSuccessfully() throws Exception {
        ProjectRequestDto request = ProjectRequestDto.builder()
                .title("New Project")
                .subtitle("Architecture")
                .build();

        when(projectService.createProject(eq(userId), any(ProjectRequestDto.class))).thenReturn(sampleProjectDto);

        mockMvc.perform(post("/portfolios/me/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("SoodFolio Core"));
    }

    @Test
    @DisplayName("POST /portfolios/me/projects - 400 Bad Request when title is blank (@Valid)")
    void shouldRejectInvalidProjectPayload() throws Exception {
        ProjectRequestDto invalidRequest = ProjectRequestDto.builder()
                .title("") // Blank title violates @NotBlank
                .build();

        mockMvc.perform(post("/portfolios/me/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.title").exists());
    }

    @Test
    @DisplayName("GET /portfolios/me/projects/{id} - 404 Not Found when project does not exist")
    void shouldReturn404WhenNotFound() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(projectService.getProjectById(userId, unknownId))
                .thenThrow(new ResourceNotFoundException("Project", "id", unknownId));

        mockMvc.perform(get("/portfolios/me/projects/" + unknownId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("DELETE /portfolios/me/projects/{id} - 204 No Content")
    void shouldDeleteProjectSuccessfully() throws Exception {
        UUID projectId = sampleProjectDto.getId();
        doNothing().when(projectService).deleteProject(userId, projectId);

        mockMvc.perform(delete("/portfolios/me/projects/" + projectId))
                .andExpect(status().isNoContent());

        verify(projectService).deleteProject(userId, projectId);
    }

    @Test
    @DisplayName("PUT /portfolios/me/projects/reorder - 400 Bad Request when projectIds list is empty (@Valid)")
    void shouldRejectEmptyReorderList() throws Exception {
        ProjectReorderRequestDto invalidReorder = ProjectReorderRequestDto.builder()
                .projectIds(List.of()) // Empty list violates @NotEmpty
                .build();

        mockMvc.perform(put("/portfolios/me/projects/reorder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReorder)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /portfolios/me/projects/import-github - 201 Created when importing GitHub repository")
    void shouldImportGitHubRepoViaProjectEndpoint() throws Exception {
        com.soodfolio.api.dto.GitHubImportProjectRequest importRequest = com.soodfolio.api.dto.GitHubImportProjectRequest.builder()
                .repoName("cloud-native-gateway")
                .githubUrl("https://github.com/alexdev/cloud-native-gateway")
                .language("Kotlin")
                .topics(List.of("k8s", "gateway"))
                .build();

        ProjectDto importedDto = ProjectDto.builder()
                .id(UUID.randomUUID())
                .title("Cloud Native Gateway")
                .subtitle("cloud-native-gateway")
                .techStack(List.of("Kotlin", "k8s", "gateway"))
                .build();

        when(projectService.importFromGitHub(eq(userId), any(com.soodfolio.api.dto.GitHubImportProjectRequest.class)))
                .thenReturn(importedDto);

        mockMvc.perform(post("/portfolios/me/projects/import-github")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(importRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Cloud Native Gateway"))
                .andExpect(jsonPath("$.subtitle").value("cloud-native-gateway"));

        verify(projectService).importFromGitHub(eq(userId), any(com.soodfolio.api.dto.GitHubImportProjectRequest.class));
    }

    @Test
    @DisplayName("PUT /portfolios/me/projects/{id} - 409 Conflict when concurrent modification causes ObjectOptimisticLockingFailureException")
    void shouldReturn409ConflictOnObjectOptimisticLockingFailure() throws Exception {
        UUID projectId = sampleProjectDto.getId();
        ProjectRequestDto updateReq = ProjectRequestDto.builder()
                .title("Concurrent Project Update")
                .build();

        when(projectService.updateProject(eq(userId), eq(projectId), any(ProjectRequestDto.class)))
                .thenThrow(new org.springframework.orm.ObjectOptimisticLockingFailureException("Project", projectId));

        mockMvc.perform(put("/portfolios/me/projects/{id}", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("common.concurrency_conflict"));
    }

    @Test
    @DisplayName("PUT /portfolios/me/projects/{id} - 409 Conflict when concurrent modification causes OptimisticLockException")
    void shouldReturn409ConflictOnOptimisticLockException() throws Exception {
        UUID projectId = sampleProjectDto.getId();
        ProjectRequestDto updateReq = ProjectRequestDto.builder()
                .title("Concurrent Project Update")
                .build();

        when(projectService.updateProject(eq(userId), eq(projectId), any(ProjectRequestDto.class)))
                .thenThrow(new jakarta.persistence.OptimisticLockException("Row modified concurrently"));

        mockMvc.perform(put("/portfolios/me/projects/{id}", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("common.concurrency_conflict"));
    }
}

