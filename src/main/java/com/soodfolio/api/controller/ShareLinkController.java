package com.soodfolio.api.controller;

import com.soodfolio.api.dto.ShareLinkDtos.*;
import com.soodfolio.api.exception.InvalidCredentialsException;
import com.soodfolio.api.security.UserPrincipal;
import com.soodfolio.api.service.ShareLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/portfolios/me/links")
@RequiredArgsConstructor
@Tag(name = "Targeted Share Links", description = "Manage company-targeted share links, slide access controls, and telemetry analytics")
@SecurityRequirement(name = "bearerAuth")
public class ShareLinkController {

    private final ShareLinkService shareLinkService;

    @PostMapping
    @Operation(summary = "Create a targeted company share link with optional expiry and view limits")
    public ResponseEntity<ShareLinkResponse> createShareLink(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreateShareLinkRequest request
    ) {
        ensureAuthenticated(userPrincipal);
        ShareLinkResponse response = shareLinkService.createShareLink(userPrincipal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List all created targeted share links for the authenticated user")
    public ResponseEntity<List<ShareLinkSummaryResponse>> getShareLinks(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        ensureAuthenticated(userPrincipal);
        return ResponseEntity.ok(shareLinkService.getShareLinks(userPrincipal.getId()));
    }

    @DeleteMapping("/{linkId}")
    @Operation(summary = "Revoke access for a targeted share link immediately")
    public ResponseEntity<Map<String, String>> revokeShareLink(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID linkId
    ) {
        ensureAuthenticated(userPrincipal);
        shareLinkService.revokeShareLink(userPrincipal.getId(), linkId);
        return ResponseEntity.ok(Map.of("message", "Đã thu hồi link ứng tuyển thành công."));
    }

    @GetMapping("/{linkId}/analytics")
    @Operation(summary = "Get detailed dwell-time telemetry heatmap and device analytics for a targeted share link")
    public ResponseEntity<TelemetryAnalyticsResponse> getLinkAnalytics(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID linkId
    ) {
        ensureAuthenticated(userPrincipal);
        return ResponseEntity.ok(shareLinkService.getLinkAnalytics(userPrincipal.getId(), linkId));
    }

    private void ensureAuthenticated(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new InvalidCredentialsException("Yêu cầu đăng nhập để thực hiện thao tác này.");
        }
    }
}
