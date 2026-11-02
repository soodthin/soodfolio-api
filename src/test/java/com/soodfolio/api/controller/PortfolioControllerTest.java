package com.soodfolio.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soodfolio.api.dto.AnalyticsDtos.ViewCountResponseDto;
import com.soodfolio.api.dto.PortfolioResponseDto;
import com.soodfolio.api.dto.UpdatePortfolioRequestDto;
import com.soodfolio.api.exception.GlobalExceptionHandler;
import com.soodfolio.api.exception.ResourceNotFoundException;
import com.soodfolio.api.security.UserPrincipal;
import com.soodfolio.api.service.I18nService;
import com.soodfolio.api.service.PortfolioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PortfolioControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PortfolioService portfolioService;

    @Mock
    private I18nService i18nService;

    @InjectMocks
    private PortfolioController portfolioController;

    private ObjectMapper objectMapper;
    private UserPrincipal testPrincipal;
    private UUID userId;
    private PortfolioResponseDto samplePortfolioDto;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        userId = UUID.randomUUID();
        testPrincipal = new UserPrincipal(
                userId,
                "creator@soodfolio.dev",
                "password",
                "Creator",
                org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_USER")
        );

        samplePortfolioDto = PortfolioResponseDto.builder()
                .id(UUID.randomUUID())
                .username("soodthin")
                .title("SOOD THIN")
                .tagline("FULLSTACK ENGINEER")
                .isPublished(true)
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

        mockMvc = MockMvcBuilders.standaloneSetup(portfolioController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .setControllerAdvice(exceptionHandler)
                .build();

        lenient().when(i18nService.getMessage(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("GET /portfolios/me - 200 OK when authenticated")
    void shouldReturnMyPortfolioWhenAuthenticated() throws Exception {
        when(portfolioService.getPortfolioByUserId(userId)).thenReturn(Optional.of(samplePortfolioDto));

        mockMvc.perform(get("/portfolios/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("soodthin"))
                .andExpect(jsonPath("$.title").value("SOOD THIN"));
    }

    @Test
    @DisplayName("GET /portfolios/me - 401 Unauthorized when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/portfolios/me")
                        .header("X-Anonymous", "true"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /portfolios/me - 200 OK on valid update request")
    void shouldUpdateMyPortfolioSuccessfully() throws Exception {
        UpdatePortfolioRequestDto updateRequest = UpdatePortfolioRequestDto.builder()
                .title("SOOD THIN (UPDATED)")
                .tagline("ARCHITECT & CREATOR")
                .build();

        when(portfolioService.updatePortfolio(eq(userId), any(UpdatePortfolioRequestDto.class)))
                .thenReturn(samplePortfolioDto);

        mockMvc.perform(put("/portfolios/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("soodthin"));
    }

    @Test
    @DisplayName("PATCH /portfolios/me - 200 OK with themeConfig in request payload")
    void shouldUpdateThemeConfigViaPatchSuccessfully() throws Exception {
        String themeConfigJson = "{\"preset\":\"spotlight-focus\"}";
        UpdatePortfolioRequestDto updateRequest = UpdatePortfolioRequestDto.builder()
                .title("SOOD THIN")
                .themeConfig(themeConfigJson)
                .build();

        PortfolioResponseDto updatedDto = PortfolioResponseDto.builder()
                .id(samplePortfolioDto.getId())
                .username("soodthin")
                .title("SOOD THIN")
                .tagline("FULLSTACK ENGINEER")
                .isPublished(true)
                .themeConfig(themeConfigJson)
                .build();

        when(portfolioService.updatePortfolio(eq(userId), any(UpdatePortfolioRequestDto.class)))
                .thenReturn(updatedDto);

        mockMvc.perform(patch("/portfolios/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("soodthin"))
                .andExpect(jsonPath("$.themeConfig").value(themeConfigJson));

        ArgumentCaptor<UpdatePortfolioRequestDto> captor = ArgumentCaptor.forClass(UpdatePortfolioRequestDto.class);
        verify(portfolioService).updatePortfolio(eq(userId), captor.capture());
        assertThat(captor.getValue().getThemeConfig()).isEqualTo(themeConfigJson);
    }

    @Test
    @DisplayName("PUT /portfolios/me - 200 OK with themeConfig verifying backward compatibility")
    void shouldUpdateThemeConfigViaPutBackwardCompatible() throws Exception {
        String themeConfigJson = "{\"preset\":\"editorial-clean\"}";
        UpdatePortfolioRequestDto updateRequest = UpdatePortfolioRequestDto.builder()
                .title("SOOD THIN (UPDATED)")
                .themeConfig(themeConfigJson)
                .build();

        PortfolioResponseDto updatedDto = PortfolioResponseDto.builder()
                .id(samplePortfolioDto.getId())
                .username("soodthin")
                .title("SOOD THIN (UPDATED)")
                .themeConfig(themeConfigJson)
                .build();

        when(portfolioService.updatePortfolio(eq(userId), any(UpdatePortfolioRequestDto.class)))
                .thenReturn(updatedDto);

        mockMvc.perform(put("/portfolios/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("SOOD THIN (UPDATED)"))
                .andExpect(jsonPath("$.themeConfig").value(themeConfigJson));
    }

    @Test
    @DisplayName("PATCH /portfolios/me/status - 200 OK to toggle isPublished")
    void shouldUpdateStatusSuccessfully() throws Exception {
        when(portfolioService.updatePublishedStatus(userId, false)).thenReturn(samplePortfolioDto);

        mockMvc.perform(patch("/portfolios/me/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("isPublished", false))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /portfolios/{username} - 200 OK for public view")
    void shouldReturnPublicPortfolio() throws Exception {

        when(portfolioService.getPortfolioByUsername("soodthin")).thenReturn(Optional.of(samplePortfolioDto));

        mockMvc.perform(get("/portfolios/soodthin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("soodthin"));
    }

    @Test
    @DisplayName("GET /portfolios/{username} - 404 Not Found when username does not exist")
    void shouldReturn404WhenUsernameNotFound() throws Exception {
        when(portfolioService.getPortfolioByUsername("unknown_user")).thenReturn(Optional.empty());

        mockMvc.perform(get("/portfolios/unknown_user"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST /portfolios/{username}/views - 201 Created on recording view impression")
    void shouldRecordViewImpression() throws Exception {
        ViewCountResponseDto viewResponse = ViewCountResponseDto.builder()
                .totalViews(42)
                .build();

        when(portfolioService.recordView(eq("soodthin"), any(), any())).thenReturn(viewResponse);

        mockMvc.perform(post("/portfolios/soodthin/views")
                        .header("User-Agent", "Mozilla/5.0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalViews").value(42));
    }

    @Test
    @DisplayName("PUT /portfolios/me - 409 Conflict when concurrent modification causes OptimisticLockingFailure")
    void shouldReturn409ConflictOnOptimisticLockingFailure() throws Exception {
        UpdatePortfolioRequestDto requestDto = UpdatePortfolioRequestDto.builder()
                .title("Concurrent Update")
                .build();

        when(portfolioService.updatePortfolio(eq(userId), any(UpdatePortfolioRequestDto.class)))
                .thenThrow(new org.springframework.orm.ObjectOptimisticLockingFailureException("Portfolio", userId));

        mockMvc.perform(put("/portfolios/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("common.concurrency_conflict"));
    }
}
