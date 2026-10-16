package com.soodfolio.api.service;

import com.soodfolio.api.dto.AuthDtos;

public interface AuthService {
    AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request);
    AuthDtos.AuthResponse login(AuthDtos.LoginRequest request);
    AuthDtos.AuthResponse refreshToken(AuthDtos.RefreshTokenRequest request);
    void logout(String refreshToken);
    AuthDtos.UserDto getCurrentUser();

    // Forgot & Reset Password
    AuthDtos.MessageResponse forgotPassword(AuthDtos.ForgotPasswordRequest request);
    AuthDtos.VerifyResetTokenResponse verifyResetToken(String token);
    AuthDtos.MessageResponse resetPassword(AuthDtos.ResetPasswordRequest request);

    // OAuth 2.0 Social Login & Sandbox
    AuthDtos.AuthResponse processOAuthLogin(AuthDtos.OAuthLoginRequest request);
    AuthDtos.AuthResponse processOAuthSandbox(AuthDtos.OAuthSandboxRequest request);
    AuthDtos.AuthResponse exchangeOAuthCode(AuthDtos.OAuthExchangeRequest request);
}
