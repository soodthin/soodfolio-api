package com.soodfolio.api.service;

import com.soodfolio.api.dto.GitHubRepoDto;

import java.util.List;

public interface GitHubService {

    /**
     * Fetch public repositories of a given GitHub username.
     *
     * @param username GitHub username
     * @return List of public repositories ordered by last updated
     */
    List<GitHubRepoDto> getUserRepos(String username);
}
