package com.soodfolio.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soodfolio.api.dto.GitHubImportProjectRequest;
import com.soodfolio.api.dto.GitHubRepoDto;
import com.soodfolio.api.dto.PortfolioResponseDto.ProjectDto;
import com.soodfolio.api.entity.Portfolio;
import com.soodfolio.api.entity.SocialLink;
import com.soodfolio.api.entity.User;
import com.soodfolio.api.exception.GlobalExceptionHandler;
import com.soodfolio.api.repository.PortfolioRepository;
import com.soodfolio.api.repository.SocialLinkRepository;
import com.soodfolio.api.security.UserPrincipal;
import com.soodfolio.api.service.GitHubService;
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
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GitHubProjectControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GitHubService gitHubService;

    @Mock
    private ProjectService projectService;

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private SocialLinkRepository socialLinkRepository;

    @Mock
    private I18nService i18nService;

    @InjectMocks
    private GitHubProjectController gitHubProjectController;

    private ObjectMapper objectMapper;
    private UserPrincipal mockUserPrincipal;
    private UUID userId;
    private Portfolio mockPortfolio;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        userId = UUID.randomUUID();
        mockUserPrincipal = new UserPrincipal(userId, "user@example.com", "password", "Alex Dev", List.of());

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return mockUserPrincipal;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(gitHubProjectController)
                .setControllerAdvice(new GlobalExceptionHandler(i18nService))
                .setCustomArgumentResolvers(authPrincipalResolver)
                .build();

        mockPortfolio = Portfolio.builder()
                .id(UUID.randomUUID())
                .user(User.builder().id(userId).build())
                .username("alexdev")
                .build();
    }

    @Test
    @DisplayName("GET /portfolios/me/github/repos with explicit username query param should return repos")
    void shouldReturnReposWithExplicitUsername() throws Exception {
        GitHubRepoDto repo = GitHubRepoDto.builder()
                .name("portfolio-kit")
                .fullName("octocat/portfolio-kit")
                .language("TypeScript")
                .stargazersCount(15)
                .build();

        when(gitHubService.getUserRepos("octocat")).thenReturn(List.of(repo));

        mockMvc.perform(get("/portfolios/me/github/repos")
                        .param("username", "octocat")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("portfolio-kit"))
                .andExpect(jsonPath("$[0].full_name").value("octocat/portfolio-kit"))
                .andExpect(jsonPath("$[0].language").value("TypeScript"))
                .andExpect(jsonPath("$[0].stargazers_count").value(15));

        verify(gitHubService).getUserRepos("octocat");
        verifyNoInteractions(socialLinkRepository);
    }

    @Test
    @DisplayName("GET /portfolios/me/github/repos without username should extract from SocialLink")
    void shouldExtractUsernameFromSocialLinkWhenOmitted() throws Exception {
        SocialLink link = SocialLink.builder()
                .platform("GITHUB")
                .url("https://github.com/sooddev")
                .build();

        GitHubRepoDto repo = GitHubRepoDto.builder()
                .name("sood-engine")
                .fullName("sooddev/sood-engine")
                .language("Java")
                .build();

        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(mockPortfolio));
        when(socialLinkRepository.findFirstByPortfolioIdAndPlatformIgnoreCase(mockPortfolio.getId(), "GITHUB"))
                .thenReturn(Optional.of(link));
        when(gitHubService.getUserRepos("sooddev")).thenReturn(List.of(repo));

        mockMvc.perform(get("/portfolios/me/github/repos")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("sood-engine"))
                .andExpect(jsonPath("$[0].full_name").value("sooddev/sood-engine"));

        verify(gitHubService).getUserRepos("sooddev");
    }

    @Test
    @DisplayName("POST /portfolios/me/github/import should import repository and return 201 Created")
    void shouldImportGitHubRepoSuccessfully() throws Exception {
        GitHubImportProjectRequest request = GitHubImportProjectRequest.builder()
                .repoName("my-awesome-tool")
                .githubUrl("https://github.com/alexdev/my-awesome-tool")
                .language("Go")
                .topics(List.of("cli", "devtools"))
                .description("A blazing fast CLI tool")
                .build();

        ProjectDto projectDto = ProjectDto.builder()
                .id(UUID.randomUUID())
                .title("My Awesome Tool")
                .subtitle("my-awesome-tool")
                .techStack(List.of("Go", "cli", "devtools"))
                .build();

        when(projectService.importFromGitHub(eq(userId), any(GitHubImportProjectRequest.class)))
                .thenReturn(projectDto);

        mockMvc.perform(post("/portfolios/me/github/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("My Awesome Tool"))
                .andExpect(jsonPath("$.subtitle").value("my-awesome-tool"))
                .andExpect(jsonPath("$.techStack[0]").value("Go"));

        verify(projectService).importFromGitHub(eq(userId), any(GitHubImportProjectRequest.class));
    }

    @Test
    @DisplayName("GET /portfolios/me/github/repos when GitHub returns empty array should return 200 OK and empty array []")
    void shouldReturnEmptyListWhenGitHubReturnsNoRepos() throws Exception {
        when(gitHubService.getUserRepos("emptyuser")).thenReturn(List.of());

        mockMvc.perform(get("/portfolios/me/github/repos")
                        .param("username", "emptyuser")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(gitHubService).getUserRepos("emptyuser");
    }

    @Test
    @DisplayName("POST /portfolios/me/github/import with complex kebab-case repo name should return 201 Created and formatted title")
    void shouldImportComplexKebabCaseRepoSuccessfully() throws Exception {
        GitHubImportProjectRequest request = GitHubImportProjectRequest.builder()
                .repoName("kafka-stream-event-pipeline")
                .githubUrl("https://github.com/alexdev/kafka-stream-event-pipeline")
                .language("Kotlin")
                .topics(List.of("kafka", "event-driven", "docker"))
                .description("Real-time distributed streaming pipeline")
                .build();

        ProjectDto projectDto = ProjectDto.builder()
                .id(UUID.randomUUID())
                .title("Kafka Stream Event Pipeline")
                .subtitle("kafka-stream-event-pipeline")
                .role("Lead Developer / Creator")
                .periodText("Present")
                .techStack(List.of("Kotlin", "kafka", "event-driven", "docker"))
                .orderIndex(3)
                .build();

        when(projectService.importFromGitHub(eq(userId), any(GitHubImportProjectRequest.class)))
                .thenReturn(projectDto);

        mockMvc.perform(post("/portfolios/me/github/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Kafka Stream Event Pipeline"))
                .andExpect(jsonPath("$.orderIndex").value(3))
                .andExpect(jsonPath("$.techStack[0]").value("Kotlin"));

        verify(projectService).importFromGitHub(eq(userId), any(GitHubImportProjectRequest.class));
    }

    @Test
    @DisplayName("GET /portfolios/me/github/repos without username and no GitHub SocialLink should return 400 Bad Request")
    void shouldReturnBadRequestWhenNoUsernameAndNoGitHubSocialLink() throws Exception {
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(mockPortfolio));
        when(socialLinkRepository.findFirstByPortfolioIdAndPlatformIgnoreCase(mockPortfolio.getId(), "GITHUB"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/portfolios/me/github/repos")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
