package com.soodfolio.api.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AuthCookieHelper {

    public static final String REFRESH_TOKEN_COOKIE_NAME = "soodfolio_refresh_token";
    public static final String OAUTH_STATE_COOKIE_NAME = "soodfolio_oauth_state";
    public static final String AUTH_COOKIE_PATH = "/api/v1/auth";

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken != null ? refreshToken : "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path(AUTH_COOKIE_PATH)
                .maxAge(Duration.ofDays(7))
                .sameSite("Lax")
                .build();
    }

    public ResponseCookie createClearRefreshTokenCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path(AUTH_COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .sameSite("Lax")
                .build();
    }

    public ResponseCookie createOAuthStateCookie(String state) {
        return ResponseCookie.from(OAUTH_STATE_COOKIE_NAME, state != null ? state : "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path(AUTH_COOKIE_PATH)
                .maxAge(Duration.ofSeconds(300))
                .sameSite("Lax")
                .build();
    }

    public ResponseCookie createClearOAuthStateCookie() {
        return ResponseCookie.from(OAUTH_STATE_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path(AUTH_COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .sameSite("Lax")
                .build();
    }
}
