package com.soodfolio.api.service;

import com.soodfolio.api.dto.ExperienceDtos.ExperienceRequestDto;
import com.soodfolio.api.dto.PortfolioResponseDto.ExperienceDto;
import com.soodfolio.api.entity.Experience;
import com.soodfolio.api.entity.Portfolio;
import com.soodfolio.api.entity.User;
import com.soodfolio.api.exception.ResourceNotFoundException;
import com.soodfolio.api.repository.ExperienceRepository;
import com.soodfolio.api.repository.PortfolioRepository;
import com.soodfolio.api.service.impl.ExperienceServiceImpl;
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
class ExperienceServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private ExperienceRepository experienceRepository;

    @InjectMocks
    private ExperienceServiceImpl experienceService;

    private UUID userId;
    private Portfolio portfolio;
    private Experience experience;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        portfolio = Portfolio.builder()
                .id(UUID.randomUUID())
                .user(User.builder().id(userId).build())
                .username("alex")
                .build();

        experience = Experience.builder()
                .id(UUID.randomUUID())
                .portfolio(portfolio)
                .roleTitle("Software Engineer")
                .companyName("TechCorp")
                .companyUrl("https://techcorp.com")
                .location("HCMC")
                .periodText("2024 - 2026")
                .description("Building backend APIs")
                .highlights(List.of("Reduced latency by 30%"))
                .skillsUsed(List.of("Java", "Spring Boot"))
                .orderIndex(0)
                .build();
    }

    @Test
    @DisplayName("Should successfully create a new experience and assign incremental orderIndex")
    void shouldCreateExperienceSuccessfully() {
        ExperienceRequestDto request = ExperienceRequestDto.builder()
                .roleTitle("Senior Architect")
                .companyName("Global Systems")
                .location("Remote")
                .periodText("2026 - Present")
                .description("Leading distributed system designs")
                .highlights(List.of("Mentored engineers"))
                .skillsUsed(List.of("Kafka", "Docker"))
                .build();

        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(experienceRepository.countByPortfolioId(portfolio.getId())).thenReturn(1);
        when(experienceRepository.save(any(Experience.class))).thenAnswer(invocation -> {
            Experience exp = invocation.getArgument(0);
            exp.setId(UUID.randomUUID());
            return exp;
        });

        ExperienceDto result = experienceService.createExperience(userId, request);

        assertThat(result).isNotNull();
        assertThat(result.getRoleTitle()).isEqualTo("Senior Architect");
        assertThat(result.getCompanyName()).isEqualTo("Global Systems");
        assertThat(result.getOrderIndex()).isEqualTo(1);
        assertThat(result.getSkillsUsed()).contains("Kafka", "Docker");

        verify(experienceRepository).save(any(Experience.class));
    }

    @Test
    @DisplayName("Should successfully update existing experience")
    void shouldUpdateExperienceSuccessfully() {
        UUID expId = experience.getId();
        ExperienceRequestDto request = ExperienceRequestDto.builder()
                .roleTitle("Lead Engineer")
                .companyName("TechCorp Updated")
                .periodText("2024 - 2026")
                .build();

        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(experienceRepository.findByIdAndPortfolioId(expId, portfolio.getId())).thenReturn(Optional.of(experience));
        when(experienceRepository.save(any(Experience.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExperienceDto updated = experienceService.updateExperience(userId, expId, request);

        assertThat(updated).isNotNull();
        assertThat(updated.getRoleTitle()).isEqualTo("Lead Engineer");
        assertThat(updated.getCompanyName()).isEqualTo("TechCorp Updated");

        verify(experienceRepository).save(experience);
    }

    @Test
    @DisplayName("Should successfully delete experience")
    void shouldDeleteExperienceSuccessfully() {
        UUID expId = experience.getId();
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(experienceRepository.findByIdAndPortfolioId(expId, portfolio.getId())).thenReturn(Optional.of(experience));

        experienceService.deleteExperience(userId, expId);

        verify(experienceRepository).delete(experience);
    }

    @Test
    @DisplayName("Should reorder experiences according to input IDs")
    void shouldReorderExperiencesSuccessfully() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Experience exp1 = Experience.builder().id(id1).portfolio(portfolio).orderIndex(0).build();
        Experience exp2 = Experience.builder().id(id2).portfolio(portfolio).orderIndex(1).build();

        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(experienceRepository.findByPortfolioIdOrderByOrderIndexAsc(portfolio.getId()))
                .thenReturn(new ArrayList<>(List.of(exp1, exp2)));

        List<ExperienceDto> reordered = experienceService.reorderExperiences(userId, List.of(id2, id1));

        assertThat(reordered).hasSize(2);
        assertThat(exp2.getOrderIndex()).isEqualTo(0);
        assertThat(exp1.getOrderIndex()).isEqualTo(1);
        verify(experienceRepository).saveAll(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when experience not found for user")
    void shouldThrowNotFoundWhenExperienceDoesNotExist() {
        UUID randomId = UUID.randomUUID();
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(experienceRepository.findByIdAndPortfolioId(randomId, portfolio.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> experienceService.getExperienceById(userId, randomId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Experience not found with id");
    }
}
