package com.soodfolio.api.service;

import com.soodfolio.api.dto.AnalyticsDtos.RecordViewRequestDto;
import com.soodfolio.api.dto.AnalyticsDtos.ViewCountResponseDto;
import com.soodfolio.api.dto.PortfolioResponseDto;
import com.soodfolio.api.dto.UpdatePortfolioRequestDto;

import java.util.Optional;
import java.util.UUID;

public interface PortfolioService {
    Optional<PortfolioResponseDto> getPortfolioByUsername(String username);
    Optional<PortfolioResponseDto> getPortfolioByUserId(UUID userId);
    PortfolioResponseDto updatePublishedStatus(UUID userId, boolean isPublished);
    PortfolioResponseDto updatePortfolio(UUID userId, UpdatePortfolioRequestDto request);
    ViewCountResponseDto recordView(String username, RecordViewRequestDto request, String userAgent);
}
