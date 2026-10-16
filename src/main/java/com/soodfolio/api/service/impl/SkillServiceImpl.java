package com.soodfolio.api.service.impl;

import com.soodfolio.api.dto.PortfolioResponseDto.SkillDto;
import com.soodfolio.api.dto.SkillDtos.SkillRequestDto;
import com.soodfolio.api.entity.Portfolio;
import com.soodfolio.api.entity.Skill;
import com.soodfolio.api.exception.ResourceNotFoundException;
import com.soodfolio.api.repository.PortfolioRepository;
import com.soodfolio.api.repository.SkillRepository;
import com.soodfolio.api.service.SkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SkillServiceImpl implements SkillService {

    private final PortfolioRepository portfolioRepository;
    private final SkillRepository skillRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SkillDto> getSkillsByUserId(UUID userId) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "userId", userId));

        return skillRepository.findByPortfolioIdOrderByOrderIndexAsc(portfolio.getId())
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SkillDto getSkillById(UUID userId, UUID skillId) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "userId", userId));

        Skill skill = skillRepository.findByIdAndPortfolioId(skillId, portfolio.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Skill", "id", skillId));

        return mapToDto(skill);
    }

    @Override
    @Transactional
    public SkillDto createSkill(UUID userId, SkillRequestDto request) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "userId", userId));

        int currentCount = skillRepository.countByPortfolioId(portfolio.getId());

        Skill skill = Skill.builder()
                .portfolio(portfolio)
                .category(request.getCategory())
                .name(request.getName())
                .orderIndex(currentCount)
                .build();

        Skill saved = skillRepository.save(skill);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public SkillDto updateSkill(UUID userId, UUID skillId, SkillRequestDto request) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "userId", userId));

        Skill skill = skillRepository.findByIdAndPortfolioId(skillId, portfolio.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Skill", "id", skillId));

        skill.setCategory(request.getCategory());
        skill.setName(request.getName());

        Skill saved = skillRepository.save(skill);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public void deleteSkill(UUID userId, UUID skillId) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "userId", userId));

        Skill skill = skillRepository.findByIdAndPortfolioId(skillId, portfolio.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Skill", "id", skillId));

        skillRepository.delete(skill);
    }

    @Override
    @Transactional
    public List<SkillDto> reorderSkills(UUID userId, List<UUID> skillIds) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "userId", userId));

        List<Skill> existing = skillRepository.findByPortfolioIdOrderByOrderIndexAsc(portfolio.getId());
        Map<UUID, Skill> map = existing.stream().collect(Collectors.toMap(Skill::getId, s -> s));

        for (int i = 0; i < skillIds.size(); i++) {
            Skill s = map.get(skillIds.get(i));
            if (s != null) {
                s.setOrderIndex(i);
            }
        }
        skillRepository.saveAll(existing);

        return skillRepository.findByPortfolioIdOrderByOrderIndexAsc(portfolio.getId())
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    private SkillDto mapToDto(Skill s) {
        return SkillDto.builder()
                .id(s.getId())
                .category(s.getCategory())
                .name(s.getName())
                .orderIndex(s.getOrderIndex())
                .build();
    }
}
