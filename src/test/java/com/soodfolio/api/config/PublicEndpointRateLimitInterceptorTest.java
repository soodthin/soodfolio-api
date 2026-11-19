package com.soodfolio.api.config;

import com.soodfolio.api.dto.ErrorResponseDto;
import com.soodfolio.api.exception.GlobalExceptionHandler;
import com.soodfolio.api.exception.RateLimitExceededException;
import com.soodfolio.api.service.I18nService;
import com.soodfolio.api.service.PublicRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("PublicEndpointRateLimitInterceptor Unit & Integration Tests")
class PublicEndpointRateLimitInterceptorTest {

    @Mock
    private PublicRateLimiter rateLimiter;

    @Mock
    private ObjectProvider<PublicRateLimiter> rateLimiterProvider;

    @Mock
    private I18nService i18nService;

    private PublicEndpointRateLimitInterceptor interceptor;

    @BeforeEach
    void setUp() {
        lenient().when(rateLimiterProvider.getIfAvailable()).thenReturn(rateLimiter);
        interceptor = new PublicEndpointRateLimitInterceptor(rateLimiterProvider);
        ReflectionTestUtils.setField(interceptor, "rateLimitEnabled", true);
    }

    @Test
    @DisplayName("Should extract client IP from X-Forwarded-For first entry")
    void shouldExtractClientIpFromXForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1, 192.168.1.1");

        String ip = interceptor.extractClientIp(request);
        assertThat(ip).isEqualTo("203.0.113.10");
    }

    @Test
    @DisplayName("Should trim and extract single IP from X-Forwarded-For")
    void shouldTrimAndExtractSingleIpFromXff() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "   198.51.100.25   ");

        String ip = interceptor.extractClientIp(request);
        assertThat(ip).isEqualTo("198.51.100.25");
    }

    @Test
    @DisplayName("Should fallback to RemoteAddr when X-Forwarded-For is missing")
    void shouldFallbackToRemoteAddrWhenXffMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");

        String ip = interceptor.extractClientIp(request);
        assertThat(ip).isEqualTo("192.168.1.100");
    }

    @Test
    @DisplayName("Should fallback to 127.0.0.1 when both X-Forwarded-For and RemoteAddr are absent")
    void shouldFallbackToDefaultWhenAllIpSourcesAbsent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(null);

        String ip = interceptor.extractClientIp(request);
        assertThat(ip).isEqualTo("127.0.0.1");
    }

    @Test
    @DisplayName("Should skip rate limiting for non-POST HTTP methods")
    void shouldSkipRateLimitingForNonPostMethods() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verify(rateLimiter, never()).checkRateLimit(any(), any(), any(int.class));
    }

    @Test
    @DisplayName("Should enforce 5 req/min on POST /auth/forgot-password")
    void shouldEnforceRateLimitOnForgotPassword() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/forgot-password");
        request.setRemoteAddr("10.0.0.5");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verify(rateLimiter).checkRateLimit("forgot-password", "10.0.0.5", 5);
    }

    @Test
    @DisplayName("Should enforce 15 req/min on POST /auth/login")
    void shouldEnforceRateLimitOnLogin() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.setRemoteAddr("10.0.0.6");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verify(rateLimiter).checkRateLimit("login", "10.0.0.6", 15);
    }

    @Test
    @DisplayName("Should enforce 30 req/min on POST /portfolios/{slug}/views")
    void shouldEnforceRateLimitOnPortfolioViews() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/portfolios/john-doe/views");
        request.setRemoteAddr("10.0.0.7");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verify(rateLimiter).checkRateLimit("views", "10.0.0.7", 30);
    }

    @Test
    @DisplayName("Should enforce 60 req/min on POST /public/links/{token}/telemetry")
    void shouldEnforceRateLimitOnTelemetry() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/public/links/abc-123/telemetry");
        request.setRemoteAddr("10.0.0.8");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verify(rateLimiter).checkRateLimit("telemetry", "10.0.0.8", 60);
    }

    @Test
    @DisplayName("Should match patterns even when prefixed with /api/v1 context path")
    void shouldMatchPatternsWithApiPrefix() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRequestURI("/api/v1/auth/login");
        request.setRemoteAddr("10.0.0.9");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verify(rateLimiter).checkRateLimit("login", "10.0.0.9", 15);
    }

    @Test
    @DisplayName("Should bypass checking when rateLimitEnabled is false")
    void shouldBypassCheckingWhenRateLimitDisabled() {
        ReflectionTestUtils.setField(interceptor, "rateLimitEnabled", false);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verify(rateLimiter, never()).checkRateLimit(any(), any(), any(int.class));
    }

    @Test
    @DisplayName("Should return HTTP 429 and ErrorResponseDto when rate limit is exceeded in MockMvc")
    void shouldReturn429WhenRateLimitExceededInMockMvc() throws Exception {
        String rateLimitMsg = "Quá nhiều yêu cầu. Vui lòng thử lại sau 1 phút.";
        when(i18nService.getMessage(rateLimitMsg)).thenReturn(rateLimitMsg);

        doThrow(new RateLimitExceededException(rateLimitMsg))
                .when(rateLimiter).checkRateLimit("login", "203.0.113.99", 15);

        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler(i18nService);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new DummyAuthController())
                .addInterceptors(interceptor)
                .setControllerAdvice(exceptionHandler)
                .build();

        mockMvc.perform(post("/auth/login")
                        .header("X-Forwarded-For", "203.0.113.99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\",\"password\":\"secret\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.message").value(rateLimitMsg))
                .andExpect(jsonPath("$.path").value("/auth/login"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @RestController
    private static class DummyAuthController {
        @PostMapping("/auth/login")
        public String login() {
            return "ok";
        }
    }
}
