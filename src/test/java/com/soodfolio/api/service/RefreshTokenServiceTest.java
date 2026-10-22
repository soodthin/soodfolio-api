package com.soodfolio.api.service;

import com.soodfolio.api.entity.RefreshToken;
import com.soodfolio.api.entity.User;
import com.soodfolio.api.exception.InvalidTokenException;
import com.soodfolio.api.repository.RefreshTokenRepository;
import com.soodfolio.api.service.impl.RefreshTokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    private RefreshToken activeToken;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .email("engineer@soodfolio.dev")
                .fullName("Sood Thin")
                .role("ROLE_USER")
                .build();

        activeToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plus(7, ChronoUnit.DAYS))
                .revoked(false)
                .build();
    }

    @Test
    @DisplayName("Should successfully create a new 7-day refresh token for user")
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
        assertThat(created.getExpiryDate()).isAfter(Instant.now().plus(6, ChronoUnit.DAYS));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should verify valid non-expired, non-revoked refresh token")
    void shouldVerifyValidTokenSuccessfully() {
        when(refreshTokenRepository.findByToken(activeToken.getToken())).thenReturn(Optional.of(activeToken));

        RefreshToken verified = refreshTokenService.verifyExpiration(activeToken.getToken());

        assertThat(verified).isNotNull();
        assertThat(verified.getToken()).isEqualTo(activeToken.getToken());
        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should throw InvalidTokenException and delete token if it has expired")
    void shouldThrowAndCleanUpWhenTokenIsExpired() {
        RefreshToken expiredToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .token("expired-token-uuid")
                .expiryDate(Instant.now().minus(1, ChronoUnit.HOURS))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken("expired-token-uuid")).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> refreshTokenService.verifyExpiration("expired-token-uuid"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("expired or revoked");

        verify(refreshTokenRepository).delete(expiredToken);
    }

    @Test
    @DisplayName("Should throw InvalidTokenException and delete token if it was revoked")
    void shouldThrowAndCleanUpWhenTokenIsRevoked() {
        RefreshToken revokedToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .token("revoked-token-uuid")
                .expiryDate(Instant.now().plus(5, ChronoUnit.DAYS))
                .revoked(true)
                .build();

        when(refreshTokenRepository.findByToken("revoked-token-uuid")).thenReturn(Optional.of(revokedToken));

        assertThatThrownBy(() -> refreshTokenService.verifyExpiration("revoked-token-uuid"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("expired or revoked");

        verify(refreshTokenRepository).deleteByUser(user);
    }

    @Test
    @DisplayName("Should throw InvalidTokenException when token does not exist in repository")
    void shouldThrowWhenTokenNotFound() {
        when(refreshTokenRepository.findByToken("non-existent-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.verifyExpiration("non-existent-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Refresh token was not found");
    }

    @Test
    @DisplayName("Should mark token as revoked when revokeToken is called")
    void shouldRevokeTokenSuccessfully() {
        when(refreshTokenRepository.findByToken(activeToken.getToken())).thenReturn(Optional.of(activeToken));

        refreshTokenService.revokeToken(activeToken.getToken());

        assertThat(activeToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(activeToken);
    }
}
