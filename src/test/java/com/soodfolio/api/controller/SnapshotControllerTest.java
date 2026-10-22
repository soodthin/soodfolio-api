package com.soodfolio.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soodfolio.api.dto.PortfolioResponseDto;
import com.soodfolio.api.dto.SnapshotDtos.CreateSnapshotRequest;
import com.soodfolio.api.dto.SnapshotDtos.RollbackResponseDto;
import com.soodfolio.api.dto.SnapshotDtos.SnapshotResponseDto;
import com.soodfolio.api.dto.SnapshotDtos.SnapshotSummaryDto;
import com.soodfolio.api.exception.GlobalExceptionHandler;
import com.soodfolio.api.security.UserPrincipal;
import com.soodfolio.api.service.I18nService;
import com.soodfolio.api.service.SnapshotService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SnapshotControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SnapshotService snapshotService;

    @Mock
    private I18nService i18nService;

    @InjectMocks
    private SnapshotController snapshotController;

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

        mockMvc = MockMvcBuilders.standaloneSetup(snapshotController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .setControllerAdvice(exceptionHandler)
                .build();

        lenient().when(i18nService.getMessage(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("POST /portfolios/me/snapshots/publish - 201 Created on valid publish")
    void shouldPublishSnapshotSuccessfully() throws Exception {
        CreateSnapshotRequest request = CreateSnapshotRequest.builder()
                .changelog("Added experiences")
                .build();

        SnapshotResponseDto responseDto = SnapshotResponseDto.builder()
                .id(UUID.randomUUID())
                .portfolioId(UUID.randomUUID())
                .version(1)
                .changelog("Added experiences")
                .createdAt(LocalDateTime.now())
                .build();

        when(snapshotService.publishSnapshot(eq(userId), eq("Added experiences"))).thenReturn(responseDto);

        mockMvc.perform(post("/portfolios/me/snapshots/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.changelog").value("Added experiences"));
    }

    @Test
    @DisplayName("POST /portfolios/me/snapshots/publish - 401 Unauthorized when not logged in")
    void shouldReturn401WhenPublishingUnauthenticated() throws Exception {
        mockMvc.perform(post("/portfolios/me/snapshots/publish")
                        .header("X-Anonymous", "true"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /portfolios/me/snapshots - 200 OK returns snapshot list")
    void shouldReturnSnapshotsListWhenAuthenticated() throws Exception {
        SnapshotSummaryDto s1 = SnapshotSummaryDto.builder()
                .id(UUID.randomUUID())
                .version(2)
                .changelog("v2")
                .createdAt(LocalDateTime.now())
                .build();

        when(snapshotService.getSnapshots(userId)).thenReturn(List.of(s1));

        mockMvc.perform(get("/portfolios/me/snapshots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].version").value(2))
                .andExpect(jsonPath("$[0].changelog").value("v2"));
    }

    @Test
    @DisplayName("GET /portfolios/me/snapshots/{version} - 200 OK returns snapshot detail")
    void shouldReturnSingleSnapshotDetail() throws Exception {
        SnapshotResponseDto dto = SnapshotResponseDto.builder()
                .id(UUID.randomUUID())
                .version(1)
                .changelog("Initial")
                .data(PortfolioResponseDto.builder().title("Alex").build())
                .build();

        when(snapshotService.getSnapshot(userId, 1)).thenReturn(dto);

        mockMvc.perform(get("/portfolios/me/snapshots/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.data.title").value("Alex"));
    }

    @Test
    @DisplayName("POST /portfolios/me/snapshots/{version}/rollback - 200 OK restores version")
    void shouldRollbackSuccessfully() throws Exception {
        RollbackResponseDto rollbackResponse = RollbackResponseDto.builder()
                .message("Khôi phục thành công về phiên bản 1")
                .restoredVersion(1)
                .portfolio(PortfolioResponseDto.builder().title("Alex Restored").build())
                .build();

        when(snapshotService.rollbackToSnapshot(userId, 1)).thenReturn(rollbackResponse);

        mockMvc.perform(post("/portfolios/me/snapshots/1/rollback"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restoredVersion").value(1))
                .andExpect(jsonPath("$.portfolio.title").value("Alex Restored"));
    }
}
