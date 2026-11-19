package com.soodfolio.api.controller;

import com.soodfolio.api.dto.AuthDtos;
import com.soodfolio.api.exception.BadRequestException;
import com.soodfolio.api.security.AuthCookieHelper;
import com.soodfolio.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User Registration, Login, Token Refresh, Social OAuth & Password Reset")
public class AuthController {

    private final AuthService authService; // DIP: Injected via Interface
    private final AuthCookieHelper cookieHelper;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a user account and returns 201 Created with initial tokens")
    @ApiResponse(responseCode = "201", description = "User registered successfully")
    public ResponseEntity<AuthDtos.AuthResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        AuthDtos.AuthResponse response = authService.register(request);
        ResponseCookie cookie = cookieHelper.createRefreshTokenCookie(response.getRefreshToken());
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "User Login", description = "Authenticate with email OR username and password")
    @ApiResponse(responseCode = "200", description = "Login successful")
    public ResponseEntity<AuthDtos.AuthResponse> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        AuthDtos.AuthResponse response = authService.login(request);
        ResponseCookie cookie = cookieHelper.createRefreshTokenCookie(response.getRefreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh Access Token", description = "Submit a 7-day refresh token to obtain a fresh 30-minute access token")
    @ApiResponse(responseCode = "200", description = "Token refreshed successfully")
    public ResponseEntity<AuthDtos.AuthResponse> refreshToken(
            @CookieValue(name = AuthCookieHelper.REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshTokenCookie,
            @RequestBody(required = false) AuthDtos.RefreshTokenRequest request
    ) {
        String token = (refreshTokenCookie != null && !refreshTokenCookie.isBlank())
                ? refreshTokenCookie
                : (request != null ? request.getRefreshToken() : null);

        if (token == null || token.isBlank()) {
            throw new BadRequestException("Refresh token is required");
        }

        AuthDtos.AuthResponse response = authService.refreshToken(new AuthDtos.RefreshTokenRequest(token));
        ResponseCookie cookie = cookieHelper.createRefreshTokenCookie(response.getRefreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Revokes the active refresh token and returns 204 No Content")
    @ApiResponse(responseCode = "204", description = "Logged out successfully")
    public ResponseEntity<Void> logout(
            @CookieValue(name = AuthCookieHelper.REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshTokenCookie,
            @RequestBody(required = false) AuthDtos.RefreshTokenRequest request
    ) {
        String token = (refreshTokenCookie != null && !refreshTokenCookie.isBlank())
                ? refreshTokenCookie
                : (request != null ? request.getRefreshToken() : null);

        if (token != null && !token.isBlank()) {
            authService.logout(token);
        }

        ResponseCookie clearCookie = cookieHelper.createClearRefreshTokenCookie();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .build();
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

    @GetMapping("/oauth/state")
    @Operation(summary = "Generate CSRF state for OAuth flow", description = "Generates secure state token, sets HttpOnly cookie and returns state")
    public ResponseEntity<Map<String, String>> generateOAuthState() {
        String state = UUID.randomUUID().toString();
        ResponseCookie stateCookie = cookieHelper.createOAuthStateCookie(state);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, stateCookie.toString())
                .body(Map.of("state", state));
    }

    @PostMapping("/oauth/exchange")
    @Operation(summary = "Real OAuth 2.0 Code Exchange", description = "Exchanges authorization code with GitHub/Google for user profile and JWT")
    @ApiResponse(responseCode = "200", description = "OAuth authenticated successfully")
    public ResponseEntity<AuthDtos.AuthResponse> oauthExchange(
            @Valid @RequestBody AuthDtos.OAuthExchangeRequest request,
            @CookieValue(name = AuthCookieHelper.OAUTH_STATE_COOKIE_NAME, required = false) String cookieState
    ) {
        if (request.getState() == null || request.getState().isBlank() || cookieState == null || !request.getState().equals(cookieState)) {
            throw new BadRequestException("CSRF State không hợp lệ hoặc đã hết hạn.");
        }

        AuthDtos.AuthResponse response = authService.exchangeOAuthCode(request);
        ResponseCookie refreshCookie = cookieHelper.createRefreshTokenCookie(response.getRefreshToken());
        ResponseCookie clearStateCookie = cookieHelper.createClearOAuthStateCookie();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .header(HttpHeaders.SET_COOKIE, clearStateCookie.toString())
                .body(response);
    }
}
