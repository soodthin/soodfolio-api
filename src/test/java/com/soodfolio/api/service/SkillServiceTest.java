package com.soodfolio.api.service;

import com.soodfolio.api.dto.PortfolioResponseDto.SkillDto;
import com.soodfolio.api.dto.SkillDtos.SkillRequestDto;
import com.soodfolio.api.entity.Portfolio;
import com.soodfolio.api.entity.Skill;
import com.soodfolio.api.entity.User;
import com.soodfolio.api.exception.ResourceNotFoundException;
import com.soodfolio.api.repository.PortfolioRepository;
import com.soodfolio.api.repository.SkillRepository;
import com.soodfolio.api.service.impl.SkillServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private SkillRepository skillRepository;

    @InjectMocks
    private SkillServiceImpl skillService;

    private UUID userId;
    private Portfolio portfolio;
    private Skill skill;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        portfolio = Portfolio.builder()
                .id(UUID.randomUUID())
                .user(User.builder().id(userId).build())
                .username("alex")
                .build();

        skill = Skill.builder()
                .id(UUID.randomUUID())
                .portfolio(portfolio)
                .category("BACKEND")
                .name("Java 21")
                .orderIndex(0)
                .build();
    }

    @Test
    @DisplayName("Should successfully create a new skill with auto orderIndex")
    void shouldCreateSkillSuccessfully() {
        SkillRequestDto request = SkillRequestDto.builder()
                .category("DATABASE")
                .name("PostgreSQL")
                .build();

        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(skillRepository.countByPortfolioId(portfolio.getId())).thenReturn(2);
        when(skillRepository.save(any(Skill.class))).thenAnswer(invocation -> {
            Skill s = invocation.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        SkillDto result = skillService.createSkill(userId, request);

        assertThat(result).isNotNull();
        assertThat(result.getCategory()).isEqualTo("DATABASE");
        assertThat(result.getName()).isEqualTo("PostgreSQL");
        assertThat(result.getOrderIndex()).isEqualTo(2);

        verify(skillRepository).save(any(Skill.class));
    }

    @Test
    @DisplayName("Should successfully update existing skill")
    void shouldUpdateSkillSuccessfully() {
        UUID skillId = skill.getId();
        SkillRequestDto request = SkillRequestDto.builder()
                .category("CORE")
                .name("Java 21 LTS")
                .build();

        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(skillRepository.findByIdAndPortfolioId(skillId, portfolio.getId())).thenReturn(Optional.of(skill));
        when(skillRepository.save(any(Skill.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SkillDto updated = skillService.updateSkill(userId, skillId, request);

        assertThat(updated).isNotNull();
        assertThat(updated.getCategory()).isEqualTo("CORE");
        assertThat(updated.getName()).isEqualTo("Java 21 LTS");

        verify(skillRepository).save(skill);
    }

    @Test
    @DisplayName("Should successfully delete skill")
    void shouldDeleteSkillSuccessfully() {
        UUID skillId = skill.getId();
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(skillRepository.findByIdAndPortfolioId(skillId, portfolio.getId())).thenReturn(Optional.of(skill));

        skillService.deleteSkill(userId, skillId);

        verify(skillRepository).delete(skill);
    }

    @Test
    @DisplayName("Should reorder skills correctly")
    void shouldReorderSkillsSuccessfully() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Skill s1 = Skill.builder().id(id1).portfolio(portfolio).orderIndex(0).build();
        Skill s2 = Skill.builder().id(id2).portfolio(portfolio).orderIndex(1).build();

        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(skillRepository.findByPortfolioIdOrderByOrderIndexAsc(portfolio.getId()))
                .thenReturn(new ArrayList<>(List.of(s1, s2)));

        List<SkillDto> reordered = skillService.reorderSkills(userId, List.of(id2, id1));

        assertThat(reordered).hasSize(2);
        assertThat(s2.getOrderIndex()).isEqualTo(0);
        assertThat(s1.getOrderIndex()).isEqualTo(1);
        verify(skillRepository).saveAll(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when skill not found")
    void shouldThrowNotFoundWhenSkillDoesNotExist() {
        UUID randomId = UUID.randomUUID();
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(skillRepository.findByIdAndPortfolioId(randomId, portfolio.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> skillService.getSkillById(userId, randomId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Skill not found with id");
    }
}
