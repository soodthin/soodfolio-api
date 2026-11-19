package com.soodfolio.api.service.impl;

import com.soodfolio.api.entity.RefreshToken;
import com.soodfolio.api.entity.User;
import com.soodfolio.api.exception.InvalidTokenException;
import com.soodfolio.api.repository.RefreshTokenRepository;
import com.soodfolio.api.security.SecurityUtils;
import com.soodfolio.api.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        String rawToken = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
        String hashedToken = SecurityUtils.sha256(rawToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(hashedToken)
                .expiryDate(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();

        RefreshToken saved = refreshTokenRepository.save(refreshToken);
        saved.setRawToken(rawToken);
        return saved;
    }

    @Override
    @Transactional
    public RefreshToken verifyExpiration(String tokenString) {
        if (tokenString == null || tokenString.isBlank()) {
            throw new InvalidTokenException("Refresh token was not found in the system");
        }

        String hashedToken = SecurityUtils.sha256(tokenString.trim());
        RefreshToken refreshToken = refreshTokenRepository.findByToken(hashedToken)
                .orElseThrow(() -> new InvalidTokenException("Refresh token was not found in the system"));

        if (refreshToken.isRevoked()) {
            // SEC-03: Token Reuse Detection - An already revoked token is used, indicating a replay attack
            refreshTokenRepository.deleteByUser(refreshToken.getUser());
            throw new InvalidTokenException("Refresh token was expired or revoked. Please sign in again.");
        }

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new InvalidTokenException("Refresh token was expired or revoked. Please sign in again.");
        }

        refreshToken.setRawToken(tokenString.trim());
        return refreshToken;
    }

    @Override
    @Transactional
    public void revokeToken(String tokenString) {
        if (tokenString == null || tokenString.isBlank()) {
            return;
        }
        String hashedToken = SecurityUtils.sha256(tokenString.trim());
        refreshTokenRepository.findByToken(hashedToken).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }
}
