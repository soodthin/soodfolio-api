package com.soodfolio.api.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("BrevoEmailServiceImpl Unit Tests (100% Offline with MockRestServiceServer)")
class BrevoEmailServiceImplTest {

    private static final String API_KEY = "xkeysib-mock-api-key";
    private static final String BASE_URL = "https://api.brevo.com";
    private static final String SENDER_EMAIL = "noreply@soodfolio.com";
    private static final String SENDER_NAME = "SoodFolio Support";

    private MockRestServiceServer mockServer;
    private BrevoEmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        emailService = new BrevoEmailServiceImpl(
                API_KEY,
                BASE_URL,
                SENDER_EMAIL,
                SENDER_NAME,
                restClient
        );
    }

    @Test
    @DisplayName("Should send password reset email via Brevo with correct payload and headers")
    void shouldSendPasswordResetEmailSuccessfully() {
        String toEmail = "developer@example.com";
        String recipientName = "Alex Nguyen";
        String resetLink = "http://localhost:3000/reset-password?token=secret123";

        String expectedEndpoint = "https://api.brevo.com/v3/smtp/email";

        mockServer.expect(requestTo(expectedEndpoint))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", API_KEY))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("noreply@soodfolio.com")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("developer@example.com")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Alex Nguyen")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("http://localhost:3000/reset-password?token=secret123")))
                .andRespond(withSuccess("{\"messageId\":\"<12345@smtp-relay.mailin.fr>\"}", MediaType.APPLICATION_JSON));

        assertDoesNotThrow(() ->
                emailService.sendPasswordResetEmail(toEmail, recipientName, resetLink)
        );

        mockServer.verify();
    }

    @Test
    @DisplayName("Should escape malicious HTML tags in recipientName to prevent HTML injection")
    void shouldEscapeHtmlInRecipientName() {
        String toEmail = "victim@example.com";
        String maliciousName = "Hacker <script>alert('XSS')</script>";
        String resetLink = "http://localhost:3000/reset-password?token=safe456";

        mockServer.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script>"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("&lt;script&gt;alert(&#x27;XSS&#x27;)&lt;/script&gt;")))
                .andRespond(withSuccess("{\"messageId\":\"<ok>\"}", MediaType.APPLICATION_JSON));

        assertDoesNotThrow(() ->
                emailService.sendPasswordResetEmail(toEmail, maliciousName, resetLink)
        );

        mockServer.verify();
    }

    @Test
    @DisplayName("Should gracefully handle Brevo 500 error without throwing unhandled exception to caller")
    void shouldHandleBrevoServerErrorGracefully() {
        String toEmail = "user@example.com";
        String recipientName = "Test User";
        String resetLink = "http://localhost:3000/reset-password?token=token789";

        mockServer.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        // Must not throw exception out to ensure caller (forgotPassword) completes safely
        assertDoesNotThrow(() ->
                emailService.sendPasswordResetEmail(toEmail, recipientName, resetLink)
        );

        mockServer.verify();
    }

    @Test
    @DisplayName("Should skip sending when apiKey is blank")
    void shouldSkipWhenApiKeyIsBlank() {
        BrevoEmailServiceImpl emptyKeyService = new BrevoEmailServiceImpl(
                "", BASE_URL, SENDER_EMAIL, SENDER_NAME
        );

        // Does not call network and does not throw
        assertDoesNotThrow(() ->
                emptyKeyService.sendPasswordResetEmail("user@example.com", "User", "http://localhost:3000")
        );
    }
}
