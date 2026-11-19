package com.soodfolio.api.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    // 256-bit base64 secret (32 bytes)
    private static final String TEST_SECRET = Base64.getEncoder().encodeToString(
            "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha-256".getBytes()
    );

    private static final long DEFAULT_EXPIRATION_MS = 900000L; // 15 minutes

    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", DEFAULT_EXPIRATION_MS);
        jwtTokenProvider.validateJwtSecret();

        userPrincipal = new UserPrincipal(
                UUID.randomUUID(),
                "security@soodfolio.dev",
                "Password123!",
                "Security Engineer",
                java.util.Collections.emptyList()
        );
    }

    @Test
    @DisplayName("Should read default 15-minute expiration (900000 ms) from configuration")
    void shouldReadDefaultExpirationFromConfiguration() {
        assertThat(jwtTokenProvider.getExpirationMs()).isEqualTo(900000L);
    }

    @Test
    @DisplayName("Should successfully validate key in @PostConstruct and reject null, blank or short secret")
    void shouldValidateJwtSecretCorrectly() {
        JwtTokenProvider provider = new JwtTokenProvider();

        // Null secret
        ReflectionTestUtils.setField(provider, "jwtSecret", null);
        assertThatThrownBy(provider::validateJwtSecret)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must not be null or blank");

        // Blank secret
        ReflectionTestUtils.setField(provider, "jwtSecret", "   ");
        assertThatThrownBy(provider::validateJwtSecret)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must not be null or blank");

        // Non-base64 secret
        ReflectionTestUtils.setField(provider, "jwtSecret", "not-a-valid-base64!@#$");
        assertThatThrownBy(provider::validateJwtSecret)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("valid Base64-encoded string");

        // Secret shorter than 256 bits (< 32 bytes)
        String shortSecret = Base64.getEncoder().encodeToString("too-short".getBytes());
        ReflectionTestUtils.setField(provider, "jwtSecret", shortSecret);
        assertThatThrownBy(provider::validateJwtSecret)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 256 bits (32 bytes)");
    }

    @Test
    @DisplayName("Should validate valid JWT token successfully")
    void shouldValidateValidTokenSuccessfully() {
        String token = jwtTokenProvider.generateAccessToken(userPrincipal);

        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUserIdFromJWT(token)).isEqualTo(userPrincipal.getId());
    }

    @Test
    @DisplayName("Should safely return false when token has fake/invalid signature (SignatureException)")
    void shouldReturnFalseOnInvalidSignature() {
        String token = jwtTokenProvider.generateAccessToken(userPrincipal);

        // Sign another token with a different secret
        String differentSecret = Base64.getEncoder().encodeToString(
                "another-completely-different-secret-key-that-is-at-least-256-bits!".getBytes()
        );
        String untrustedToken = Jwts.builder()
                .subject(userPrincipal.getId().toString())
                .claim("email", userPrincipal.getEmail())
                .claim("name", userPrincipal.getFullName())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 900000L))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(differentSecret)))
                .compact();

        assertThat(jwtTokenProvider.validateToken(untrustedToken)).isFalse();
    }

    @Test
    @DisplayName("Should safely return false when token is expired (ExpiredJwtException)")
    void shouldReturnFalseOnExpiredToken() {
        Date past = new Date(System.currentTimeMillis() - 10000); // 10 seconds ago
        Date pastExpiry = new Date(System.currentTimeMillis() - 1000); // 1 second ago

        String expiredToken = Jwts.builder()
                .subject(userPrincipal.getId().toString())
                .claim("email", userPrincipal.getEmail())
                .issuedAt(past)
                .expiration(pastExpiry)
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET)))
                .compact();

        assertThat(jwtTokenProvider.validateToken(expiredToken)).isFalse();
    }

    @Test
    @DisplayName("Should safely return false when token is malformed, corrupted, or empty")
    void shouldReturnFalseOnMalformedOrEmptyToken() {
        assertThat(jwtTokenProvider.validateToken(null)).isFalse();
        assertThat(jwtTokenProvider.validateToken("")).isFalse();
        assertThat(jwtTokenProvider.validateToken("   ")).isFalse();
        assertThat(jwtTokenProvider.validateToken("not-a-valid-jwt.token")).isFalse();
        assertThat(jwtTokenProvider.validateToken("header.invalidpayload.signature")).isFalse();
    }
}
