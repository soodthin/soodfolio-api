package com.soodfolio.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soodfolio.api.dto.EducationDtos.EducationReorderRequestDto;
import com.soodfolio.api.dto.EducationDtos.EducationRequestDto;
import com.soodfolio.api.dto.PortfolioResponseDto.EducationDto;
import com.soodfolio.api.exception.GlobalExceptionHandler;
import com.soodfolio.api.exception.ResourceNotFoundException;
import com.soodfolio.api.security.UserPrincipal;
import com.soodfolio.api.service.EducationService;
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
class EducationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EducationService educationService;

    @Mock
    private I18nService i18nService;

    @InjectMocks
    private EducationController educationController;

    private ObjectMapper objectMapper;
    private UserPrincipal testPrincipal;
    private UUID userId;
    private EducationDto sampleEducationDto;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        userId = UUID.randomUUID();
        testPrincipal = new UserPrincipal(
                userId,
                "student@soodfolio.dev",
                "password",
                "Student",
                org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_USER")
        );

        sampleEducationDto = EducationDto.builder()
                .id(UUID.randomUUID())
                .institution("HCM Open University")
                .degree("Bachelor of Science")
                .fieldOfStudy("Computer Science")
                .periodText("2022 — Present")
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

        mockMvc = MockMvcBuilders.standaloneSetup(educationController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .setControllerAdvice(exceptionHandler)
                .build();

        lenient().when(i18nService.getMessage(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("GET /portfolios/me/educations - 200 OK when authenticated")
    void shouldReturnEducationsWhenAuthenticated() throws Exception {
        when(educationService.getEducationsByUserId(userId)).thenReturn(List.of(sampleEducationDto));

        mockMvc.perform(get("/portfolios/me/educations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].institution").value("HCM Open University"))
                .andExpect(jsonPath("$[0].degree").value("Bachelor of Science"));
    }

    @Test
    @DisplayName("GET /portfolios/me/educations - 401 Unauthorized when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/portfolios/me/educations")
                        .header("X-Anonymous", "true"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /portfolios/me/educations - 201 Created on valid request")
    void shouldCreateEducationSuccessfully() throws Exception {
        EducationRequestDto request = EducationRequestDto.builder()
                .institution("MIT")
                .degree("Master")
                .periodText("2026 — 2028")
                .build();

        when(educationService.createEducation(eq(userId), any(EducationRequestDto.class))).thenReturn(sampleEducationDto);

        mockMvc.perform(post("/portfolios/me/educations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.institution").value("HCM Open University"));
    }

    @Test
    @DisplayName("POST /portfolios/me/educations - 400 Bad Request when required fields are missing (@Valid)")
    void shouldRejectInvalidEducationPayload() throws Exception {
        EducationRequestDto invalidRequest = EducationRequestDto.builder()
                .institution("") // Blank violates @NotBlank
                .degree("")
                .periodText("")
                .build();

        mockMvc.perform(post("/portfolios/me/educations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.institution").exists());
    }

    @Test
    @DisplayName("GET /portfolios/me/educations/{id} - 404 Not Found when education does not exist")
    void shouldReturn404WhenNotFound() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(educationService.getEducationById(userId, unknownId))
                .thenThrow(new ResourceNotFoundException("Education", "id", unknownId));

        mockMvc.perform(get("/portfolios/me/educations/" + unknownId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("DELETE /portfolios/me/educations/{id} - 204 No Content")
    void shouldDeleteEducationSuccessfully() throws Exception {
        UUID eduId = sampleEducationDto.getId();
        doNothing().when(educationService).deleteEducation(userId, eduId);

        mockMvc.perform(delete("/portfolios/me/educations/" + eduId))
                .andExpect(status().isNoContent());

        verify(educationService).deleteEducation(userId, eduId);
    }

    @Test
    @DisplayName("PUT /portfolios/me/educations/reorder - 400 Bad Request when educationIds list is empty (@Valid)")
    void shouldRejectEmptyReorderList() throws Exception {
        EducationReorderRequestDto invalidReorder = EducationReorderRequestDto.builder()
                .educationIds(List.of())
                .build();

        mockMvc.perform(put("/portfolios/me/educations/reorder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReorder)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
