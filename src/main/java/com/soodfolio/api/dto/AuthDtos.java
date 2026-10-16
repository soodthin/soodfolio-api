package com.soodfolio.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

public class AuthDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RegisterRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Username is required")
        @Pattern(regexp = "^[a-z0-9_-]{3,30}$", message = "Username must be 3-30 lowercase characters (letters, numbers, hyphen, underscore)")
        private String username;

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;

        @NotBlank(message = "Full name is required")
        private String fullName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank(message = "Email or Username is required")
        private String usernameOrEmail;

        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefreshTokenRequest {
        @NotBlank(message = "Refresh token is required")
        private String refreshToken;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForgotPasswordRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResetPasswordRequest {
        @NotBlank(message = "Reset token is required")
        private String token;

        @NotBlank(message = "New password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String newPassword;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OAuthLoginRequest {
        @NotBlank(message = "Provider is required (GITHUB or GOOGLE)")
        private String provider;

        private String code;
        private String idToken;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OAuthSandboxRequest {
        @NotBlank(message = "Provider is required (GITHUB or GOOGLE)")
        private String provider;

        private String email;
        private String fullName;
        private String username;
        private String avatarUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OAuthExchangeRequest {
        @NotBlank(message = "Provider is required (GITHUB or GOOGLE)")
        private String provider;

        @NotBlank(message = "Authorization code is required")
        private String code;

        private String redirectUri;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class VerifyResetTokenResponse {
        private boolean valid;
        private String email;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private long expiresInMs;
        private UserDto user;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class UserDto {
        private UUID id;
        private String email;
        private String fullName;
        private String username;
        private String avatarUrl;
        private String role;
        private String authProvider;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MessageResponse {
        private String message;
    }
}
