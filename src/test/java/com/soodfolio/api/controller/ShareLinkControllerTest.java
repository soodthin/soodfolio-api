package com.soodfolio.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soodfolio.api.dto.ShareLinkDtos.*;
import com.soodfolio.api.exception.GlobalExceptionHandler;
import com.soodfolio.api.security.UserPrincipal;
import com.soodfolio.api.service.I18nService;
import com.soodfolio.api.service.ShareLinkService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ShareLinkControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ShareLinkService shareLinkService;

    @Mock
    private I18nService i18nService;

    @InjectMocks
    private ShareLinkController shareLinkController;

    private ObjectMapper objectMapper;
    private UserPrincipal testPrincipal;
    private UUID userId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        userId = UUID.randomUUID();
        testPrincipal = new UserPrincipal(
                userId,
                "alex@soodfolio.dev",
                "password",
                "Alex Nguyen",
                org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_USER")
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

        mockMvc = MockMvcBuilders.standaloneSetup(shareLinkController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .setControllerAdvice(exceptionHandler)
                .build();

        lenient().when(i18nService.getMessage(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("POST /portfolios/me/links - 201 Created on valid request")
    void shouldCreateShareLinkSuccessfully() throws Exception {
        CreateShareLinkRequest request = CreateShareLinkRequest.builder()
                .companyName("Shopee")
                .customLayout("CINEMATIC_DECK")
                .build();

        ShareLinkResponse response = ShareLinkResponse.builder()
                .id(UUID.randomUUID())
                .companyName("Shopee")
                .rawToken("st_live_Shopee123")
                .shareUrl("/p/st_live_Shopee123")
                .viewCount(0)
                .isRevoked(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(shareLinkService.createShareLink(eq(userId), any(CreateShareLinkRequest.class))).thenReturn(response);

        mockMvc.perform(post("/portfolios/me/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.companyName").value("Shopee"))
                .andExpect(jsonPath("$.rawToken").value("st_live_Shopee123"))
                .andExpect(jsonPath("$.shareUrl").value("/p/st_live_Shopee123"));
    }

    @Test
    @DisplayName("POST /portfolios/me/links - 401 Unauthorized when unauthenticated")
    void shouldReturn401WhenCreatingLinkUnauthenticated() throws Exception {
        CreateShareLinkRequest request = CreateShareLinkRequest.builder()
                .companyName("Shopee")
                .build();

        mockMvc.perform(post("/portfolios/me/links")
                        .header("X-Anonymous", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /portfolios/me/links - 200 OK returns creator's link list")
    void shouldReturnShareLinksList() throws Exception {
        ShareLinkSummaryResponse linkSummary = ShareLinkSummaryResponse.builder()
                .id(UUID.randomUUID())
                .companyName("Grab")
                .viewCount(2)
                .isRevoked(false)
                .build();

        when(shareLinkService.getShareLinks(userId)).thenReturn(List.of(linkSummary));

        mockMvc.perform(get("/portfolios/me/links"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].companyName").value("Grab"))
                .andExpect(jsonPath("$[0].viewCount").value(2));
    }

    @Test
    @DisplayName("DELETE /portfolios/me/links/{linkId} - 200 OK revokes link")
    void shouldRevokeShareLinkSuccessfully() throws Exception {
        UUID linkId = UUID.randomUUID();
        doNothing().when(shareLinkService).revokeShareLink(userId, linkId);

        mockMvc.perform(delete("/portfolios/me/links/" + linkId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đã thu hồi link ứng tuyển thành công."));

        verify(shareLinkService).revokeShareLink(userId, linkId);
    }

    @Test
    @DisplayName("GET /portfolios/me/links/{linkId}/analytics - 200 OK returns heatmap telemetry")
    void shouldReturnLinkAnalyticsSuccessfully() throws Exception {
        UUID linkId = UUID.randomUUID();
        TelemetryAnalyticsResponse analytics = TelemetryAnalyticsResponse.builder()
                .shareLinkId(linkId)
                .companyName("Shopee")
                .totalViews(10)
                .totalDwellSeconds(120)
                .slideStats(List.of(
                        SlideTelemetryStatDto.builder()
                                .slideKey("hero")
                                .totalDwellSeconds(60)
                                .viewCount(2)
                                .avgDwellSeconds(30.0)
                                .build()
                ))
                .build();

        when(shareLinkService.getLinkAnalytics(userId, linkId)).thenReturn(analytics);

        mockMvc.perform(get("/portfolios/me/links/" + linkId + "/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Shopee"))
                .andExpect(jsonPath("$.totalDwellSeconds").value(120))
                .andExpect(jsonPath("$.slideStats[0].slideKey").value("hero"));
    }
}
