package com.soodfolio.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soodfolio.api.client.GeminiRestClient;
import com.soodfolio.api.dto.AiDtos.AiPolishRequestDto;
import com.soodfolio.api.dto.AiDtos.AiPolishResponseDto;
import com.soodfolio.api.dto.AiDtos.AiThemeSuggestRequestDto;
import com.soodfolio.api.dto.AiDtos.AiThemeSuggestResponseDto;
import com.soodfolio.api.exception.AiServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiServiceImpl Unit Tests with ATS-Validator & Fallback Verification")
class AiServiceImplTest {

    @Mock
    private GeminiRestClient geminiRestClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AiServiceImpl aiService;

    private ArgumentCaptor<String> systemPromptCaptor;
    private ArgumentCaptor<String> userPromptCaptor;

    @BeforeEach
    void setUp() {
        systemPromptCaptor = ArgumentCaptor.forClass(String.class);
        userPromptCaptor = ArgumentCaptor.forClass(String.class);
    }

    // ==========================================
    // 1. Polish Text Tests (ATS X-Y-Z & Prompts)
    // ==========================================

    @Test
    @DisplayName("Should polish text via Gemini with strict ATS X-Y-Z compliance")
    void shouldPolishTextUsingGeminiWhenClientSucceeds() {
        String geminiResponseJson = """
                {
                  "metricXyz": "Accomplished 45% latency reduction in payment checkout, measured by p99 dropping from 800ms to 440ms at 10k RPS, by optimizing Java and Spring Boot 3 connection pooling.",
                  "architectural": "Architected resilient transactional pipeline utilizing Redis distributed locks and PostgreSQL composite indexing.",
                  "concise": "Scaled high-throughput payment services to 10k RPS with 45% lower latency."
                }
                """;

        when(geminiRestClient.generateContent(systemPromptCaptor.capture(), userPromptCaptor.capture()))
                .thenReturn(geminiResponseJson);

        AiPolishRequestDto request = AiPolishRequestDto.builder()
                .rawText("I worked on payment checkout with Java and Spring Boot.")
                .field("project_desc")
                .build();

        AiPolishResponseDto response = aiService.polishText(request);

        assertThat(response).isNotNull();
        assertThat(response.getMetricXyz())
                .contains("45%")
                .contains("p99")
                .contains("Java")
                .contains("Spring Boot 3")
                .doesNotContainIgnoringCase("Responsible for")
                .doesNotContainIgnoringCase("Worked on");

        assertThat(response.getArchitectural()).contains("PostgreSQL", "Redis");
        assertThat(response.getConcise()).isNotEmpty();

        // Verify ATS System Prompt invariants
        String systemPrompt = systemPromptCaptor.getValue();
        assertThat(systemPrompt)
                .contains("ats-validator")
                .contains("Google / IBM X-Y-Z")
                .contains("Accomplished [X], measured by [Y], by doing [Z]")
                .contains("Java, Spring Boot 3, Next.js, Redis, Docker, PostgreSQL")
                .contains("Responsible for...");

        // Verify user prompt
        String userPrompt = userPromptCaptor.getValue();
        assertThat(userPrompt)
                .contains("Field: project_desc")
                .contains("Raw Text: I worked on payment checkout with Java and Spring Boot.");
    }

    @Test
    @DisplayName("Should fallback to heuristic polish when Gemini throws AiServiceUnavailableException")
    void shouldFallbackToHeuristicPolishWhenGeminiThrowsAiServiceUnavailableException() {
        when(geminiRestClient.generateContent(anyString(), anyString()))
                .thenThrow(new AiServiceUnavailableException("Connection timed out after 8s"));

        AiPolishRequestDto request = AiPolishRequestDto.builder()
                .rawText("Responsible for developing backend auth microservices with Spring Boot 3.")
                .field("exp_bullet")
                .build();

        AiPolishResponseDto response = aiService.polishText(request);

        assertThat(response).isNotNull();

        // 1. Rejection of anti-pattern "Responsible for"
        assertThat(response.getMetricXyz())
                .doesNotContainIgnoringCase("Responsible for")
                .startsWith("Optimized developing backend auth microservices with Spring Boot 3");

        // 2. Google / IBM X-Y-Z formula enforcement in fallback
        assertThat(response.getMetricXyz())
                .contains("improving system throughput by 35%")
                .contains("through standardized engineering best practices");

        // 3. Architectural option
        assertThat(response.getArchitectural())
                .startsWith("Architected scalable solution for")
                .contains("modular service boundaries and resilient data patterns");

        // 4. Concise option
        assertThat(response.getConcise())
                .startsWith("Engineered high-performance delivery for");
    }

