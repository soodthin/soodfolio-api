package com.soodfolio.api.controller;

import com.soodfolio.api.dto.ShareLinkDtos.PublicDeckResponse;
import com.soodfolio.api.dto.ShareLinkDtos.TelemetryBatchRequest;
import com.soodfolio.api.service.ShareLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/public/links")
@RequiredArgsConstructor
@Tag(name = "Public Deck Viewer", description = "Public-facing endpoints for recruiters viewing targeted portfolio pitch-decks")
public class PublicDeckController {

    private final ShareLinkService shareLinkService;

    @GetMapping("/{token}")
    @Operation(summary = "Access a targeted pitch-deck via secure share token")
    public ResponseEntity<PublicDeckResponse> getDeckByToken(@PathVariable String token) {
        PublicDeckResponse response = shareLinkService.getPublicDeck(token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{token}/telemetry")
    @Operation(summary = "Record slide dwell-time events batched by recruiter's browser (sendBeacon)")
    public ResponseEntity<Map<String, String>> recordTelemetry(
            @PathVariable String token,
            @Valid @RequestBody TelemetryBatchRequest request,
            HttpServletRequest servletRequest
    ) {
        String userAgent = servletRequest.getHeader(HttpHeaders.USER_AGENT);
        shareLinkService.recordTelemetry(token, request, userAgent);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
