package com.soodfolio.api.service.impl;

import com.soodfolio.api.dto.GitHubRepoDto;
import com.soodfolio.api.exception.BadRequestException;
import com.soodfolio.api.exception.ResourceNotFoundException;
import com.soodfolio.api.service.GitHubService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.core.ParameterizedTypeReference;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class GitHubServiceImpl implements GitHubService {

    private static final String GITHUB_API_BASE_URL = "https://api.github.com";
    private static final String USER_AGENT_HEADER = "SoodFolio-App";
    private static final String GITHUB_ACCEPT_HEADER = "application/vnd.github.v3+json";

    private final RestClient restClient;

    @Autowired
    public GitHubServiceImpl(RestClient.Builder restClientBuilder) {

        this.restClient = restClientBuilder
                .baseUrl(GITHUB_API_BASE_URL)
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT_HEADER)
                .defaultHeader(HttpHeaders.ACCEPT, GITHUB_ACCEPT_HEADER)
                .build();
    }


    // Secondary constructor for custom RestClient injection (e.g., in unit tests)
    public GitHubServiceImpl(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<GitHubRepoDto> getUserRepos(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new BadRequestException("Tên người dùng GitHub không được để trống.");
        }

        String sanitizedUsername = username.trim();
        log.info("Fetching public repositories from GitHub for user: {}", sanitizedUsername);

        try {
            List<GitHubRepoDto> repos = restClient.get()
                    .uri("/users/{username}/repos?sort=updated&per_page=30", sanitizedUsername)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<GitHubRepoDto>>() {});

            return repos != null ? repos : Collections.emptyList();
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("GitHub user '{}' not found: {}", sanitizedUsername, e.getMessage());
            throw new ResourceNotFoundException("Tài khoản GitHub '" + sanitizedUsername + "' không tồn tại.");
        } catch (HttpClientErrorException.Forbidden e) {
            log.warn("GitHub API rate limit exceeded or access forbidden for user '{}': {}", sanitizedUsername, e.getMessage());
            throw new BadRequestException("Giới hạn tốc độ gọi GitHub API đã đạt mức tối đa hoặc quyền truy cập bị từ chối. Vui lòng thử lại sau.");
        } catch (HttpClientErrorException e) {
            log.warn("GitHub API client error ({}) for user '{}': {}", e.getStatusCode(), sanitizedUsername, e.getMessage());
            throw new BadRequestException("Lỗi truy vấn GitHub API (" + e.getStatusCode().value() + "): " + e.getStatusText());
        } catch (RestClientException e) {
            log.error("Failed to connect to GitHub API for user '{}': {}", sanitizedUsername, e.getMessage());
            throw new BadRequestException("Không thể kết nối tới máy chủ GitHub API. Vui lòng kiểm tra lại kết nối mạng hoặc thử lại sau.");
        }
    }
}