    @Test
    @DisplayName("Should strip various passive prefixes in fallback polish")
    void shouldStripPassivePrefixesInFallbackPolish() {
        when(geminiRestClient.generateContent(anyString(), anyString()))
                .thenThrow(new AiServiceUnavailableException("Offline mode"));

        List<String> passivePrefixes = List.of(
                "Responsible for Docker and Kubernetes deployment",
                "Assisted in Docker and Kubernetes deployment",
                "Worked on Docker and Kubernetes deployment",
                "Helped with Docker and Kubernetes deployment"
        );

        for (String input : passivePrefixes) {
            AiPolishRequestDto request = AiPolishRequestDto.builder()
                    .rawText(input)
                    .field("project_desc")
                    .build();

            AiPolishResponseDto response = aiService.polishText(request);
            assertThat(response.getMetricXyz())
                    .as("Passive prefix must be stripped for: " + input)
                    .doesNotContainIgnoringCase("responsible for")
                    .doesNotContainIgnoringCase("assisted in")
                    .doesNotContainIgnoringCase("worked on")
                    .doesNotContainIgnoringCase("helped with")
                    .contains("Docker and Kubernetes deployment");
        }
    }

    @Test
    @DisplayName("Should handle empty, whitespace-only, or null text gracefully in fallback polish")
    void shouldHandleBlankRawTextInFallbackPolish() {
        when(geminiRestClient.generateContent(anyString(), anyString()))
                .thenThrow(new AiServiceUnavailableException("Offline mode"));

        AiPolishRequestDto requestEmpty = AiPolishRequestDto.builder()
                .rawText("")
                .field("bio")
                .build();

        AiPolishResponseDto responseEmpty = aiService.polishText(requestEmpty);
        assertThat(responseEmpty).isNotNull();
        assertThat(responseEmpty.getMetricXyz())
                .contains("core application services")
                .contains("35%");

        AiPolishRequestDto requestWhitespace = AiPolishRequestDto.builder()
                .rawText("    ")
                .field("bio")
                .build();

        AiPolishResponseDto responseWhitespace = aiService.polishText(requestWhitespace);
        assertThat(responseWhitespace.getMetricXyz())
                .contains("core application services")
                .contains("35%");

        AiPolishRequestDto requestNull = AiPolishRequestDto.builder()
                .rawText(null)
                .field("bio")
                .build();

        AiPolishResponseDto responseNull = aiService.polishText(requestNull);
        assertThat(responseNull.getMetricXyz())
                .contains("core application services")
                .contains("35%");
    }

    @Test
    @DisplayName("Should fallback gracefully when Gemini returns malformed or non-JSON string")
    void shouldFallbackWhenGeminiReturnsMalformedJson() {
        when(geminiRestClient.generateContent(anyString(), anyString()))
                .thenReturn("<<< INVALID NON-JSON RESPONSE >>>");

        AiPolishRequestDto request = AiPolishRequestDto.builder()
                .rawText("Optimizing PostgreSQL query execution plans")
                .field("exp_bullet")
                .build();

        AiPolishResponseDto response = aiService.polishText(request);
        assertThat(response).isNotNull();
        assertThat(response.getMetricXyz()).contains("35%");
        assertThat(response.getArchitectural()).contains("Architected scalable solution");
    }

    // ==========================================
    // 2. Suggest Theme Tests
    // ==========================================

    @Test
    @DisplayName("Should suggest theme via Gemini when client succeeds")
    void shouldSuggestThemeUsingGeminiWhenClientSucceeds() {
        String geminiResponseJson = """
                {
                  "recommendedTemplateId": "terminal",
                  "reason": "Đề xuất phong cách Terminal Matrix phù hợp nhất với kỹ sư hạ tầng và backend phân tán."
                }
                """;

        when(geminiRestClient.generateContent(systemPromptCaptor.capture(), userPromptCaptor.capture()))
                .thenReturn(geminiResponseJson);

        AiThemeSuggestRequestDto request = AiThemeSuggestRequestDto.builder()
                .title("Senior Cloud Architect")
                .bio("10+ years specializing in distributed systems, Kubernetes, and Golang.")
                .skills(List.of("Go", "Kubernetes", "AWS", "Terraform"))
                .build();

        AiThemeSuggestResponseDto response = aiService.suggestTheme(request);

        assertThat(response).isNotNull();
        assertThat(response.getRecommendedTemplateId()).isEqualTo("terminal");
        assertThat(response.getReason()).contains("Terminal Matrix");

        // Verify Theme System Prompt
        String systemPrompt = systemPromptCaptor.getValue();
        assertThat(systemPrompt)
                .contains("classic")
                .contains("spotlight")
                .contains("terminal")
                .contains("editorial")
                .contains("recommendedTemplateId");
    }

