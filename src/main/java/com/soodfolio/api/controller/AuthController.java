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
@Tag(name = "Authentication", description = "User Registration, Login, Token Refresh & Profile")
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
        return ResponseEntity.noContent().build(); // REST Rule 7: 204 No Content
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile", description = "Requires Authorization: Bearer <token>")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<AuthDtos.UserDto> getCurrentUser() {
        return ResponseEntity.ok(authService.getCurrentUser());
    }
}
