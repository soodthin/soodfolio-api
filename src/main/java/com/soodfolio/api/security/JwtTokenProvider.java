package com.soodfolio.api.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:1800000}")
    private long jwtExpirationMs;

    @PostConstruct
    public void validateJwtSecret() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("JWT secret key must not be null or blank");
        }
        byte[] decodedKey;
        try {
            decodedKey = Decoders.BASE64.decode(jwtSecret);
        } catch (Exception e) {
            throw new IllegalStateException("JWT secret key must be a valid Base64-encoded string: " + e.getMessage(), e);
        }
        if (decodedKey.length < 32) {
            throw new IllegalStateException("JWT secret key must be at least 256 bits (32 bytes), but got " + (decodedKey.length * 8) + " bits (" + decodedKey.length + " bytes)");
        }
    }

    private Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public String generateAccessToken(UserPrincipal userPrincipal) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(userPrincipal.getId().toString())
                .claim("email", userPrincipal.getEmail())
                .claim("name", userPrincipal.getFullName())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key())
                .compact();
    }

    public UUID getUserIdFromJWT(String token) {
        Claims claims = Jwts.parser()
                .verifyWith((SecretKey) key())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return UUID.fromString(claims.getSubject());
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().verifyWith((SecretKey) key()).build().parseSignedClaims(authToken);
            return true;
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty.");
        }
        return false;
    }

    public long getExpirationMs() {
        return jwtExpirationMs;
    }
}
