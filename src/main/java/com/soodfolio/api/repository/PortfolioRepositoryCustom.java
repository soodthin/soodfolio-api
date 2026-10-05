package com.soodfolio.api.repository;

import com.soodfolio.api.entity.Portfolio;

import java.util.Optional;

public interface PortfolioRepositoryCustom {
    Optional<Portfolio> findActivePortfolioWithFullDetails(String username);
}
