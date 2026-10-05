package com.soodfolio.api.service;

import com.soodfolio.api.dto.PortfolioResponseDto;

import java.util.Optional;

public interface PortfolioService {
    Optional<PortfolioResponseDto> getPortfolioByUsername(String username);
}