    @Test
    @DisplayName("Should fallback to 'terminal' theme for backend & DevOps profiles")
    void shouldFallbackToTerminalThemeForBackendSkills() {
        when(geminiRestClient.generateContent(anyString(), anyString()))
                .thenThrow(new AiServiceUnavailableException("Gemini unavailable"));

        AiThemeSuggestRequestDto request = AiThemeSuggestRequestDto.builder()
                .title("Senior Backend Engineer")
                .bio("Specializing in high-throughput Java microservices and PostgreSQL tuning.")
                .skills(List.of("Java", "Spring Boot", "Docker", "PostgreSQL", "Redis"))
                .build();

        AiThemeSuggestResponseDto response = aiService.suggestTheme(request);

        assertThat(response).isNotNull();
        assertThat(response.getRecommendedTemplateId()).isEqualTo("terminal");
        assertThat(response.getReason())
                .contains("Terminal Matrix")
                .contains("Backend, Cloud và Hệ thống phân tán");
    }

    @Test
    @DisplayName("Should fallback to 'spotlight' theme for frontend & UI/UX profiles")
    void shouldFallbackToSpotlightThemeForFrontendSkills() {
        when(geminiRestClient.generateContent(anyString(), anyString()))
                .thenThrow(new AiServiceUnavailableException("Gemini unavailable"));

        AiThemeSuggestRequestDto request = AiThemeSuggestRequestDto.builder()
                .title("Lead Frontend Developer")
                .bio("Passionate about crafting interactive web interfaces and sleek design systems.")
                .skills(List.of("React", "Next.js", "Tailwind CSS", "UI/UX", "Animation"))
                .build();

        AiThemeSuggestResponseDto response = aiService.suggestTheme(request);

        assertThat(response).isNotNull();
        assertThat(response.getRecommendedTemplateId()).isEqualTo("spotlight");
        assertThat(response.getReason())
                .contains("Spotlight Focus")
                .contains("trực quan và trải nghiệm người dùng");
    }

    @Test
    @DisplayName("Should fallback to 'editorial' theme for leadership & architect profiles")
    void shouldFallbackToEditorialThemeForLeadershipProfiles() {
        when(geminiRestClient.generateContent(anyString(), anyString()))
                .thenThrow(new AiServiceUnavailableException("Gemini unavailable"));

        AiThemeSuggestRequestDto request = AiThemeSuggestRequestDto.builder()
                .title("Engineering Director & Tech Lead")
                .bio("Technical writer, enterprise consultant, and open source author.")
                .skills(List.of("Engineering Leadership", "Technical Writing", "Consulting"))
                .build();

        AiThemeSuggestResponseDto response = aiService.suggestTheme(request);

        assertThat(response).isNotNull();
        assertThat(response.getRecommendedTemplateId()).isEqualTo("editorial");
        assertThat(response.getReason())
                .contains("Editorial Clean")
                .contains("chiều sâu lãnh đạo kỹ thuật");
    }

    @Test
    @DisplayName("Should fallback to 'classic' theme for general software engineering profiles")
    void shouldFallbackToClassicThemeForGeneralProfile() {
        when(geminiRestClient.generateContent(anyString(), anyString()))
                .thenThrow(new AiServiceUnavailableException("Gemini unavailable"));

        AiThemeSuggestRequestDto request = AiThemeSuggestRequestDto.builder()
                .title("Software Developer")
                .bio("General software application developer.")
                .skills(List.of("Algorithms", "Data Structures"))
                .build();

        AiThemeSuggestResponseDto response = aiService.suggestTheme(request);

        assertThat(response).isNotNull();
        assertThat(response.getRecommendedTemplateId()).isEqualTo("classic");
        assertThat(response.getReason())
                .contains("Classic Deck")
                .contains("cân đối, chuẩn mực");
    }

    @Test
    @DisplayName("Should trigger heuristic fallback if Gemini returns blank template ID")
    void shouldFallbackWhenGeminiReturnsBlankTemplateId() {
        when(geminiRestClient.generateContent(anyString(), anyString()))
                .thenReturn("{\"recommendedTemplateId\": \"   \", \"reason\": \"\"}");

        AiThemeSuggestRequestDto request = AiThemeSuggestRequestDto.builder()
                .title("Backend Developer")
                .skills(List.of("PostgreSQL", "Docker"))
                .build();

        AiThemeSuggestResponseDto response = aiService.suggestTheme(request);

        assertThat(response).isNotNull();
        assertThat(response.getRecommendedTemplateId()).isEqualTo("terminal");
    }
}
