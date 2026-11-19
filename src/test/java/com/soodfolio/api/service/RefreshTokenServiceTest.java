package com.soodfolio.api.service;

import com.soodfolio.api.entity.RefreshToken;
import com.soodfolio.api.entity.User;
import com.soodfolio.api.exception.InvalidTokenException;
import com.soodfolio.api.repository.RefreshTokenRepository;
import com.soodfolio.api.security.SecurityUtils;
import com.soodfolio.api.service.impl.RefreshTokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    private User user;
    private String rawToken;
    private String hashedToken;
    private RefreshToken activeToken;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .email("engineer@soodfolio.dev")
                .fullName("Sood Thin")
                .role("ROLE_USER")
                .build();

        rawToken = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
        hashedToken = SecurityUtils.sha256(rawToken);

        activeToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .token(hashedToken)
                .rawToken(rawToken)
                .expiryDate(Instant.now().plus(7, ChronoUnit.DAYS))
                .revoked(false)
                .build();
    }

    @Test
    @DisplayName("Should successfully create a new 7-day refresh token with SHA-256 hash for user")
    void shouldCreateRefreshTokenSuccessfully() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            token.setId(UUID.randomUUID());
            return token;
        });

        RefreshToken created = refreshTokenService.createRefreshToken(user);

        assertThat(created).isNotNull();
        assertThat(created.getUser()).isEqualTo(user);
        assertThat(created.getToken()).isNotBlank();
        assertThat(created.getToken()).hasSize(64); // SHA-256 hex length
        assertThat(created.getRawToken()).isNotBlank();
        assertThat(created.getToken()).isEqualTo(SecurityUtils.sha256(created.getRawToken()));
        assertThat(created.getToken()).isNotEqualTo(created.getRawToken());
        assertThat(created.getExpiryDate()).isAfter(Instant.now().plus(6, ChronoUnit.DAYS));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should verify valid non-expired, non-revoked refresh token via SHA-256 lookup")
    void shouldVerifyValidTokenSuccessfully() {
        when(refreshTokenRepository.findByToken(hashedToken)).thenReturn(Optional.of(activeToken));

        RefreshToken verified = refreshTokenService.verifyExpiration(rawToken);

        assertThat(verified).isNotNull();
        assertThat(verified.getToken()).isEqualTo(hashedToken);
        assertThat(verified.getRawToken()).isEqualTo(rawToken);
        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should throw InvalidTokenException and delete token if it has expired")
    void shouldThrowAndCleanUpWhenTokenIsExpired() {
        String expiredRawToken = "expired-token-raw";
        String expiredHashedToken = SecurityUtils.sha256(expiredRawToken);

        RefreshToken expiredToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .token(expiredHashedToken)
                .expiryDate(Instant.now().minus(1, ChronoUnit.HOURS))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken(expiredHashedToken)).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> refreshTokenService.verifyExpiration(expiredRawToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("expired or revoked");

        verify(refreshTokenRepository).delete(expiredToken);
    }

    @Test
    @DisplayName("Should throw InvalidTokenException and delete all user tokens if token was revoked (reuse detection)")
    void shouldThrowAndCleanUpWhenTokenIsRevoked() {
        String revokedRawToken = "revoked-token-raw";
        String revokedHashedToken = SecurityUtils.sha256(revokedRawToken);

        RefreshToken revokedToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .token(revokedHashedToken)
                .expiryDate(Instant.now().plus(5, ChronoUnit.DAYS))
                .revoked(true)
                .build();

        when(refreshTokenRepository.findByToken(revokedHashedToken)).thenReturn(Optional.of(revokedToken));

        assertThatThrownBy(() -> refreshTokenService.verifyExpiration(revokedRawToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("expired or revoked");

        verify(refreshTokenRepository).deleteByUser(user);
    }

    @Test
    @DisplayName("Should throw InvalidTokenException when token does not exist in repository")
    void shouldThrowWhenTokenNotFound() {
        String unknownRawToken = "non-existent-token";
        String unknownHashedToken = SecurityUtils.sha256(unknownRawToken);

        when(refreshTokenRepository.findByToken(unknownHashedToken)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.verifyExpiration(unknownRawToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Refresh token was not found");
    }

    @Test
    @DisplayName("Should mark token as revoked when revokeToken is called with raw token")
    void shouldRevokeTokenSuccessfully() {
        when(refreshTokenRepository.findByToken(hashedToken)).thenReturn(Optional.of(activeToken));

        refreshTokenService.revokeToken(rawToken);

        assertThat(activeToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(activeToken);
    }
}
