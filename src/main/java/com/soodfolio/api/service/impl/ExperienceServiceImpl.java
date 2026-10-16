package com.soodfolio.api.service.impl;

import com.soodfolio.api.dto.ExperienceDtos.ExperienceRequestDto;
import com.soodfolio.api.dto.PortfolioResponseDto.ExperienceDto;
import com.soodfolio.api.entity.Experience;
import com.soodfolio.api.entity.Portfolio;
import com.soodfolio.api.exception.ResourceNotFoundException;
import com.soodfolio.api.repository.ExperienceRepository;
import com.soodfolio.api.repository.PortfolioRepository;
import com.soodfolio.api.service.ExperienceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExperienceServiceImpl implements ExperienceService {

    private final PortfolioRepository portfolioRepository;
    private final ExperienceRepository experienceRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ExperienceDto> getExperiencesByUserId(UUID userId) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "userId", userId));

        return experienceRepository.findByPortfolioIdOrderByOrderIndexAsc(portfolio.getId())
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ExperienceDto getExperienceById(UUID userId, UUID experienceId) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "userId", userId));

        Experience experience = experienceRepository.findByIdAndPortfolioId(experienceId, portfolio.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Experience", "id", experienceId));

        return mapToDto(experience);
    }

    @Override
    @Transactional
    public ExperienceDto createExperience(UUID userId, ExperienceRequestDto request) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "userId", userId));

        int currentCount = experienceRepository.countByPortfolioId(portfolio.getId());

        Experience experience = Experience.builder()
                .portfolio(portfolio)
                .roleTitle(request.getRoleTitle())
                .companyName(request.getCompanyName())
                .companyUrl(request.getCompanyUrl())
                .location(request.getLocation())
                .periodText(request.getPeriodText())
                .description(request.getDescription())
                .highlights(request.getHighlights() != null ? new ArrayList<>(request.getHighlights()) : new ArrayList<>())
                .skillsUsed(request.getSkillsUsed() != null ? new ArrayList<>(request.getSkillsUsed()) : new ArrayList<>())
                .orderIndex(currentCount)
                .build();

        Experience saved = experienceRepository.save(experience);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public ExperienceDto updateExperience(UUID userId, UUID experienceId, ExperienceRequestDto request) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "userId", userId));

        Experience experience = experienceRepository.findByIdAndPortfolioId(experienceId, portfolio.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Experience", "id", experienceId));

        experience.setRoleTitle(request.getRoleTitle());
        experience.setCompanyName(request.getCompanyName());
        experience.setCompanyUrl(request.getCompanyUrl());
        experience.setLocation(request.getLocation());
        experience.setPeriodText(request.getPeriodText());
        experience.setDescription(request.getDescription());
        if (request.getHighlights() != null) {
            experience.setHighlights(new ArrayList<>(request.getHighlights()));
        }
        if (request.getSkillsUsed() != null) {
            experience.setSkillsUsed(new ArrayList<>(request.getSkillsUsed()));
        }

        Experience saved = experienceRepository.save(experience);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public void deleteExperience(UUID userId, UUID experienceId) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "userId", userId));

        Experience experience = experienceRepository.findByIdAndPortfolioId(experienceId, portfolio.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Experience", "id", experienceId));

        experienceRepository.delete(experience);
    }

    @Override
    @Transactional
    public List<ExperienceDto> reorderExperiences(UUID userId, List<UUID> experienceIds) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "userId", userId));

        List<Experience> existing = experienceRepository.findByPortfolioIdOrderByOrderIndexAsc(portfolio.getId());
        Map<UUID, Experience> map = existing.stream().collect(Collectors.toMap(Experience::getId, e -> e));

        for (int i = 0; i < experienceIds.size(); i++) {
            Experience e = map.get(experienceIds.get(i));
            if (e != null) {
                e.setOrderIndex(i);
            }
        }
        experienceRepository.saveAll(existing);

        return experienceRepository.findByPortfolioIdOrderByOrderIndexAsc(portfolio.getId())
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    private ExperienceDto mapToDto(Experience e) {
        return ExperienceDto.builder()
                .id(e.getId())
                .roleTitle(e.getRoleTitle())
                .companyName(e.getCompanyName())
                .companyUrl(e.getCompanyUrl())
                .location(e.getLocation())
                .periodText(e.getPeriodText())
                .description(e.getDescription())
                .highlights(e.getHighlights() != null ? e.getHighlights() : List.of())
                .skillsUsed(e.getSkillsUsed() != null ? e.getSkillsUsed() : List.of())
                .orderIndex(e.getOrderIndex())
                .build();
    }
}
