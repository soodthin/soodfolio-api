package com.soodfolio.api.service;

import com.soodfolio.api.dto.ExperienceDtos.ExperienceRequestDto;
import com.soodfolio.api.dto.PortfolioResponseDto.ExperienceDto;

import java.util.List;
import java.util.UUID;

public interface ExperienceService {
    List<ExperienceDto> getExperiencesByUserId(UUID userId);
    ExperienceDto getExperienceById(UUID userId, UUID experienceId);
    ExperienceDto createExperience(UUID userId, ExperienceRequestDto request);
    ExperienceDto updateExperience(UUID userId, UUID experienceId, ExperienceRequestDto request);
    void deleteExperience(UUID userId, UUID experienceId);
    List<ExperienceDto> reorderExperiences(UUID userId, List<UUID> experienceIds);
}
