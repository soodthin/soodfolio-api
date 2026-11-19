package com.soodfolio.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soodfolio.api.dto.AuthDtos;
import com.soodfolio.api.exception.GlobalExceptionHandler;
import com.soodfolio.api.security.AuthCookieHelper;
import com.soodfolio.api.service.AuthService;
import com.soodfolio.api.service.I18nService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @Spy
    private AuthCookieHelper cookieHelper = new AuthCookieHelper();

    @Mock
    private I18nService i18nService;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler(i18nService);

        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(exceptionHandler)
                .build();

        lenient().when(i18nService.getMessage(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("POST /auth/register - 201 Created and sets HttpOnly refresh cookie")
    void shouldRegisterUserSuccessfully() throws Exception {
        AuthDtos.RegisterRequest request = AuthDtos.RegisterRequest.builder()
                .email("test@soodfolio.dev")
                .username("sooduser")
                .fullName("Sood User")
                .password("Password123!")
                .build();

        AuthDtos.AuthResponse response = AuthDtos.AuthResponse.builder()
                .accessToken("mock-access-token")
                .refreshToken("mock-refresh-token")
                .tokenType("Bearer")
                .build();

        when(authService.register(any(AuthDtos.RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("mock-access-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("soodfolio_refresh_token=mock-refresh-token")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/v1/auth")));
    }

    @Test
    @DisplayName("POST /auth/register - 400 Bad Request when email is invalid or password too short (@Valid)")
    void shouldRejectInvalidRegistration() throws Exception {
        AuthDtos.RegisterRequest invalidRequest = AuthDtos.RegisterRequest.builder()
                .email("not-an-email")
                .username("INVALID_CAPS")
                .fullName("")
                .password("123")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.email").exists())
                .andExpect(jsonPath("$.validationErrors.password").exists());
    }

    @Test
    @DisplayName("POST /auth/login - 200 OK and sets HttpOnly refresh cookie")
    void shouldLoginSuccessfully() throws Exception {
        AuthDtos.LoginRequest request = new AuthDtos.LoginRequest("test@soodfolio.dev", "Password123!");
        AuthDtos.AuthResponse response = AuthDtos.AuthResponse.builder()
                .accessToken("mock-login-token")
                .refreshToken("mock-login-refresh-token")
                .tokenType("Bearer")
                .build();

        when(authService.login(any(AuthDtos.LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock-login-token"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("soodfolio_refresh_token=mock-login-refresh-token")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")));
    }

    @Test
    @DisplayName("POST /auth/login - 400 Bad Request when missing fields")
    void shouldRejectBlankLoginFields() throws Exception {
        AuthDtos.LoginRequest invalid = new AuthDtos.LoginRequest("", "");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.usernameOrEmail").exists())
                .andExpect(jsonPath("$.validationErrors.password").exists());
    }

    @Test
    @DisplayName("POST /auth/refresh - 200 OK on token refresh from request body")
    void shouldRefreshTokenFromBodySuccessfully() throws Exception {
        AuthDtos.RefreshTokenRequest request = new AuthDtos.RefreshTokenRequest("valid-body-refresh-token");
        AuthDtos.AuthResponse response = AuthDtos.AuthResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refreshed-token")
                .build();

        when(authService.refreshToken(any(AuthDtos.RefreshTokenRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("soodfolio_refresh_token=new-refreshed-token")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")));

        verify(authService).refreshToken(argThat(r -> "valid-body-refresh-token".equals(r.getRefreshToken())));
    }

    @Test
    @DisplayName("POST /auth/refresh - 200 OK on token refresh from HttpOnly cookie")
    void shouldRefreshTokenFromCookieSuccessfully() throws Exception {
        AuthDtos.AuthResponse response = AuthDtos.AuthResponse.builder()
                .accessToken("cookie-access-token")
                .refreshToken("new-cookie-refresh-token")
                .build();

        when(authService.refreshToken(any(AuthDtos.RefreshTokenRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/refresh")
                        .cookie(new Cookie("soodfolio_refresh_token", "cookie-source-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("cookie-access-token"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("soodfolio_refresh_token=new-cookie-refresh-token")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")));

        verify(authService).refreshToken(argThat(r -> "cookie-source-token".equals(r.getRefreshToken())));
    }

    @Test
    @DisplayName("POST /auth/refresh - 400 Bad Request when both cookie and request body are missing")
    void shouldRejectRefreshWhenTokenIsMissing() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Refresh token is required"));

        verify(authService, never()).refreshToken(any());
    }

    @Test
    @DisplayName("POST /auth/logout - 204 No Content with cookie token and clears cookie (Max-Age=0)")
    void shouldLogoutWithCookieAndClearCookie() throws Exception {
        doNothing().when(authService).logout("cookie-logout-token");

        mockMvc.perform(post("/auth/logout")
                        .cookie(new Cookie("soodfolio_refresh_token", "cookie-logout-token")))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("soodfolio_refresh_token=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        verify(authService).logout("cookie-logout-token");
    }

    @Test
    @DisplayName("POST /auth/logout - 204 No Content with request body and clears cookie (Max-Age=0)")
    void shouldLogoutWithBodyAndClearCookie() throws Exception {
        AuthDtos.RefreshTokenRequest request = new AuthDtos.RefreshTokenRequest("body-logout-token");
        doNothing().when(authService).logout("body-logout-token");

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("soodfolio_refresh_token=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        verify(authService).logout("body-logout-token");
    }

    @Test
    @DisplayName("POST /auth/logout - 204 No Content when no token provided still clears cookie")
    void shouldLogoutWithoutTokenAndStillClearCookie() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("soodfolio_refresh_token=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        verify(authService, never()).logout(any());
    }

    @Test
    @DisplayName("GET /auth/oauth/state - 200 OK returns UUID state and sets HttpOnly cookie")
    void shouldGenerateOAuthStateWithCookie() throws Exception {
        mockMvc.perform(get("/auth/oauth/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").isNotEmpty())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("soodfolio_oauth_state=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/v1/auth")));
    }

    @Test
    @DisplayName("POST /auth/oauth/exchange - 400 Bad Request when state is missing in request body")
    void shouldRejectOAuthExchangeWhenStateMissingInBody() throws Exception {
        AuthDtos.OAuthExchangeRequest request = new AuthDtos.OAuthExchangeRequest(
                "GITHUB", "auth-code-123", "http://localhost:3000/callback", null
        );

        mockMvc.perform(post("/auth/oauth/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .cookie(new Cookie("soodfolio_oauth_state", "expected-state")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("CSRF State không hợp lệ hoặc đã hết hạn."));

        verify(authService, never()).exchangeOAuthCode(any());
    }

    @Test
    @DisplayName("POST /auth/oauth/exchange - 400 Bad Request when cookie state is missing")
    void shouldRejectOAuthExchangeWhenCookieStateMissing() throws Exception {
        AuthDtos.OAuthExchangeRequest request = new AuthDtos.OAuthExchangeRequest(
                "GITHUB", "auth-code-123", "http://localhost:3000/callback", "csrf-token-123"
        );

        mockMvc.perform(post("/auth/oauth/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("CSRF State không hợp lệ hoặc đã hết hạn."));

        verify(authService, never()).exchangeOAuthCode(any());
    }

    @Test
    @DisplayName("POST /auth/oauth/exchange - 400 Bad Request when state mismatches cookie state")
    void shouldRejectOAuthExchangeWhenStateMismatchesCookie() throws Exception {
        AuthDtos.OAuthExchangeRequest request = new AuthDtos.OAuthExchangeRequest(
                "GITHUB", "auth-code-123", "http://localhost:3000/callback", "state-attacker"
        );

        mockMvc.perform(post("/auth/oauth/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .cookie(new Cookie("soodfolio_oauth_state", "state-genuine")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("CSRF State không hợp lệ hoặc đã hết hạn."));

        verify(authService, never()).exchangeOAuthCode(any());
    }

    @Test
    @DisplayName("POST /auth/oauth/exchange - 200 OK with valid CSRF state, sets refresh cookie & clears state cookie")
    void shouldExchangeOAuthCodeSuccessfullyWhenStateMatches() throws Exception {
        String validState = "safe-state-token-uuid-1234";
        AuthDtos.OAuthExchangeRequest request = new AuthDtos.OAuthExchangeRequest(
                "GITHUB", "auth-code-valid", "http://localhost:3000/callback", validState
        );

        AuthDtos.AuthResponse response = AuthDtos.AuthResponse.builder()
                .accessToken("oauth-access-token")
                .refreshToken("oauth-refresh-token")
                .tokenType("Bearer")
                .user(AuthDtos.UserDto.builder()
                        .id(UUID.randomUUID())
                        .email("oauth@example.com")
                        .username("oauthuser")
                        .fullName("OAuth User")
                        .role("ROLE_USER")
                        .authProvider("GITHUB")
                        .build())
                .build();

        when(authService.exchangeOAuthCode(any(AuthDtos.OAuthExchangeRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/oauth/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .cookie(new Cookie("soodfolio_oauth_state", validState)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("oauth-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("oauth-refresh-token"))
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE,
                        hasItem(containsString("soodfolio_refresh_token=oauth-refresh-token"))))
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE,
                        hasItem(allOf(containsString("soodfolio_oauth_state="), containsString("Max-Age=0")))));

        verify(authService).exchangeOAuthCode(argThat(r ->
                "GITHUB".equals(r.getProvider()) &&
                "auth-code-valid".equals(r.getCode()) &&
                validState.equals(r.getState())
        ));
    }

    @Test
    @DisplayName("POST /auth/forgot-password - 200 OK")
    void shouldTriggerForgotPassword() throws Exception {
        AuthDtos.ForgotPasswordRequest request = new AuthDtos.ForgotPasswordRequest("creator@soodfolio.dev");
        when(authService.forgotPassword(any())).thenReturn(new AuthDtos.MessageResponse("Link sent"));

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Link sent"));
    }

    @Test
    @DisplayName("GET /auth/verify-reset-token - 200 OK")
    void shouldVerifyResetToken() throws Exception {
        AuthDtos.VerifyResetTokenResponse response = AuthDtos.VerifyResetTokenResponse.builder()
                .valid(true)
                .email("creator@soodfolio.dev")
                .build();

        when(authService.verifyResetToken("test-hex-token")).thenReturn(response);

        mockMvc.perform(get("/auth/verify-reset-token").param("token", "test-hex-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.email").value("creator@soodfolio.dev"));
    }

    @Test
    @DisplayName("POST /auth/reset-password - 200 OK")
    void shouldResetPassword() throws Exception {
        AuthDtos.ResetPasswordRequest request = new AuthDtos.ResetPasswordRequest("token-123", "NewSecretPass123!");
        when(authService.resetPassword(any())).thenReturn(new AuthDtos.MessageResponse("Password reset successful"));

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successful"));
    }
}
