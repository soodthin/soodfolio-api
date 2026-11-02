package com.soodfolio.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soodfolio.api.dto.AiDtos.AiPolishRequestDto;
import com.soodfolio.api.dto.AiDtos.AiPolishResponseDto;
import com.soodfolio.api.dto.AiDtos.AiThemeSuggestRequestDto;
import com.soodfolio.api.dto.AiDtos.AiThemeSuggestResponseDto;
import com.soodfolio.api.exception.GlobalExceptionHandler;
import com.soodfolio.api.exception.RateLimitExceededException;
import com.soodfolio.api.security.UserPrincipal;
import com.soodfolio.api.service.AiRateLimiter;
import com.soodfolio.api.service.AiService;
import com.soodfolio.api.service.I18nService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiController Standalone MockMvc Tests")
class AiControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AiService aiService;

    @Mock
    private AiRateLimiter aiRateLimiter;

    @Mock
    private I18nService i18nService;

    @InjectMocks
    private AiController aiController;

    private ObjectMapper objectMapper;
    private UserPrincipal testPrincipal;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        testUserId = UUID.randomUUID();
        testPrincipal = new UserPrincipal(
                testUserId,
                "alex@soodfolio.dev",
                "encodedPassword",
                "Alex Sood",
                AuthorityUtils.createAuthorityList("ROLE_USER")
        );

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                if ("true".equalsIgnoreCase(webRequest.getHeader("X-Anonymous"))) {
                    return null;
                }
                return testPrincipal;
            }
        };

        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler(i18nService);

        mockMvc = MockMvcBuilders.standaloneSetup(aiController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .setControllerAdvice(exceptionHandler)
                .build();

        lenient().when(i18nService.getMessage(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ==========================================
    // 1. POST /ai/polish Tests
    // ==========================================

    @Test
    @DisplayName("POST /ai/polish - Success with valid payload and ATS X-Y-Z response")
    void shouldPolishTextSuccessfullyWithValidPayload() throws Exception {
        AiPolishRequestDto requestDto = AiPolishRequestDto.builder()
                .rawText("Optimized database indexing in PostgreSQL and implemented Redis caching in Spring Boot 3.")
                .field("exp_bullet")
                .build();

        AiPolishResponseDto responseDto = AiPolishResponseDto.builder()
                .metricXyz("Accomplished 40% latency reduction, measured by p99 dropping to 220ms, by doing PostgreSQL index tuning and Redis caching in Spring Boot 3.")
                .architectural("Architected distributed cache-aside pattern with TTL invalidation.")
                .concise("Optimized database access with 40% latency improvement.")
                .build();

        when(aiService.polishText(any(AiPolishRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/ai/polish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metricXyz").value(responseDto.getMetricXyz()))
                .andExpect(jsonPath("$.metricXyz").value(org.hamcrest.Matchers.containsString("Accomplished")))
                .andExpect(jsonPath("$.metricXyz").value(org.hamcrest.Matchers.containsString("measured by")))
                .andExpect(jsonPath("$.metricXyz").value(org.hamcrest.Matchers.containsString("by doing")))
                .andExpect(jsonPath("$.architectural").value(responseDto.getArchitectural()))
                .andExpect(jsonPath("$.concise").value(responseDto.getConcise()));

        verify(aiRateLimiter).checkRateLimit(eq(testUserId));
        verify(aiService).polishText(any(AiPolishRequestDto.class));
    }

    @Test
    @DisplayName("POST /ai/polish - 400 Bad Request when rawText is blank")
    void shouldRejectPolishRequestWithBlankRawText() throws Exception {
        AiPolishRequestDto requestDto = AiPolishRequestDto.builder()
                .rawText("   ")
                .field("project_desc")
                .build();

        mockMvc.perform(post("/ai/polish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.rawText").exists());

        verifyNoInteractions(aiRateLimiter);
        verifyNoInteractions(aiService);
    }

    @Test
    @DisplayName("POST /ai/polish - 400 Bad Request when field is invalid")
    void shouldRejectPolishRequestWithInvalidField() throws Exception {
        AiPolishRequestDto requestDto = AiPolishRequestDto.builder()
                .rawText("Valid portfolio text")
                .field("invalid_field_type")
                .build();

        mockMvc.perform(post("/ai/polish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.field").exists());

        verifyNoInteractions(aiRateLimiter);
        verifyNoInteractions(aiService);
    }

    @Test
    @DisplayName("POST /ai/polish - 400 Bad Request when rawText exceeds 2000 characters")
    void shouldRejectPolishRequestWhenRawTextExceedsLimit() throws Exception {
        String longText = "A".repeat(2001);
        AiPolishRequestDto requestDto = AiPolishRequestDto.builder()
                .rawText(longText)
                .field("bio")
                .build();

        mockMvc.perform(post("/ai/polish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.rawText").exists());

        verifyNoInteractions(aiRateLimiter);
        verifyNoInteractions(aiService);
    }

    @Test
    @DisplayName("POST /ai/polish - 429 Too Many Requests when rate limit exceeded")
    void shouldEnforceRateLimitOnPolish() throws Exception {
        doThrow(new RateLimitExceededException("Bạn đã vượt quá giới hạn 15 yêu cầu AI mỗi phút. Vui lòng chờ giây lát."))
                .when(aiRateLimiter).checkRateLimit(eq(testUserId));

        AiPolishRequestDto requestDto = AiPolishRequestDto.builder()
                .rawText("Valid text to polish")
                .field("exp_bullet")
                .build();

        mockMvc.perform(post("/ai/polish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("15 yêu cầu AI mỗi phút")));

        verify(aiRateLimiter).checkRateLimit(eq(testUserId));
        verifyNoInteractions(aiService);
    }

    @Test
    @DisplayName("POST /ai/polish - 401 Unauthorized when unauthenticated")
    void shouldReturn401WhenUnauthenticatedOnPolish() throws Exception {
        AiPolishRequestDto requestDto = AiPolishRequestDto.builder()
                .rawText("Valid text to polish")
                .field("exp_bullet")
                .build();

        mockMvc.perform(post("/ai/polish")
                        .header("X-Anonymous", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        verifyNoInteractions(aiRateLimiter);
        verifyNoInteractions(aiService);
    }

    // ==========================================
    // 2. POST /ai/suggest-theme Tests
    // ==========================================

    @Test
    @DisplayName("POST /ai/suggest-theme - Success with valid profile and template recommendation")
    void shouldSuggestThemeSuccessfullyWithValidPayload() throws Exception {
        AiThemeSuggestRequestDto requestDto = AiThemeSuggestRequestDto.builder()
                .title("Staff Infrastructure Engineer")
                .bio("Specialist in Kubernetes, high-concurrency event streaming, and cloud platforms.")
                .skills(List.of("Java", "Go", "Kubernetes", "Kafka", "Docker"))
                .build();

        AiThemeSuggestResponseDto responseDto = AiThemeSuggestResponseDto.builder()
                .recommendedTemplateId("terminal")
                .reason("Đề xuất giao diện Terminal Matrix dựa trên năng lực chuyên sâu về Backend và Cloud.")
                .build();

        when(aiService.suggestTheme(any(AiThemeSuggestRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/ai/suggest-theme")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendedTemplateId").value("terminal"))
                .andExpect(jsonPath("$.reason").value(responseDto.getReason()));

        verify(aiRateLimiter).checkRateLimit(eq(testUserId));
        verify(aiService).suggestTheme(any(AiThemeSuggestRequestDto.class));
    }

    @Test
    @DisplayName("POST /ai/suggest-theme - 400 Bad Request when title is blank")
    void shouldRejectSuggestThemeWithBlankTitle() throws Exception {
        AiThemeSuggestRequestDto requestDto = AiThemeSuggestRequestDto.builder()
                .title("   ")
                .skills(List.of("Java"))
                .build();

        mockMvc.perform(post("/ai/suggest-theme")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.title").exists());

        verifyNoInteractions(aiRateLimiter);
        verifyNoInteractions(aiService);
    }

    @Test
    @DisplayName("POST /ai/suggest-theme - 400 Bad Request when title exceeds 200 characters")
    void shouldRejectSuggestThemeWhenTitleExceedsLimit() throws Exception {
        String longTitle = "T".repeat(201);
        AiThemeSuggestRequestDto requestDto = AiThemeSuggestRequestDto.builder()
                .title(longTitle)
                .build();

        mockMvc.perform(post("/ai/suggest-theme")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.title").exists());

        verifyNoInteractions(aiRateLimiter);
        verifyNoInteractions(aiService);
    }

    @Test
    @DisplayName("POST /ai/suggest-theme - 429 Too Many Requests when rate limit exceeded")
    void shouldEnforceRateLimitOnSuggestTheme() throws Exception {
        doThrow(new RateLimitExceededException("Bạn đã vượt quá giới hạn 15 yêu cầu AI mỗi phút."))
                .when(aiRateLimiter).checkRateLimit(eq(testUserId));

        AiThemeSuggestRequestDto requestDto = AiThemeSuggestRequestDto.builder()
                .title("Fullstack Developer")
                .build();

        mockMvc.perform(post("/ai/suggest-theme")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429));

        verify(aiRateLimiter).checkRateLimit(eq(testUserId));
        verifyNoInteractions(aiService);
    }

    @Test
    @DisplayName("POST /ai/suggest-theme - 401 Unauthorized when unauthenticated")
    void shouldReturn401WhenUnauthenticatedOnSuggestTheme() throws Exception {
        AiThemeSuggestRequestDto requestDto = AiThemeSuggestRequestDto.builder()
                .title("Fullstack Developer")
                .build();

        mockMvc.perform(post("/ai/suggest-theme")
                        .header("X-Anonymous", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        verifyNoInteractions(aiRateLimiter);
        verifyNoInteractions(aiService);
    }

    @Test
    @DisplayName("POST /ai/suggest-theme - Success with minimal payload (optional bio and skills omitted)")
    void shouldAllowMinimalPayloadOnSuggestTheme() throws Exception {
        AiThemeSuggestRequestDto requestDto = AiThemeSuggestRequestDto.builder()
                .title("Software Engineer")
                .build();

        AiThemeSuggestResponseDto responseDto = AiThemeSuggestResponseDto.builder()
                .recommendedTemplateId("classic")
                .reason("Đề xuất giao diện Classic Deck.")
                .build();

        when(aiService.suggestTheme(any(AiThemeSuggestRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/ai/suggest-theme")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendedTemplateId").value("classic"));

        verify(aiRateLimiter).checkRateLimit(eq(testUserId));
        verify(aiService).suggestTheme(any(AiThemeSuggestRequestDto.class));
    }
}
