package com.soodfolio.api.controller;

import com.soodfolio.api.security.AuthCookieHelper;
import com.soodfolio.api.security.JwtAuthenticationEntryPoint;
import com.soodfolio.api.security.JwtAuthenticationFilter;
import com.soodfolio.api.security.JwtTokenProvider;
import com.soodfolio.api.service.AuthService;
import com.soodfolio.api.service.I18nService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, OAuthSandboxController.class})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "app.jwt.secret=dGVzdC1qd3Qtc2VjcmV0LWZvci11bml0LXRlc3RzLW11c3QtYmUtYXQtbGVhc3QtMjU2LWJpdHMtZXh0cmEtbG9uZw==", // gitleaks:allow
        "app.jwt.expiration-ms=900000",
        "app.oauth2.sandbox-enabled=true"
})
class OAuthSandboxMappingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private AuthCookieHelper authCookieHelper;

    @MockBean
    private I18nService i18nService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("Should successfully initialize application context without ambiguous mapping between AuthController and OAuthSandboxController")
    void shouldInitializeWithoutAmbiguousMapping() {
        assertThat(mockMvc).isNotNull();
    }

    @Test
    @DisplayName("Should route /auth/oauth/state to AuthController and /auth/oauth/sandbox to OAuthSandboxController")
    void shouldRouteToCorrectControllers() throws Exception {
        when(authCookieHelper.createOAuthStateCookie(any()))
                .thenReturn(ResponseCookie.from("oauth_state", "test").build());

        // Verifies AuthController mapping
        mockMvc.perform(get("/auth/oauth/state"))
                .andExpect(status().isOk());

        // Verifies OAuthSandboxController mapping exists and is not ambiguous
        // 400 Bad Request is expected because payload is missing/empty, confirming Spring DispatcherServlet routed to the controller method
        mockMvc.perform(post("/auth/oauth/sandbox")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
