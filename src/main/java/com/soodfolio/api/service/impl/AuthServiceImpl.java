package com.soodfolio.api.service.impl;

import com.soodfolio.api.dto.AuthDtos;
import com.soodfolio.api.entity.Portfolio;
import com.soodfolio.api.entity.RefreshToken;
import com.soodfolio.api.entity.User;
import com.soodfolio.api.exception.DuplicateResourceException;
import com.soodfolio.api.exception.InvalidCredentialsException;
import com.soodfolio.api.repository.PortfolioRepository;
import com.soodfolio.api.repository.UserRepository;
import com.soodfolio.api.security.JwtTokenProvider;
import com.soodfolio.api.security.UserPrincipal;
import com.soodfolio.api.service.AuthService;
import com.soodfolio.api.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final RefreshTokenService refreshTokenService; // DIP: Injected via Interface
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @Override
    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        String cleanUsername = request.getUsername().toLowerCase().trim();

        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already registered!");
        }

        if (portfolioRepository.existsByUsername(cleanUsername)) {
            throw new DuplicateResourceException("Username '" + cleanUsername + "' is already taken!");
        }

        // 1. Create User
        User user = User.builder()
                .email(request.getEmail().trim().toLowerCase())
                .fullName(request.getFullName().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role("ROLE_USER")
                .authProvider("LOCAL")
                .build();
        userRepository.save(user);

        // 2. Automatically create initial Portfolio for this user
        Portfolio portfolio = Portfolio.builder()
                .user(user)
                .username(cleanUsername)
                .title(user.getFullName().toUpperCase())
                .tagline("DEVELOPER & CREATOR")
                .bio("Welcome to my interactive pitch-deck portfolio.")
                .statusText("AVAILABLE FOR WORK")
                .themeMode("DARK")
                .build();
        portfolioRepository.save(portfolio);

        // 3. Authenticate & generate tokens
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        String accessToken = tokenProvider.generateAccessToken(userPrincipal);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthDtos.AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresInMs(tokenProvider.getExpirationMs())
                .user(buildUserDto(user, cleanUsername))
                .build();
    }

    @Override
    @Transactional
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail().trim(), request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            User user = userRepository.findById(userPrincipal.getId())
                    .orElseThrow(() -> new InvalidCredentialsException("User account not found"));

            String username = portfolioRepository.findByUserId(user.getId())
                    .map(Portfolio::getUsername)
                    .orElse("user");

            String accessToken = tokenProvider.generateAccessToken(userPrincipal);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

            return AuthDtos.AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresInMs(tokenProvider.getExpirationMs())
                .user(buildUserDto(user, username))
                .build();
        } catch (BadCredentialsException ex) {
            throw new InvalidCredentialsException("Invalid email, username, or password");
        }
    }

    @Override
    @Transactional
    public AuthDtos.AuthResponse refreshToken(AuthDtos.RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.verifyExpiration(request.getRefreshToken());
        User user = refreshToken.getUser();

        UserPrincipal userPrincipal = UserPrincipal.create(user);
        String newAccessToken = tokenProvider.generateAccessToken(userPrincipal);

        String username = portfolioRepository.findByUserId(user.getId())
                .map(Portfolio::getUsername)
                .orElse("user");

        return AuthDtos.AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresInMs(tokenProvider.getExpirationMs())
                .user(buildUserDto(user, username))
                .build();
    }

    @Override
    @Transactional
    public void logout(String refreshTokenString) {
        refreshTokenService.revokeToken(refreshTokenString);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthDtos.UserDto getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            throw new InvalidCredentialsException("User is not authenticated");
        }

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        String username = portfolioRepository.findByUserId(user.getId())
                .map(Portfolio::getUsername)
                .orElse("user");

        return buildUserDto(user, username);
    }

    private AuthDtos.UserDto buildUserDto(User user, String username) {
        return AuthDtos.UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .username(username)
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .build();
    }
}
