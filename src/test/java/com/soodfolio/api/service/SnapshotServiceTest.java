package com.soodfolio.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soodfolio.api.dto.PortfolioResponseDto;
import com.soodfolio.api.dto.SnapshotDtos.RollbackResponseDto;
import com.soodfolio.api.dto.SnapshotDtos.SnapshotResponseDto;
import com.soodfolio.api.dto.SnapshotDtos.SnapshotSummaryDto;
import com.soodfolio.api.entity.*;
import com.soodfolio.api.exception.BadRequestException;
import com.soodfolio.api.exception.ResourceNotFoundException;
import com.soodfolio.api.repository.PortfolioRepository;
import com.soodfolio.api.repository.PortfolioSnapshotRepository;
import com.soodfolio.api.service.impl.SnapshotServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SnapshotServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private PortfolioSnapshotRepository snapshotRepository;

    @Mock
    private PortfolioService portfolioService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SnapshotServiceImpl snapshotService;

    private UUID userId;
    private UUID portfolioId;
    private Portfolio portfolio;
    private PortfolioResponseDto samplePortfolioDto;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        portfolioId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .fullName("Alex Nguyen")
                .build();

        portfolio = Portfolio.builder()
                .id(portfolioId)
                .user(user)
                .username("alex")
                .title("Alex Nguyen")
                .tagline("Software Engineer")
                .bio("Original bio")
                .statusText("AVAILABLE")
                .themeMode("DARK")
                .isPublished(true)
                .socialLinks(new ArrayList<>())
                .experiences(new ArrayList<>())
                .projects(new ArrayList<>())
                .skills(new ArrayList<>())
                .educations(new ArrayList<>())
                .build();

        samplePortfolioDto = PortfolioResponseDto.builder()
                .id(portfolioId)
                .username("alex")
                .fullName("Alex Nguyen")
                .title("Alex Nguyen")
                .tagline("Software Engineer")
                .bio("Original bio")
                .statusText("AVAILABLE")
                .themeMode("DARK")
                .isPublished(true)
                .totalViews(100L)
                .showProjects(true)
                .showExperience(true)
                .showSkills(true)
                .showEducation(true)
                .showContact(true)
                .socialLinks(List.of(
                        PortfolioResponseDto.SocialLinkDto.builder()
                                .platform("GITHUB")
                                .url("https://github.com/alex")
                                .label("[ GitHub ]")
                                .build()
                ))
                .experiences(List.of(
                        PortfolioResponseDto.ExperienceDto.builder()
                                .id(UUID.randomUUID())
                                .roleTitle("Senior Backend Engineer")
                                .companyName("TechCorp")
                                .periodText("2023 - Present")
                                .description("Led cloud migration")
                                .highlights(List.of("Reduced latency by 40%"))
                                .skillsUsed(List.of("Java", "Spring Boot"))
                                .orderIndex(0)
                                .build()
                ))
                .projects(List.of(
                        PortfolioResponseDto.ProjectDto.builder()
                                .id(UUID.randomUUID())
                                .title("SoodFolio Platform")
                                .subtitle("Pitch deck engine")
                                .role("Lead Architect")
                                .periodText("2024")
                                .description("Next-gen portfolio system")
                                .highlights(List.of("Zero latency rendering"))
                                .techStack(List.of("Java", "Next.js"))
                                .orderIndex(0)
                                .build()
                ))
                .skills(List.of(
                        PortfolioResponseDto.SkillDto.builder()
                                .id(UUID.randomUUID())
                                .category("BACKEND")
                                .name("Spring Boot")
                                .orderIndex(0)
                                .build()
                ))
                .educations(List.of(
                        PortfolioResponseDto.EducationDto.builder()
                                .id(UUID.randomUUID())
                                .institution("University of Science")
                                .degree("Bachelor of CS")
                                .fieldOfStudy("Computer Science")
                                .periodText("2019 - 2023")
                                .highlights("GPA 3.8")
                                .orderIndex(0)
                                .build()
                ))
                .build();
    }

    // ==========================================
    // 1. publishSnapshot Tests
    // ==========================================

    @Test
    @DisplayName("Should successfully publish first snapshot with version 1 and serialized JSON data")
    void shouldPublishFirstSnapshotSuccessfully() throws JsonProcessingException {
        String jsonPayload = "{\"username\":\"alex\",\"title\":\"Alex Nguyen\"}";

        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(portfolioService.getPortfolioByUserId(userId)).thenReturn(Optional.of(samplePortfolioDto));
        when(snapshotRepository.findTopByPortfolioIdOrderByVersionDesc(portfolioId)).thenReturn(Optional.empty());
        when(objectMapper.writeValueAsString(samplePortfolioDto)).thenReturn(jsonPayload);

        when(snapshotRepository.save(any(PortfolioSnapshot.class))).thenAnswer(invocation -> {
            PortfolioSnapshot entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            entity.setCreatedAt(LocalDateTime.now());
            return entity;
        });

        SnapshotResponseDto response = snapshotService.publishSnapshot(userId, "Initial Release v1");

        assertThat(response).isNotNull();
        assertThat(response.getVersion()).isEqualTo(1);
        assertThat(response.getChangelog()).isEqualTo("Initial Release v1");
        assertThat(response.getPortfolioId()).isEqualTo(portfolioId);
        assertThat(response.getData()).isEqualTo(samplePortfolioDto);

        ArgumentCaptor<PortfolioSnapshot> captor = ArgumentCaptor.forClass(PortfolioSnapshot.class);
        verify(snapshotRepository).save(captor.capture());
        PortfolioSnapshot captured = captor.getValue();
        assertThat(captured.getVersion()).isEqualTo(1);
        assertThat(captured.getChangelog()).isEqualTo("Initial Release v1");
        assertThat(captured.getSnapshotData()).isEqualTo(jsonPayload);
        assertThat(captured.getPortfolio()).isEqualTo(portfolio);
    }

    @Test
    @DisplayName("Should increment version to N+1 and default changelog to 'Version N+1' when changelog is blank")
    void shouldIncrementVersionAndDefaultChangelogWhenBlank() throws JsonProcessingException {
        PortfolioSnapshot priorSnapshot = PortfolioSnapshot.builder()
                .version(2)
                .build();

        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(portfolioService.getPortfolioByUserId(userId)).thenReturn(Optional.of(samplePortfolioDto));
        when(snapshotRepository.findTopByPortfolioIdOrderByVersionDesc(portfolioId)).thenReturn(Optional.of(priorSnapshot));
        when(objectMapper.writeValueAsString(samplePortfolioDto)).thenReturn("{}");

        when(snapshotRepository.save(any(PortfolioSnapshot.class))).thenAnswer(invocation -> {
            PortfolioSnapshot entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            entity.setCreatedAt(LocalDateTime.now());
            return entity;
        });

        SnapshotResponseDto response = snapshotService.publishSnapshot(userId, "   ");

        assertThat(response.getVersion()).isEqualTo(3);
        assertThat(response.getChangelog()).isEqualTo("Version 3");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when publishing snapshot for non-existent user portfolio")
    void shouldThrowNotFoundWhenPublishingForNonExistentPortfolio() {
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> snapshotService.publishSnapshot(userId, "Changelog"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Portfolio not found with userId");

        verify(snapshotRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when current portfolio DTO is not found")
    void shouldThrowNotFoundWhenPortfolioDtoNotFound() {
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(portfolioService.getPortfolioByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> snapshotService.publishSnapshot(userId, "Changelog"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(snapshotRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BadRequestException when ObjectMapper fails to serialize portfolio DTO")
    void shouldThrowBadRequestExceptionWhenJsonSerializationFails() throws JsonProcessingException {
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(portfolioService.getPortfolioByUserId(userId)).thenReturn(Optional.of(samplePortfolioDto));
        when(snapshotRepository.findTopByPortfolioIdOrderByVersionDesc(portfolioId)).thenReturn(Optional.empty());
        when(objectMapper.writeValueAsString(samplePortfolioDto)).thenThrow(new JsonProcessingException("Serialization error") {});

        assertThatThrownBy(() -> snapshotService.publishSnapshot(userId, "Changelog"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Không thể tạo snapshot do lỗi dữ liệu.");

        verify(snapshotRepository, never()).save(any());
    }

    // ==========================================
    // 2. getSnapshots & getSnapshot Tests
    // ==========================================

    @Test
    @DisplayName("Should return list of snapshot summaries ordered by version descending")
    void shouldGetSnapshotsSuccessfully() {
        PortfolioSnapshot snap1 = PortfolioSnapshot.builder()
                .id(UUID.randomUUID())
                .version(1)
                .changelog("First version")
                .createdAt(LocalDateTime.now().minusDays(2))
                .build();

        PortfolioSnapshot snap2 = PortfolioSnapshot.builder()
                .id(UUID.randomUUID())
                .version(2)
                .changelog("Second version")
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(snapshotRepository.findByPortfolioIdOrderByVersionDesc(portfolioId)).thenReturn(List.of(snap2, snap1));

        List<SnapshotSummaryDto> summaries = snapshotService.getSnapshots(userId);

        assertThat(summaries).hasSize(2);
        assertThat(summaries.get(0).getVersion()).isEqualTo(2);
        assertThat(summaries.get(0).getChangelog()).isEqualTo("Second version");
        assertThat(summaries.get(1).getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when getting snapshot list for non-existent portfolio")
    void shouldThrowNotFoundWhenGettingSnapshotsForUnknownUser() {
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> snapshotService.getSnapshots(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should retrieve specific snapshot version and deserialize JSON payload")
    void shouldGetSpecificSnapshotSuccessfully() throws JsonProcessingException {
        String json = "{\"username\":\"alex\",\"title\":\"Alex Nguyen\"}";
        PortfolioSnapshot snapshot = PortfolioSnapshot.builder()
                .id(UUID.randomUUID())
                .portfolio(portfolio)
                .version(2)
                .snapshotData(json)
                .changelog("Targeted snapshot")
                .createdAt(LocalDateTime.now())
                .build();

        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(snapshotRepository.findByPortfolioIdAndVersion(portfolioId, 2)).thenReturn(Optional.of(snapshot));
        when(objectMapper.readValue(json, PortfolioResponseDto.class)).thenReturn(samplePortfolioDto);

        SnapshotResponseDto response = snapshotService.getSnapshot(userId, 2);

        assertThat(response).isNotNull();
        assertThat(response.getVersion()).isEqualTo(2);
        assertThat(response.getChangelog()).isEqualTo("Targeted snapshot");
        assertThat(response.getData()).isEqualTo(samplePortfolioDto);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when snapshot version does not exist")
    void shouldThrowNotFoundWhenSnapshotVersionDoesNotExist() {
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(snapshotRepository.findByPortfolioIdAndVersion(portfolioId, 99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> snapshotService.getSnapshot(userId, 99))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Snapshot not found with version: '99'");
    }

    @Test
    @DisplayName("Should throw BadRequestException when deserialization fails on getSnapshot")
    void shouldThrowBadRequestWhenDeserializationFailsOnGet() throws JsonProcessingException {
        String corruptJson = "{invalid}";
        PortfolioSnapshot snapshot = PortfolioSnapshot.builder()
                .id(UUID.randomUUID())
                .version(1)
                .snapshotData(corruptJson)
                .build();

        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(snapshotRepository.findByPortfolioIdAndVersion(portfolioId, 1)).thenReturn(Optional.of(snapshot));
        when(objectMapper.readValue(corruptJson, PortfolioResponseDto.class))
                .thenThrow(new JsonProcessingException("Corrupted JSON") {});

        assertThatThrownBy(() -> snapshotService.getSnapshot(userId, 1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Không thể đọc dữ liệu snapshot.");
    }

    // ==========================================
    // 3. rollbackToSnapshot Tests
    // ==========================================

    @Test
    @DisplayName("Should successfully rollback portfolio profile, visibility flags, and all child collections")
    void shouldRollbackToSnapshotSuccessfully() throws JsonProcessingException {
        // Prepare historical snapshot data to restore
        PortfolioResponseDto historicalDto = PortfolioResponseDto.builder()
                .id(portfolioId)
                .title("Alex Historical Title")
                .tagline("Historical Tagline")
                .bio("Historical Bio")
                .statusText("OPEN TO WORK")
                .resumeUrl("https://storage/resume-v1.pdf")
                .themeMode("LIGHT")
                .isPublished(false)
                .showProjects(true)
                .showExperience(true)
                .showSkills(true)
                .showEducation(true)
                .showContact(false)
                .socialLinks(List.of(
                        PortfolioResponseDto.SocialLinkDto.builder()
                                .platform("TWITTER")
                                .url("https://x.com/alex")
                                .label("[ X ]")
                                .build()
                ))
                .experiences(List.of(
                        PortfolioResponseDto.ExperienceDto.builder()
                                .roleTitle("Intern Engineer")
                                .companyName("OldCompany")
                                .companyUrl("https://old.com")
                                .location("HCMC")
                                .periodText("2021 - 2022")
                                .description("Old experience")
                                .highlights(List.of("Learned Docker"))
                                .skillsUsed(List.of("Python"))
                                .orderIndex(0)
                                .build()
                ))
                .projects(List.of(
                        PortfolioResponseDto.ProjectDto.builder()
                                .title("Legacy Project")
                                .subtitle("Old school")
                                .role("Developer")
                                .periodText("2021")
                                .description("Legacy project description")
                                .highlights(List.of("Legacy highlight"))
                                .techStack(List.of("Django"))
                                .liveUrl("https://legacy.dev")
                                .githubUrl("https://github.com/alex/legacy")
                                .imageUrl("https://storage/legacy.png")
                                .orderIndex(0)
                                .build()
                ))
                .skills(List.of(
                        PortfolioResponseDto.SkillDto.builder()
                                .category("LANGUAGES")
                                .name("Python")
                                .orderIndex(0)
                                .build()
                ))
                .educations(List.of(
                        PortfolioResponseDto.EducationDto.builder()
                                .institution("High School")
                                .degree("High School Diploma")
                                .fieldOfStudy("General")
                                .periodText("2016 - 2019")
                                .highlights("Honor student")
                                .orderIndex(0)
                                .build()
                ))
                .build();

        String snapshotJson = "{\"title\":\"Alex Historical Title\"}";
        PortfolioSnapshot snapshot = PortfolioSnapshot.builder()
                .id(UUID.randomUUID())
                .portfolio(portfolio)
                .version(1)
                .snapshotData(snapshotJson)
                .build();

        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(snapshotRepository.findByPortfolioIdAndVersion(portfolioId, 1)).thenReturn(Optional.of(snapshot));
        when(objectMapper.readValue(snapshotJson, PortfolioResponseDto.class)).thenReturn(historicalDto);
        when(portfolioRepository.save(any(Portfolio.class))).thenAnswer(inv -> inv.getArgument(0));
        when(portfolioService.getPortfolioByUserId(userId)).thenReturn(Optional.of(historicalDto));

        RollbackResponseDto result = snapshotService.rollbackToSnapshot(userId, 1);

        assertThat(result).isNotNull();
        assertThat(result.getRestoredVersion()).isEqualTo(1);
        assertThat(result.getMessage()).isEqualTo("Khôi phục thành công về phiên bản 1");
        assertThat(result.getPortfolio()).isEqualTo(historicalDto);

        // Verify that entity state was accurately rolled back
        assertThat(portfolio.getTitle()).isEqualTo("Alex Historical Title");
        assertThat(portfolio.getTagline()).isEqualTo("Historical Tagline");
        assertThat(portfolio.getBio()).isEqualTo("Historical Bio");
        assertThat(portfolio.getStatusText()).isEqualTo("OPEN TO WORK");
        assertThat(portfolio.getResumeUrl()).isEqualTo("https://storage/resume-v1.pdf");
        assertThat(portfolio.getThemeMode()).isEqualTo("LIGHT");
        assertThat(portfolio.isPublished()).isFalse();
        assertThat(portfolio.isShowContact()).isFalse();

        // Verify child collections were replaced
        assertThat(portfolio.getSocialLinks()).hasSize(1);
        assertThat(portfolio.getSocialLinks().get(0).getPlatform()).isEqualTo("TWITTER");
        assertThat(portfolio.getExperiences()).hasSize(1);
        assertThat(portfolio.getExperiences().get(0).getRoleTitle()).isEqualTo("Intern Engineer");
        assertThat(portfolio.getProjects()).hasSize(1);
        assertThat(portfolio.getProjects().get(0).getTitle()).isEqualTo("Legacy Project");
        assertThat(portfolio.getSkills()).hasSize(1);
        assertThat(portfolio.getSkills().get(0).getName()).isEqualTo("Python");
        assertThat(portfolio.getEducations()).hasSize(1);
        assertThat(portfolio.getEducations().get(0).getInstitution()).isEqualTo("High School");

        verify(portfolioRepository).save(portfolio);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when rolling back for unknown user portfolio")
    void shouldThrowNotFoundWhenRollingBackNonExistentPortfolio() {
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> snapshotService.rollbackToSnapshot(userId, 1))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(portfolioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when rolling back to non-existent snapshot version")
    void shouldThrowNotFoundWhenRollingBackToNonExistentVersion() {
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(snapshotRepository.findByPortfolioIdAndVersion(portfolioId, 99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> snapshotService.rollbackToSnapshot(userId, 99))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Snapshot not found with version: '99'");

        verify(portfolioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BadRequestException when deserialization fails during rollback")
    void shouldThrowBadRequestWhenDeserializationFailsOnRollback() throws JsonProcessingException {
        String badJson = "{corrupt}";
        PortfolioSnapshot snapshot = PortfolioSnapshot.builder()
                .version(1)
                .snapshotData(badJson)
                .build();

        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(snapshotRepository.findByPortfolioIdAndVersion(portfolioId, 1)).thenReturn(Optional.of(snapshot));
        when(objectMapper.readValue(badJson, PortfolioResponseDto.class))
                .thenThrow(new JsonProcessingException("Bad json") {});

        assertThatThrownBy(() -> snapshotService.rollbackToSnapshot(userId, 1))
                .isInstanceOf(BadRequestException.class);

        verify(portfolioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should clear all existing collections when snapshot data has null collections")
    void shouldClearCollectionsWhenSnapshotDataHasNullCollections() throws JsonProcessingException {
        portfolio.getSocialLinks().add(SocialLink.builder().platform("GITHUB").url("https://github.com/alex").build());
        portfolio.getExperiences().add(Experience.builder().roleTitle("Dev").companyName("Corp").periodText("2020").build());
        portfolio.getProjects().add(Project.builder().title("Proj").build());
        portfolio.getSkills().add(Skill.builder().category("BACKEND").name("Java").build());
        portfolio.getEducations().add(Education.builder().institution("Uni").degree("BS").periodText("2020").build());

        PortfolioResponseDto emptyDto = PortfolioResponseDto.builder()
                .id(portfolioId)
                .title("Empty Collections")
                .socialLinks(null)
                .experiences(null)
                .projects(null)
                .skills(null)
                .educations(null)
                .build();

        String snapshotJson = "{\"title\":\"Empty Collections\"}";
        PortfolioSnapshot snapshot = PortfolioSnapshot.builder()
                .portfolio(portfolio)
                .version(2)
                .snapshotData(snapshotJson)
                .build();

        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(snapshotRepository.findByPortfolioIdAndVersion(portfolioId, 2)).thenReturn(Optional.of(snapshot));
        when(objectMapper.readValue(snapshotJson, PortfolioResponseDto.class)).thenReturn(emptyDto);
        when(portfolioRepository.save(any(Portfolio.class))).thenAnswer(inv -> inv.getArgument(0));
        when(portfolioService.getPortfolioByUserId(userId)).thenReturn(Optional.of(emptyDto));

        snapshotService.rollbackToSnapshot(userId, 2);

        assertThat(portfolio.getSocialLinks()).isEmpty();
        assertThat(portfolio.getExperiences()).isEmpty();
        assertThat(portfolio.getProjects()).isEmpty();
        assertThat(portfolio.getSkills()).isEmpty();
        assertThat(portfolio.getEducations()).isEmpty();
    }
}

