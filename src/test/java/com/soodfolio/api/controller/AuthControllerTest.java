package com.soodfolio.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soodfolio.api.dto.AuthDtos;
import com.soodfolio.api.exception.GlobalExceptionHandler;
import com.soodfolio.api.service.AuthService;
import com.soodfolio.api.service.I18nService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

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
    @DisplayName("POST /auth/register - 201 Created on valid register payload")
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
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("POST /auth/register - 400 Bad Request when email is invalid or password too short (@Valid)")
    void shouldRejectInvalidRegistration() throws Exception {
        AuthDtos.RegisterRequest invalidRequest = AuthDtos.RegisterRequest.builder()
                .email("not-an-email")
                .username("INVALID_CAPS") // Violates regex lowercase
                .fullName("")
                .password("123") // Less than 6 characters
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
    @DisplayName("POST /auth/login - 200 OK on valid credentials")
    void shouldLoginSuccessfully() throws Exception {
        AuthDtos.LoginRequest request = new AuthDtos.LoginRequest("test@soodfolio.dev", "Password123!");
        AuthDtos.AuthResponse response = AuthDtos.AuthResponse.builder()
                .accessToken("mock-login-token")
                .refreshToken("mock-refresh-token")
                .tokenType("Bearer")
                .build();

        when(authService.login(any(AuthDtos.LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock-login-token"));
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
    @DisplayName("POST /auth/refresh - 200 OK on token refresh")
    void shouldRefreshTokenSuccessfully() throws Exception {
        AuthDtos.RefreshTokenRequest request = new AuthDtos.RefreshTokenRequest("valid-refresh-uuid");
        AuthDtos.AuthResponse response = AuthDtos.AuthResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("valid-refresh-uuid")
                .build();

        when(authService.refreshToken(any(AuthDtos.RefreshTokenRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"));
    }

    @Test
    @DisplayName("POST /auth/logout - 204 No Content")
    void shouldLogoutSuccessfully() throws Exception {
        AuthDtos.RefreshTokenRequest request = new AuthDtos.RefreshTokenRequest("valid-refresh-uuid");
        doNothing().when(authService).logout("valid-refresh-uuid");

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(authService).logout("valid-refresh-uuid");
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
