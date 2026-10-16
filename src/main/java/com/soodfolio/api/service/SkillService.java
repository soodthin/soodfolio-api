package com.soodfolio.api.service;

import com.soodfolio.api.dto.PortfolioResponseDto.SkillDto;
import com.soodfolio.api.dto.SkillDtos.SkillRequestDto;

import java.util.List;
import java.util.UUID;

public interface SkillService {
    List<SkillDto> getSkillsByUserId(UUID userId);
    SkillDto getSkillById(UUID userId, UUID skillId);
    SkillDto createSkill(UUID userId, SkillRequestDto request);
    SkillDto updateSkill(UUID userId, UUID skillId, SkillRequestDto request);
    void deleteSkill(UUID userId, UUID skillId);
    List<SkillDto> reorderSkills(UUID userId, List<UUID> skillIds);
}
