package com.soodfolio.api.service;

import com.soodfolio.api.dto.GitHubRepoDto;
import com.soodfolio.api.exception.BadRequestException;
import com.soodfolio.api.exception.ResourceNotFoundException;
import com.soodfolio.api.service.impl.GitHubServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GitHubServiceTest {

    private MockRestServiceServer mockServer;
    private GitHubServiceImpl gitHubService;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        gitHubService = new GitHubServiceImpl(restClientBuilder);
    }

    @Test
    @DisplayName("Should successfully fetch repositories from GitHub API")
    void shouldFetchRepositoriesSuccessfully() {
        String jsonResponse = """
                [
                    {
                        "name": "soodfolio-api",
                        "full_name": "octocat/soodfolio-api",
                        "description": "Core Backend API",
                        "html_url": "https://github.com/octocat/soodfolio-api",
                        "homepage": "https://soodfolio.dev",
                        "language": "Java",
                        "topics": ["spring-boot", "rest-api"],
                        "stargazers_count": 25,
                        "forks_count": 5,
                        "updated_at": "2026-03-15T12:00:00Z"
                    }
                ]
                """;

        mockServer.expect(requestTo("https://api.github.com/users/octocat/repos?sort=updated&per_page=30"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("User-Agent", "SoodFolio-App"))
                .andExpect(header("Accept", "application/vnd.github.v3+json"))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        List<GitHubRepoDto> repos = gitHubService.getUserRepos("octocat");

        mockServer.verify();
        assertThat(repos).hasSize(1);
        GitHubRepoDto repo = repos.get(0);
        assertThat(repo.getName()).isEqualTo("soodfolio-api");
        assertThat(repo.getFullName()).isEqualTo("octocat/soodfolio-api");
        assertThat(repo.getDescription()).isEqualTo("Core Backend API");
        assertThat(repo.getLanguage()).isEqualTo("Java");
        assertThat(repo.getTopics()).containsExactly("spring-boot", "rest-api");
        assertThat(repo.getStargazersCount()).isEqualTo(25);
        assertThat(repo.getForksCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when GitHub username does not exist (404)")
    void shouldThrowResourceNotFoundWhenGitHubUserNotFound() {
        mockServer.expect(requestTo("https://api.github.com/users/nonexistent-user/repos?sort=updated&per_page=30"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> gitHubService.getUserRepos("nonexistent-user"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Tài khoản GitHub 'nonexistent-user' không tồn tại.");

        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw BadRequestException when GitHub API rate limit is exceeded (403)")
    void shouldThrowBadRequestWhenRateLimitExceeded() {
        mockServer.expect(requestTo("https://api.github.com/users/octocat/repos?sort=updated&per_page=30"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> gitHubService.getUserRepos("octocat"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Giới hạn tốc độ gọi GitHub API đã đạt mức tối đa");

        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw BadRequestException when username is blank")
    void shouldThrowBadRequestWhenUsernameIsBlank() {
        assertThatThrownBy(() -> gitHubService.getUserRepos("   "))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Tên người dùng GitHub không được để trống.");
    }

    @Test
    @DisplayName("Should throw BadRequestException when username is null")
    void shouldThrowBadRequestWhenUsernameIsNull() {
        assertThatThrownBy(() -> gitHubService.getUserRepos(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Tên người dùng GitHub không được để trống.");
    }

    @Test
    @DisplayName("Should return empty list safely without NPE when GitHub API returns empty array []")
    void shouldReturnEmptyListWhenGitHubApiReturnsEmptyArray() {
        mockServer.expect(requestTo("https://api.github.com/users/emptyuser/repos?sort=updated&per_page=30"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("User-Agent", "SoodFolio-App"))
                .andExpect(header("Accept", "application/vnd.github.v3+json"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        List<GitHubRepoDto> repos = gitHubService.getUserRepos("emptyuser");

        mockServer.verify();
        assertThat(repos).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should throw BadRequestException when GitHub API responds with 500 Internal Server Error")
    void shouldThrowBadRequestWhenGitHubApiReturnsServerError() {
        mockServer.expect(requestTo("https://api.github.com/users/octocat/repos?sort=updated&per_page=30"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> gitHubService.getUserRepos("octocat"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Không thể kết nối tới máy chủ GitHub API");

        mockServer.verify();
    }
}
