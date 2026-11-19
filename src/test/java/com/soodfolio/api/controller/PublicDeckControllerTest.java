package com.soodfolio.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soodfolio.api.dto.PortfolioResponseDto;
import com.soodfolio.api.dto.ShareLinkDtos.PublicDeckResponse;
import com.soodfolio.api.dto.ShareLinkDtos.TelemetryBatchRequest;
import com.soodfolio.api.dto.ShareLinkDtos.TelemetryEventDto;
import com.soodfolio.api.exception.ForbiddenException;
import com.soodfolio.api.exception.GlobalExceptionHandler;
import com.soodfolio.api.exception.ResourceNotFoundException;
import com.soodfolio.api.exception.ShareLinkExpiredException;
import com.soodfolio.api.service.I18nService;
import com.soodfolio.api.service.ShareLinkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PublicDeckControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ShareLinkService shareLinkService;

    @Mock
    private I18nService i18nService;

    @InjectMocks
    private PublicDeckController publicDeckController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler(i18nService);

        mockMvc = MockMvcBuilders.standaloneSetup(publicDeckController)
                .setControllerAdvice(exceptionHandler)
                .build();

        lenient().when(i18nService.getMessage(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("GET /public/links/{token} - 200 OK returns deck response for recruiter")
    void shouldReturnPublicDeckSuccessfully() throws Exception {
        String token = "st_live_Shopee123";

        PublicDeckResponse deckResponse = PublicDeckResponse.builder()
                .companyName("Shopee")
                .customLayout("CINEMATIC_DECK")
                .snapshotVersion(1)
                .portfolio(PortfolioResponseDto.builder().title("Alex Portfolio").build())
                .build();

        when(shareLinkService.getPublicDeck(token)).thenReturn(deckResponse);

        mockMvc.perform(get("/public/links/" + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Shopee"))
                .andExpect(jsonPath("$.customLayout").value("CINEMATIC_DECK"))
                .andExpect(jsonPath("$.portfolio.title").value("Alex Portfolio"));
    }

    @Test
    @DisplayName("GET /public/links/{token} - 403 Forbidden when link is revoked")
    void shouldReturn403WhenLinkIsRevoked() throws Exception {
        String token = "st_live_RevokedToken";

        when(shareLinkService.getPublicDeck(token))
                .thenThrow(new ForbiddenException("Link truy cập đã bị thu hồi bởi ứng viên."));

        mockMvc.perform(get("/public/links/" + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /public/links/{token} - 410 Gone when link is expired")
    void shouldReturn410WhenLinkIsExpired() throws Exception {
        String token = "st_live_ExpiredToken";

        when(shareLinkService.getPublicDeck(token))
                .thenThrow(new ShareLinkExpiredException("Link ứng tuyển đã hết hạn truy cập."));

        mockMvc.perform(get("/public/links/" + token))
                .andExpect(status().isGone());
    }

    @Test
    @DisplayName("GET /public/links/{token} - 404 Not Found when token does not exist")
    void shouldReturn404WhenTokenDoesNotExist() throws Exception {
        String token = "st_live_UnknownToken";

        when(shareLinkService.getPublicDeck(token))
                .thenThrow(new ResourceNotFoundException("ShareLink", "token", "invalid"));

        mockMvc.perform(get("/public/links/" + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /public/links/{token}/telemetry - 200 OK records recruiter dwell time")
    void shouldRecordTelemetrySuccessfully() throws Exception {
        String token = "st_live_Shopee123";

        TelemetryBatchRequest request = TelemetryBatchRequest.builder()
                .sessionId("sess_test_123")
                .events(List.of(
                        TelemetryEventDto.builder()
                                .slideKey("hero")
                                .dwellSeconds(20)
                                .deviceType("DESKTOP")
                                .build()
                ))
                .build();

        doNothing().when(shareLinkService).recordTelemetry(eq(token), any(TelemetryBatchRequest.class), any());

        mockMvc.perform(post("/public/links/" + token + "/telemetry")
                        .header(HttpHeaders.USER_AGENT, "Mozilla/5.0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));

        verify(shareLinkService).recordTelemetry(eq(token), any(TelemetryBatchRequest.class), eq("Mozilla/5.0"));
    }

    @Test
    @DisplayName("POST /public/links/{token}/telemetry - 400 Bad Request when batch exceeds 50 events")
    void shouldReturn400WhenTelemetryBatchExceeds50Events() throws Exception {
        String token = "st_live_Shopee123";

        List<TelemetryEventDto> events = new ArrayList<>();
        for (int i = 0; i < 51; i++) {
            events.add(TelemetryEventDto.builder()
                    .slideKey("slide-" + i)
                    .dwellSeconds(5)
                    .build());
        }

        TelemetryBatchRequest request = TelemetryBatchRequest.builder()
                .sessionId("sess_too_many")
                .events(events)
                .build();

        mockMvc.perform(post("/public/links/" + token + "/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.events").value("Maximum 50 telemetry events allowed per batch"));

        verify(shareLinkService, never()).recordTelemetry(any(), any(), any());
    }
}
