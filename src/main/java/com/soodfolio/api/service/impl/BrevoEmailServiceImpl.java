package com.soodfolio.api.service.impl;

import com.soodfolio.api.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "brevo")
public class BrevoEmailServiceImpl implements EmailService {

    private final String apiKey;
    private final String baseUrl;
    private final String senderEmail;
    private final String senderName;
    private final RestClient restClient;

    public BrevoEmailServiceImpl(
            @Value("${app.mail.brevo.api-key:}") String apiKey,
            @Value("${app.mail.brevo.base-url:https://api.brevo.com}") String baseUrl,
            @Value("${app.mail.sender-email:}") String senderEmail,
            @Value("${app.mail.sender-name:SoodFolio}") String senderName
    ) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.baseUrl = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl.replaceAll("/+$", "") : "https://api.brevo.com";
        this.senderEmail = senderEmail != null ? senderEmail.trim() : "";
        this.senderName = (senderName != null && !senderName.isBlank()) ? senderName.trim() : "SoodFolio";
        this.restClient = buildDefaultRestClient();
    }

    public BrevoEmailServiceImpl(
            String apiKey,
            String baseUrl,
            String senderEmail,
            String senderName,
            RestClient restClient
    ) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.baseUrl = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl.replaceAll("/+$", "") : "https://api.brevo.com";
        this.senderEmail = senderEmail != null ? senderEmail.trim() : "";
        this.senderName = (senderName != null && !senderName.isBlank()) ? senderName.trim() : "SoodFolio";
        this.restClient = restClient != null ? restClient : buildDefaultRestClient();
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String recipientName, String resetLink) {
        if (apiKey.isBlank()) {
            log.warn("Brevo email dispatch skipped: BREVO_API_KEY is not configured");
            return;
        }

        String safeRecipientName = (recipientName != null && !recipientName.isBlank())
                ? escapeHtml(recipientName)
                : "Người dùng";

        String htmlContent = buildResetPasswordHtml(safeRecipientName, resetLink);

        Map<String, Object> payload = Map.of(
                "sender", Map.of("name", senderName, "email", senderEmail),
                "to", List.of(Map.of("email", toEmail, "name", safeRecipientName)),
                "subject", "SoodFolio - Yêu cầu đặt lại mật khẩu",
                "htmlContent", htmlContent
        );

        String endpoint = baseUrl + "/v3/smtp/email";

        try {
            restClient.post()
                    .uri(endpoint)
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Password reset email dispatched successfully via Brevo");
        } catch (RestClientResponseException e) {
            log.warn("Brevo email dispatch failed: status={}", e.getStatusCode().value());
        } catch (RestClientException e) {
            log.warn("Brevo email dispatch failed: network error/timeout: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("Brevo email dispatch failed: unexpected error");
        }
    }

    private String buildResetPasswordHtml(String safeName, String resetLink) {
        return "<!DOCTYPE html>"
                + "<html><head><meta charset=\"utf-8\"></head>"
                + "<body style=\"font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #0b0f19; color: #f3f4f6; padding: 24px;\">"
                + "<div style=\"max-width: 560px; margin: 0 auto; background-color: #111827; padding: 32px; border-radius: 8px; border: 1px solid #1f2937;\">"
                + "<h2 style=\"color: #6366f1; margin-top: 0; font-size: 20px;\">SoodFolio - Khôi phục mật khẩu</h2>"
                + "<p style=\"font-size: 15px; line-height: 1.5;\">Xin chào <strong>" + safeName + "</strong>,</p>"
                + "<p style=\"font-size: 15px; line-height: 1.5;\">Chúng tôi đã nhận được yêu cầu đặt lại mật khẩu cho tài khoản SoodFolio của bạn. Vui lòng bấm vào nút bên dưới để tiến hành thiết lập mật khẩu mới:</p>"
                + "<p style=\"margin: 32px 0; text-align: center;\">"
                + "<a href=\"" + resetLink + "\" style=\"background-color: #4f46e5; color: #ffffff; padding: 12px 28px; text-decoration: none; border-radius: 6px; font-weight: 600; display: inline-block;\">Đặt lại mật khẩu</a>"
                + "</p>"
                + "<p style=\"color: #9ca3af; font-size: 13px; line-height: 1.5;\">Liên kết này có hiệu lực trong vòng 15 phút. Nếu bạn không yêu cầu hành động này, vui lòng bỏ qua email.</p>"
                + "<hr style=\"border: none; border-top: 1px solid #1f2937; margin: 24px 0;\" />"
                + "<p style=\"color: #6b7280; font-size: 12px; margin-bottom: 0;\">© SoodFolio Platform. All rights reserved.</p>"
                + "</div></body></html>";
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    private static RestClient buildDefaultRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(8));
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }
}
