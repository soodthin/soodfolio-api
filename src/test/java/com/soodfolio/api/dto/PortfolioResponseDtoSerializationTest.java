package com.soodfolio.api.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PortfolioResponseDtoSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Should successfully round-trip serialize and deserialize full PortfolioResponseDto")
    void shouldRoundTripSerializeAndDeserializeFullDto() throws Exception {
        UUID id = UUID.randomUUID();
        UUID expId = UUID.randomUUID();
        UUID projId = UUID.randomUUID();
        UUID skillId = UUID.randomUUID();
        UUID eduId = UUID.randomUUID();

        PortfolioResponseDto dto = PortfolioResponseDto.builder()
                .id(id)
                .username("thinhdo")
                .fullName("Thinh Do")
                .avatarUrl("https://example.com/avatar.png")
                .title("Senior Backend Engineer")
                .tagline("High-throughput distributed systems")
                .bio("10 years building Java microservices")
                .statusText("OPEN_TO_WORK")
                .resumeUrl("https://example.com/cv.pdf")
                .themeMode("DARK")
                .themeConfig("{\"palette\":\"cyberpunk\"}")
                .isPublished(true)
                .totalViews(1250)
                .showExperience(true)
                .showProjects(true)
                .showSkills(true)
                .showEducation(true)
                .showContact(true)
                .socialLinks(List.of(
                        PortfolioResponseDto.SocialLinkDto.builder()
                                .platform("GITHUB")
                                .url("https://github.com/thinhdo")
                                .label("GitHub")
                                .build()
                ))
                .experiences(List.of(
                        PortfolioResponseDto.ExperienceDto.builder()
                                .id(expId)
                                .roleTitle("Staff Engineer")
                                .companyName("Tech Corp")
                                .companyUrl("https://tech.corp")
                                .location("Hanoi, Vietnam")
                                .periodText("2021 - Present")
                                .description("Architected microservices")
                                .highlights(List.of("Cut p99 by 40%"))
                                .skillsUsed(List.of("Java", "Spring Boot", "Kafka"))
                                .orderIndex(0)
                                .build()
                ))
                .projects(List.of(
                        PortfolioResponseDto.ProjectDto.builder()
                                .id(projId)
                                .title("SoodFolio API")
                                .subtitle("Developer Portfolio Engine")
                                .role("Lead Architect")
                                .periodText("2024")
                                .description("High performance portfolio platform")
                                .highlights(List.of("P0 Security hardened"))
                                .techStack(List.of("Java 17", "Spring Boot 3", "PostgreSQL"))
                                .liveUrl("https://soodfolio.dev")
                                .githubUrl("https://github.com/soodthin/soodfolio-api")
                                .imageUrl("https://example.com/preview.png")
                                .orderIndex(0)
                                .build()
                ))
                .skills(List.of(
                        PortfolioResponseDto.SkillDto.builder()
                                .id(skillId)
                                .category("BACKEND")
                                .name("Spring Boot")
                                .orderIndex(0)
                                .build()
                ))
                .educations(List.of(
                        PortfolioResponseDto.EducationDto.builder()
                                .id(eduId)
                                .institution("Hanoi University of Science and Technology")
                                .degree("Bachelor of Science")
                                .fieldOfStudy("Computer Science")
                                .periodText("2016 - 2020")
                                .highlights("GPA 3.8/4.0")
                                .orderIndex(0)
                                .build()
                ))
                .build();

        String json = objectMapper.writeValueAsString(dto);
        assertThat(json).contains("\"isPublished\":true");

        PortfolioResponseDto deserialized = objectMapper.readValue(json, PortfolioResponseDto.class);

        assertThat(deserialized).isEqualTo(dto);
        assertThat(deserialized.isPublished()).isTrue();
        assertThat(deserialized.getExperiences()).hasSize(1);
        assertThat(deserialized.getExperiences().get(0).getRoleTitle()).isEqualTo("Staff Engineer");
        assertThat(deserialized.getProjects()).hasSize(1);
        assertThat(deserialized.getProjects().get(0).getTitle()).isEqualTo("SoodFolio API");
        assertThat(deserialized.getSocialLinks()).hasSize(1);
        assertThat(deserialized.getSkills()).hasSize(1);
        assertThat(deserialized.getEducations()).hasSize(1);
    }

    @Test
    @DisplayName("Should maintain backward compatibility when legacy JSON is missing new fields")
    void shouldMaintainBackwardCompatibilityWhenLegacyJsonMissingNewFields() throws Exception {
        String legacyJson = """
                {
                  "username": "legacy_user",
                  "fullName": "Legacy User",
                  "isPublished": false,
                  "totalViews": 50,
                  "socialLinks": [],
                  "experiences": []
                }
                """;

        PortfolioResponseDto dto = objectMapper.readValue(legacyJson, PortfolioResponseDto.class);

        assertThat(dto).isNotNull();
        assertThat(dto.getUsername()).isEqualTo("legacy_user");
        assertThat(dto.getFullName()).isEqualTo("Legacy User");
        assertThat(dto.isPublished()).isFalse();
        assertThat(dto.getTotalViews()).isEqualTo(50);
        assertThat(dto.getThemeConfig()).isNull();
        assertThat(dto.isShowContact()).isFalse();
    }

    @Test
    @DisplayName("Should maintain forward compatibility when JSON contains unknown future fields")
    void shouldMaintainForwardCompatibilityWhenJsonContainsUnknownFutureFields() throws Exception {
        String futureJson = """
                {
                  "username": "future_user",
                  "fullName": "Future User",
                  "isPublished": true,
                  "unknownFutureField": "experimental_value",
                  "futureMetrics": {
                    "stars": 999,
                    "reputation": "elite"
                  },
                  "experiences": [
                    {
                      "roleTitle": "Quantum Architect",
                      "companyName": "Future Corp",
                      "unsupportedMeta": "discard_me"
                    }
                  ],
                  "projects": [
                    {
                      "title": "Quantum OS",
                      "experimentalFlag": true
                    }
                  ]
                }
                """;

        PortfolioResponseDto dto = objectMapper.readValue(futureJson, PortfolioResponseDto.class);

        assertThat(dto).isNotNull();
        assertThat(dto.getUsername()).isEqualTo("future_user");
        assertThat(dto.isPublished()).isTrue();
        assertThat(dto.getExperiences()).hasSize(1);
        assertThat(dto.getExperiences().get(0).getRoleTitle()).isEqualTo("Quantum Architect");
        assertThat(dto.getProjects()).hasSize(1);
        assertThat(dto.getProjects().get(0).getTitle()).isEqualTo("Quantum OS");
    }

    @Test
    @DisplayName("Should successfully deserialize SnapshotDtos.CreateSnapshotRequest from JSON")
    void shouldDeserializeCreateSnapshotRequest() throws Exception {
        String jsonWithChangelog = "{\"changelog\":\"v2 updated\"}";
        SnapshotDtos.CreateSnapshotRequest req = objectMapper.readValue(jsonWithChangelog, SnapshotDtos.CreateSnapshotRequest.class);
        assertThat(req).isNotNull();
        assertThat(req.getChangelog()).isEqualTo("v2 updated");

        String emptyJson = "{}";
        SnapshotDtos.CreateSnapshotRequest emptyReq = objectMapper.readValue(emptyJson, SnapshotDtos.CreateSnapshotRequest.class);
        assertThat(emptyReq).isNotNull();
        assertThat(emptyReq.getChangelog()).isNull();
    }
}
