package com.soodfolio.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soodfolio.api.dto.PortfolioResponseDto.SkillDto;
import com.soodfolio.api.dto.SkillDtos.SkillReorderRequestDto;
import com.soodfolio.api.dto.SkillDtos.SkillRequestDto;
import com.soodfolio.api.exception.GlobalExceptionHandler;
import com.soodfolio.api.exception.ResourceNotFoundException;
import com.soodfolio.api.security.UserPrincipal;
import com.soodfolio.api.service.I18nService;
import com.soodfolio.api.service.SkillService;
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
class SkillControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SkillService skillService;

    @Mock
    private I18nService i18nService;

    @InjectMocks
    private SkillController skillController;

    private ObjectMapper objectMapper;
    private UserPrincipal testPrincipal;
    private UUID userId;
    private SkillDto sampleSkillDto;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        userId = UUID.randomUUID();
        testPrincipal = new UserPrincipal(
                userId,
                "skills@soodfolio.dev",
                "password",
                "Skills Engineer",
                org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_USER")
        );

        sampleSkillDto = SkillDto.builder()
                .id(UUID.randomUUID())
                .category("BACKEND")
                .name("Spring Boot")
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

        mockMvc = MockMvcBuilders.standaloneSetup(skillController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .setControllerAdvice(exceptionHandler)
                .build();

        lenient().when(i18nService.getMessage(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("GET /portfolios/me/skills - 200 OK when authenticated")
    void shouldReturnSkillsWhenAuthenticated() throws Exception {
        when(skillService.getSkillsByUserId(userId)).thenReturn(List.of(sampleSkillDto));

        mockMvc.perform(get("/portfolios/me/skills"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Spring Boot"))
                .andExpect(jsonPath("$[0].category").value("BACKEND"));
    }

    @Test
    @DisplayName("GET /portfolios/me/skills - 401 Unauthorized when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/portfolios/me/skills")
                        .header("X-Anonymous", "true"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /portfolios/me/skills - 201 Created on valid request")
    void shouldCreateSkillSuccessfully() throws Exception {
        SkillRequestDto request = SkillRequestDto.builder()
                .category("BACKEND")
                .name("PostgreSQL")
                .build();

        when(skillService.createSkill(eq(userId), any(SkillRequestDto.class))).thenReturn(sampleSkillDto);

        mockMvc.perform(post("/portfolios/me/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Spring Boot"));
    }

    @Test
    @DisplayName("POST /portfolios/me/skills - 400 Bad Request when category or name is blank (@Valid)")
    void shouldRejectInvalidSkillPayload() throws Exception {
        SkillRequestDto invalidRequest = SkillRequestDto.builder()
                .category("")
                .name("")
                .build();

        mockMvc.perform(post("/portfolios/me/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.category").exists())
                .andExpect(jsonPath("$.validationErrors.name").exists());
    }

    @Test
    @DisplayName("GET /portfolios/me/skills/{id} - 404 Not Found when skill does not exist")
    void shouldReturn404WhenNotFound() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(skillService.getSkillById(userId, unknownId))
                .thenThrow(new ResourceNotFoundException("Skill", "id", unknownId));

        mockMvc.perform(get("/portfolios/me/skills/" + unknownId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("DELETE /portfolios/me/skills/{id} - 204 No Content")
    void shouldDeleteSkillSuccessfully() throws Exception {
        UUID skillId = sampleSkillDto.getId();
        doNothing().when(skillService).deleteSkill(userId, skillId);

        mockMvc.perform(delete("/portfolios/me/skills/" + skillId))
                .andExpect(status().isNoContent());

        verify(skillService).deleteSkill(userId, skillId);
    }

    @Test
    @DisplayName("PUT /portfolios/me/skills/reorder - 400 Bad Request when list is empty (@Valid)")
    void shouldRejectEmptyReorderList() throws Exception {
        SkillReorderRequestDto invalidReorder = SkillReorderRequestDto.builder()
                .skillIds(List.of())
                .build();

        mockMvc.perform(put("/portfolios/me/skills/reorder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReorder)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
