package com.soodfolio.api.service;

import com.soodfolio.api.dto.EducationDtos.EducationRequestDto;
import com.soodfolio.api.dto.PortfolioResponseDto.EducationDto;

import java.util.List;
import java.util.UUID;

public interface EducationService {
    List<EducationDto> getEducationsByUserId(UUID userId);
    EducationDto getEducationById(UUID userId, UUID educationId);
    EducationDto createEducation(UUID userId, EducationRequestDto request);
    EducationDto updateEducation(UUID userId, UUID educationId, EducationRequestDto request);
    void deleteEducation(UUID userId, UUID educationId);
    List<EducationDto> reorderEducations(UUID userId, List<UUID> educationIds);
}
