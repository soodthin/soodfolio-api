package com.soodfolio.api.service;

import com.soodfolio.api.dto.AuthDtos;

public interface AuthService {
    AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request);
    AuthDtos.AuthResponse login(AuthDtos.LoginRequest request);
    AuthDtos.AuthResponse refreshToken(AuthDtos.RefreshTokenRequest request);
    void logout(String refreshToken);
    AuthDtos.UserDto getCurrentUser();
}
