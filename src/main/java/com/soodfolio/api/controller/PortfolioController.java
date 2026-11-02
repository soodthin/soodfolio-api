package com.soodfolio.api.controller;

import com.soodfolio.api.dto.AnalyticsDtos.RecordViewRequestDto;
import com.soodfolio.api.dto.AnalyticsDtos.ViewCountResponseDto;
import com.soodfolio.api.dto.PortfolioResponseDto;
import com.soodfolio.api.dto.UpdatePortfolioRequestDto;
import com.soodfolio.api.exception.InvalidCredentialsException;
import com.soodfolio.api.exception.ResourceNotFoundException;
import com.soodfolio.api.security.UserPrincipal;
import com.soodfolio.api.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/portfolios")
@RequiredArgsConstructor
@Tag(name = "Portfolio", description = "Portfolio APIs for Creators and Public Viewers")
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user's portfolio", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<PortfolioResponseDto> getMyPortfolio(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new InvalidCredentialsException("Yêu cầu đăng nhập để truy cập thông tin portfolio.");
        }
        PortfolioResponseDto dto = portfolioService.getPortfolioByUserId(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "userId", userPrincipal.getId()));
        return ResponseEntity.ok(dto);
    }

    @RequestMapping(value = "/me", method = {RequestMethod.PUT, RequestMethod.PATCH})
    @Operation(summary = "Update current authenticated user's portfolio profile and settings", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<PortfolioResponseDto> updateMyPortfolio(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UpdatePortfolioRequestDto request
    ) {
        if (userPrincipal == null) {
            throw new InvalidCredentialsException("Yêu cầu đăng nhập để thực hiện thao tác này.");
        }
        PortfolioResponseDto dto = portfolioService.updatePortfolio(userPrincipal.getId(), request);
        return ResponseEntity.ok(dto);
    }

    @PatchMapping("/me/status")
    @Operation(summary = "Update published visibility status", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<PortfolioResponseDto> updateStatus(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody Map<String, Boolean> body
    ) {
        if (userPrincipal == null) {
            throw new InvalidCredentialsException("Yêu cầu đăng nhập để thực hiện thao tác này.");
        }
        boolean isPublished = body.getOrDefault("isPublished", true);
        PortfolioResponseDto dto = portfolioService.updatePublishedStatus(userPrincipal.getId(), isPublished);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{username}")

    @Operation(summary = "Get full portfolio by username", description = "Returns complete portfolio data for the public slide deck")
    public ResponseEntity<PortfolioResponseDto> getPortfolio(@PathVariable String username) {
        PortfolioResponseDto dto = portfolioService.getPortfolioByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "username", username));
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{username}/views")
    @Operation(summary = "Record view event for portfolio slide deck", description = "Public endpoint for viewers to track impressions")
    public ResponseEntity<ViewCountResponseDto> recordView(
            @PathVariable String username,
            @RequestBody(required = false) RecordViewRequestDto request,
            @RequestHeader(value = "User-Agent", required = false) String userAgent
    ) {
        ViewCountResponseDto response = portfolioService.recordView(username, request, userAgent);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
