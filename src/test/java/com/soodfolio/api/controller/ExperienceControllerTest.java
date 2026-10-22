package com.soodfolio.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soodfolio.api.dto.ExperienceDtos.ExperienceReorderRequestDto;
import com.soodfolio.api.dto.ExperienceDtos.ExperienceRequestDto;
import com.soodfolio.api.dto.PortfolioResponseDto.ExperienceDto;
import com.soodfolio.api.exception.GlobalExceptionHandler;
import com.soodfolio.api.exception.ResourceNotFoundException;
import com.soodfolio.api.security.UserPrincipal;
import com.soodfolio.api.service.ExperienceService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ExperienceControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ExperienceService experienceService;

    @Mock
    private I18nService i18nService;

    @InjectMocks
    private ExperienceController experienceController;

    private ObjectMapper objectMapper;
    private UserPrincipal testPrincipal;
    private UUID userId;
    private ExperienceDto sampleExperienceDto;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        userId = UUID.randomUUID();
        testPrincipal = new UserPrincipal(
                userId,
                "lead@soodfolio.dev",
                "password",
                "Lead Architect",
                org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_USER")
        );

        sampleExperienceDto = ExperienceDto.builder()
                .id(UUID.randomUUID())
                .roleTitle("Principal Architect")
                .companyName("CloudScale Inc")
                .periodText("2024 — Present")
                .orderIndex(0)
                .build();

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

        mockMvc = MockMvcBuilders.standaloneSetup(experienceController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .setControllerAdvice(exceptionHandler)
                .build();

        lenient().when(i18nService.getMessage(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("GET /portfolios/me/experiences - 200 OK when authenticated")
    void shouldReturnExperiencesWhenAuthenticated() throws Exception {
        when(experienceService.getExperiencesByUserId(userId)).thenReturn(List.of(sampleExperienceDto));

        mockMvc.perform(get("/portfolios/me/experiences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roleTitle").value("Principal Architect"))
                .andExpect(jsonPath("$[0].companyName").value("CloudScale Inc"));
    }

    @Test
    @DisplayName("GET /portfolios/me/experiences - 401 Unauthorized when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/portfolios/me/experiences")
                        .header("X-Anonymous", "true"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /portfolios/me/experiences - 201 Created on valid request")
    void shouldCreateExperienceSuccessfully() throws Exception {
        ExperienceRequestDto request = ExperienceRequestDto.builder()
                .roleTitle("Senior Backend Engineer")
                .companyName("Tech Corp")
                .build();

        when(experienceService.createExperience(eq(userId), any(ExperienceRequestDto.class))).thenReturn(sampleExperienceDto);

        mockMvc.perform(post("/portfolios/me/experiences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roleTitle").value("Principal Architect"));
    }

    @Test
    @DisplayName("POST /portfolios/me/experiences - 400 Bad Request when role or company is blank (@Valid)")
    void shouldRejectInvalidExperiencePayload() throws Exception {
        ExperienceRequestDto invalidRequest = ExperienceRequestDto.builder()
                .roleTitle("") // Blank violates @NotBlank
                .companyName("")
                .build();

        mockMvc.perform(post("/portfolios/me/experiences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.roleTitle").exists())
                .andExpect(jsonPath("$.validationErrors.companyName").exists());
    }

    @Test
    @DisplayName("GET /portfolios/me/experiences/{id} - 404 Not Found when experience does not exist")
    void shouldReturn404WhenNotFound() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(experienceService.getExperienceById(userId, unknownId))
                .thenThrow(new ResourceNotFoundException("Experience", "id", unknownId));

        mockMvc.perform(get("/portfolios/me/experiences/" + unknownId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("DELETE /portfolios/me/experiences/{id} - 204 No Content")
    void shouldDeleteExperienceSuccessfully() throws Exception {
        UUID expId = sampleExperienceDto.getId();
        doNothing().when(experienceService).deleteExperience(userId, expId);

        mockMvc.perform(delete("/portfolios/me/experiences/" + expId))
                .andExpect(status().isNoContent());

        verify(experienceService).deleteExperience(userId, expId);
    }

    @Test
    @DisplayName("PUT /portfolios/me/experiences/reorder - 400 Bad Request when list is empty (@Valid)")
    void shouldRejectEmptyReorderList() throws Exception {
        ExperienceReorderRequestDto invalidReorder = ExperienceReorderRequestDto.builder()
                .experienceIds(List.of())
                .build();

        mockMvc.perform(put("/portfolios/me/experiences/reorder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReorder)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
