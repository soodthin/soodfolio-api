package com.soodfolio.api.config;

import com.soodfolio.api.controller.AuthController;
import com.soodfolio.api.controller.PortfolioController;
import com.soodfolio.api.dto.PortfolioResponseDto;
import com.soodfolio.api.security.AuthCookieHelper;
import com.soodfolio.api.security.CustomUserDetailsService;
import com.soodfolio.api.security.JwtAuthenticationEntryPoint;
import com.soodfolio.api.security.JwtAuthenticationFilter;
import com.soodfolio.api.security.JwtTokenProvider;
import com.soodfolio.api.service.AuthService;
import com.soodfolio.api.service.I18nService;
import com.soodfolio.api.service.PortfolioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, PortfolioController.class})
@Import({
        WebSecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class,
        JwtTokenProvider.class
})
@TestPropertySource(properties = {
        "app.jwt.secret=dGVzdC1qd3Qtc2VjcmV0LWZvci11bml0LXRlc3RzLW11c3QtYmUtYXQtbGVhc3QtMjU2LWJpdHMtZXh0cmEtbG9uZw==", // gitleaks:allow
        "app.jwt.expiration-ms=900000",
        "app.cors.allowed-origins=http://localhost:3000,http://localhost:3001",
        "app.oauth2.sandbox-enabled=false"
})
class WebSecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private I18nService i18nService;

    @MockBean
    private AuthService authService;

    @MockBean
    private PortfolioService portfolioService;

    @MockBean
    private AuthCookieHelper authCookieHelper;

    @Test
    @DisplayName("GET /auth/me without JWT token must be rejected with 401 Unauthorized")
    void shouldRejectGetAuthMeWithoutToken() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource"));
    }

    @Test
    @DisplayName("GET /portfolios/me without JWT token must be rejected with 401 Unauthorized")
    void shouldRejectGetPortfoliosMeWithoutToken() throws Exception {
        mockMvc.perform(get("/portfolios/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("GET /portfolios/{username} is public and permits unauthenticated access")
    void shouldPermitPublicPortfolioAccess() throws Exception {
        PortfolioResponseDto dto = PortfolioResponseDto.builder()
                .username("alex")
                .title("Alex Nguyen")
                .isPublished(true)
                .build();

        when(portfolioService.getPortfolioByUsername("alex")).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/portfolios/alex"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alex"));
    }

    @Test
    @DisplayName("CORS preflight OPTIONS for allowed origin should return 200 OK and allow credentials")
    void shouldAcceptCorsPreflightForAllowedOrigin() throws Exception {
        mockMvc.perform(options("/portfolios/alex")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @DisplayName("CORS preflight OPTIONS for untrusted origin (e.g. evil.example.com) should be rejected (403 Forbidden)")
    void shouldRejectCorsPreflightForUntrustedOrigin() throws Exception {
        mockMvc.perform(options("/portfolios/alex")
                        .header(HttpHeaders.ORIGIN, "https://evil.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("GET /actuator/health is permitted anonymously without JWT token")
    void shouldPermitActuatorHealthWithoutToken() throws Exception {
        // Must not be rejected with 401 Unauthorized
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().is(org.hamcrest.Matchers.not(401)));
    }

    @Test
    @DisplayName("GET /actuator/env and /actuator/beans must be rejected with 401 Unauthorized")
    void shouldRejectSensitiveActuatorEndpointsWithoutToken() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/actuator/beans"))
                .andExpect(status().isUnauthorized());
    }
}
