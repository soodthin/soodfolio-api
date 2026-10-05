package com.soodfolio.api.controller;

import com.soodfolio.api.dto.AuthDtos;
import com.soodfolio.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User Registration, Login, Token Refresh & Profile")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a user account and automatically initializes their portfolio")
    public ResponseEntity<AuthDtos.AuthResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "User Login", description = "Authenticate with email OR username and password to receive JWT tokens")
    public ResponseEntity<AuthDtos.AuthResponse> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh Access Token", description = "Submit a valid 7-day refresh token to obtain a fresh 30-minute access token")
    public ResponseEntity<AuthDtos.AuthResponse> refreshToken(@Valid @RequestBody AuthDtos.RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Revokes the active refresh token")
    public ResponseEntity<AuthDtos.MessageResponse> logout(@Valid @RequestBody AuthDtos.RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(new AuthDtos.MessageResponse("Logged out successfully"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user", description = "Requires Authorization: Bearer <token>")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<AuthDtos.UserDto> getCurrentUser() {
        return ResponseEntity.ok(authService.getCurrentUser());
    }
}
