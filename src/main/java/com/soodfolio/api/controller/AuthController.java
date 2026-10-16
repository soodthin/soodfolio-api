package com.soodfolio.api.controller;

import com.soodfolio.api.dto.AuthDtos;
import com.soodfolio.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User Registration, Login, Token Refresh, Social OAuth & Password Reset")
public class AuthController {

    private final AuthService authService; // DIP: Injected via Interface

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a user account and returns 201 Created with initial tokens")
    @ApiResponse(responseCode = "201", description = "User registered successfully")
    public ResponseEntity<AuthDtos.AuthResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "User Login", description = "Authenticate with email OR username and password")
    @ApiResponse(responseCode = "200", description = "Login successful")
    public ResponseEntity<AuthDtos.AuthResponse> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh Access Token", description = "Submit a 7-day refresh token to obtain a fresh 30-minute access token")
    @ApiResponse(responseCode = "200", description = "Token refreshed successfully")
    public ResponseEntity<AuthDtos.AuthResponse> refreshToken(@Valid @RequestBody AuthDtos.RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Revokes the active refresh token and returns 204 No Content")
    @ApiResponse(responseCode = "204", description = "Logged out successfully")
    public ResponseEntity<Void> logout(@Valid @RequestBody AuthDtos.RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile", description = "Requires Authorization: Bearer <token>")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<AuthDtos.UserDto> getCurrentUser() {
        return ResponseEntity.ok(authService.getCurrentUser());
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset", description = "Generates a password reset token and sends link/logs to console")
    @ApiResponse(responseCode = "200", description = "Password reset link generated")
    public ResponseEntity<AuthDtos.MessageResponse> forgotPassword(@Valid @RequestBody AuthDtos.ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @GetMapping("/verify-reset-token")
    @Operation(summary = "Verify reset token validity", description = "Checks if password reset token is active and unexpired")
    public ResponseEntity<AuthDtos.VerifyResetTokenResponse> verifyResetToken(@RequestParam String token) {
        return ResponseEntity.ok(authService.verifyResetToken(token));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset user password", description = "Applies new password using valid reset token")
    @ApiResponse(responseCode = "200", description = "Password reset successfully")
    public ResponseEntity<AuthDtos.MessageResponse> resetPassword(@Valid @RequestBody AuthDtos.ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @PostMapping("/oauth/sandbox")
    @Operation(summary = "OAuth Sandbox Mock Login", description = "Fast 1-click test login for GitHub and Google without cloud secrets")
    @ApiResponse(responseCode = "200", description = "Sandbox authenticated successfully")
    public ResponseEntity<AuthDtos.AuthResponse> oauthSandbox(@Valid @RequestBody AuthDtos.OAuthSandboxRequest request) {
        return ResponseEntity.ok(authService.processOAuthSandbox(request));
    }

    @PostMapping("/oauth/callback")
    @Operation(summary = "OAuth 2.0 Token Exchange", description = "Processes authorization code from GitHub or Google")
    @ApiResponse(responseCode = "200", description = "OAuth authenticated successfully")
    public ResponseEntity<AuthDtos.AuthResponse> oauthCallback(@Valid @RequestBody AuthDtos.OAuthLoginRequest request) {
        return ResponseEntity.ok(authService.processOAuthLogin(request));
    }

    @PostMapping("/oauth/exchange")
    @Operation(summary = "Real OAuth 2.0 Code Exchange", description = "Exchanges authorization code with GitHub/Google for user profile and JWT")
    @ApiResponse(responseCode = "200", description = "OAuth authenticated successfully")
    public ResponseEntity<AuthDtos.AuthResponse> oauthExchange(@Valid @RequestBody AuthDtos.OAuthExchangeRequest request) {
        return ResponseEntity.ok(authService.exchangeOAuthCode(request));
    }
}
