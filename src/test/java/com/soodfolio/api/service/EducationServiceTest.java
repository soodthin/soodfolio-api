package com.soodfolio.api.service;

import com.soodfolio.api.dto.EducationDtos.EducationRequestDto;
import com.soodfolio.api.dto.PortfolioResponseDto.EducationDto;
import com.soodfolio.api.entity.Education;
import com.soodfolio.api.entity.Portfolio;
import com.soodfolio.api.entity.User;
import com.soodfolio.api.exception.ResourceNotFoundException;
import com.soodfolio.api.repository.EducationRepository;
import com.soodfolio.api.repository.PortfolioRepository;
import com.soodfolio.api.service.impl.EducationServiceImpl;
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
class EducationServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private EducationRepository educationRepository;

    @InjectMocks
    private EducationServiceImpl educationService;

    private UUID userId;
    private Portfolio portfolio;
    private Education education;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        portfolio = Portfolio.builder()
                .id(UUID.randomUUID())
                .user(User.builder().id(userId).build())
                .username("soodthin")
                .build();

        education = Education.builder()
                .id(UUID.randomUUID())
                .portfolio(portfolio)
                .institution("Ho Chi Minh City Open University")
                .degree("Bachelor of Science")
                .fieldOfStudy("Computer Science")
                .periodText("2022 — Present")
                .highlights("GPA 3.6/4.0")
                .orderIndex(0)
                .build();
    }

    @Test
    @DisplayName("Should successfully create a new education entry with auto-incremented orderIndex")
    void shouldCreateEducationSuccessfully() {
        EducationRequestDto request = EducationRequestDto.builder()
                .institution("Stanford University")
                .degree("Master of Science")
                .fieldOfStudy("Software Systems")
                .periodText("2026 — 2028")
                .highlights("Specialization in Distributed Computing")
                .build();

        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(educationRepository.countByPortfolioId(portfolio.getId())).thenReturn(1);
        when(educationRepository.save(any(Education.class))).thenAnswer(invocation -> {
            Education e = invocation.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        EducationDto result = educationService.createEducation(userId, request);

        assertThat(result).isNotNull();
        assertThat(result.getInstitution()).isEqualTo("Stanford University");
        assertThat(result.getDegree()).isEqualTo("Master of Science");
        assertThat(result.getOrderIndex()).isEqualTo(1);

        verify(educationRepository).save(any(Education.class));
    }

    @Test
    @DisplayName("Should successfully update an existing education entry")
    void shouldUpdateEducationSuccessfully() {
        UUID eduId = education.getId();
        EducationRequestDto request = EducationRequestDto.builder()
                .institution("HCMCOU - Updated")
                .degree("Bachelor of Science (Honors)")
                .fieldOfStudy("Computer Science")
                .periodText("2022 — 2026")
                .highlights("Graduated with High Distinction")
                .build();

        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(educationRepository.findByIdAndPortfolioId(eduId, portfolio.getId())).thenReturn(Optional.of(education));
        when(educationRepository.save(any(Education.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EducationDto updated = educationService.updateEducation(userId, eduId, request);

        assertThat(updated).isNotNull();
        assertThat(updated.getInstitution()).isEqualTo("HCMCOU - Updated");
        assertThat(updated.getDegree()).isEqualTo("Bachelor of Science (Honors)");

        verify(educationRepository).save(education);
    }

    @Test
    @DisplayName("Should successfully delete an education entry")
    void shouldDeleteEducationSuccessfully() {
        UUID eduId = education.getId();
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(educationRepository.findByIdAndPortfolioId(eduId, portfolio.getId())).thenReturn(Optional.of(education));

        educationService.deleteEducation(userId, eduId);

        verify(educationRepository).delete(education);
    }

    @Test
    @DisplayName("Should reorder education entries correctly")
    void shouldReorderEducationsSuccessfully() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Education edu1 = Education.builder().id(id1).portfolio(portfolio).orderIndex(0).build();
        Education edu2 = Education.builder().id(id2).portfolio(portfolio).orderIndex(1).build();

        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(educationRepository.findByPortfolioIdOrderByOrderIndexAsc(portfolio.getId()))
                .thenReturn(new ArrayList<>(List.of(edu1, edu2)));

        List<EducationDto> reordered = educationService.reorderEducations(userId, List.of(id2, id1));

        assertThat(reordered).hasSize(2);
        assertThat(edu2.getOrderIndex()).isEqualTo(0);
        assertThat(edu1.getOrderIndex()).isEqualTo(1);
        verify(educationRepository).saveAll(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when education does not exist")
    void shouldThrowNotFoundWhenEducationDoesNotExist() {
        UUID unknownId = UUID.randomUUID();
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(educationRepository.findByIdAndPortfolioId(unknownId, portfolio.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> educationService.getEducationById(userId, unknownId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Education not found with id");
    }
}
