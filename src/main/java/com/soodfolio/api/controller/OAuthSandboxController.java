package com.soodfolio.api.controller;

import com.soodfolio.api.dto.AuthDtos;
import com.soodfolio.api.security.AuthCookieHelper;
import com.soodfolio.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/oauth")
@Profile({"dev", "local"})
@ConditionalOnProperty(name = "app.oauth2.sandbox-enabled", havingValue = "true")
@RequiredArgsConstructor
@Tag(name = "OAuth Sandbox", description = "Mock OAuth authentication for development")
public class OAuthSandboxController {

    private final AuthService authService;
    private final AuthCookieHelper cookieHelper;

    @PostMapping("/sandbox")
    @Operation(summary = "OAuth Sandbox Mock Login", description = "Fast 1-click test login for GitHub and Google without cloud secrets")
    @ApiResponse(responseCode = "200", description = "Sandbox authenticated successfully")
    public ResponseEntity<AuthDtos.AuthResponse> oauthSandbox(@Valid @RequestBody AuthDtos.OAuthSandboxRequest request) {
        AuthDtos.AuthResponse response = authService.processOAuthSandbox(request);
        ResponseCookie refreshCookie = cookieHelper.createRefreshTokenCookie(response.getRefreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(response);
    }

    @PostMapping("/callback")
    @Operation(summary = "OAuth Sandbox Mock Callback", description = "Mock OAuth callback login for development")
    @ApiResponse(responseCode = "200", description = "Sandbox callback authenticated successfully")
    public ResponseEntity<AuthDtos.AuthResponse> oauthCallback(@Valid @RequestBody AuthDtos.OAuthLoginRequest request) {
        AuthDtos.AuthResponse response = authService.processOAuthLogin(request);
        ResponseCookie refreshCookie = cookieHelper.createRefreshTokenCookie(response.getRefreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(response);
    }
}
