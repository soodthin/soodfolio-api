package com.soodfolio.api.service.impl;

import com.soodfolio.api.dto.EducationDtos.EducationRequestDto;
import com.soodfolio.api.dto.PortfolioResponseDto.EducationDto;
import com.soodfolio.api.entity.Education;
import com.soodfolio.api.entity.Portfolio;
import com.soodfolio.api.exception.ResourceNotFoundException;
import com.soodfolio.api.repository.EducationRepository;
import com.soodfolio.api.repository.PortfolioRepository;
import com.soodfolio.api.service.EducationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EducationServiceImpl implements EducationService {

    private final PortfolioRepository portfolioRepository;
    private final EducationRepository educationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<EducationDto> getEducationsByUserId(UUID userId) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "userId", userId));

        return educationRepository.findByPortfolioIdOrderByOrderIndexAsc(portfolio.getId())
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EducationDto getEducationById(UUID userId, UUID educationId) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "userId", userId));

        Education education = educationRepository.findByIdAndPortfolioId(educationId, portfolio.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Education", "id", educationId));

        return mapToDto(education);
    }

    @Override
    @Transactional
    public EducationDto createEducation(UUID userId, EducationRequestDto request) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "userId", userId));

        int currentCount = educationRepository.countByPortfolioId(portfolio.getId());

        Education education = Education.builder()
                .portfolio(portfolio)
                .institution(request.getInstitution())
                .degree(request.getDegree())
                .fieldOfStudy(request.getFieldOfStudy())
                .periodText(request.getPeriodText())
                .highlights(request.getHighlights())
                .orderIndex(currentCount)
                .build();

        Education saved = educationRepository.save(education);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public EducationDto updateEducation(UUID userId, UUID educationId, EducationRequestDto request) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "userId", userId));

        Education education = educationRepository.findByIdAndPortfolioId(educationId, portfolio.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Education", "id", educationId));

        education.setInstitution(request.getInstitution());
        education.setDegree(request.getDegree());
        education.setFieldOfStudy(request.getFieldOfStudy());
        education.setPeriodText(request.getPeriodText());
        education.setHighlights(request.getHighlights());

        Education saved = educationRepository.save(education);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public void deleteEducation(UUID userId, UUID educationId) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "userId", userId));

        Education education = educationRepository.findByIdAndPortfolioId(educationId, portfolio.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Education", "id", educationId));

        educationRepository.delete(education);
    }

    @Override
    @Transactional
    public List<EducationDto> reorderEducations(UUID userId, List<UUID> educationIds) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "userId", userId));

        List<Education> existing = educationRepository.findByPortfolioIdOrderByOrderIndexAsc(portfolio.getId());
        Map<UUID, Education> map = existing.stream().collect(Collectors.toMap(Education::getId, e -> e));

        for (int i = 0; i < educationIds.size(); i++) {
            Education e = map.get(educationIds.get(i));
            if (e != null) {
                e.setOrderIndex(i);
            }
        }
        educationRepository.saveAll(existing);

        return educationRepository.findByPortfolioIdOrderByOrderIndexAsc(portfolio.getId())
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    private EducationDto mapToDto(Education e) {
        return EducationDto.builder()
                .id(e.getId())
                .institution(e.getInstitution())
                .degree(e.getDegree())
                .fieldOfStudy(e.getFieldOfStudy())
                .periodText(e.getPeriodText())
                .highlights(e.getHighlights())
                .orderIndex(e.getOrderIndex())
                .build();
    }
}
