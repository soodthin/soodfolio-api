package com.soodfolio.api.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.soodfolio.api.exception.AiServiceUnavailableException;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;

@Component
@Slf4j
public class GeminiRestClient {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    @Autowired
    public GeminiRestClient(
            RestClient.Builder restClientBuilder,
            @Value("${app.gemini.api-key:}") String apiKey,
            @Value("${app.gemini.model:gemini-3.5-flash-lite}") String model,
            @Value("${app.gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
            @Value("${app.gemini.timeout-seconds:8}") int timeoutSeconds
    ) {
        this.apiKey = apiKey;
        this.model = model;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(4));
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // Secondary constructor for testing with custom RestClient
    public GeminiRestClient(RestClient restClient, String apiKey, String model) {
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.model = model;
    }

    public String generateContent(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("GEMINI_API_KEY is not configured.");
            throw new AiServiceUnavailableException("Dịch vụ AI chưa được cấu hình khóa truy cập (GEMINI_API_KEY).");
        }

        GeminiApiRequest requestPayload = GeminiApiRequest.builder()
                .contents(List.of(
                        GeminiContent.builder()
                                .role("user")
                                .parts(List.of(new GeminiPart(userPrompt)))
                                .build()
                ))
                .systemInstruction(
                        GeminiContent.builder()
                                .parts(List.of(new GeminiPart(systemPrompt)))
                                .build()
                )
                .generationConfig(
                        GeminiGenerationConfig.builder()
                                .temperature(0.2)
                                .responseMimeType("application/json")
                                .build()
                )
                .build();

        try {
            GeminiApiResponse response = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/{model}:generateContent")
                            .queryParam("key", apiKey)
                            .build(model))
                    .body(requestPayload)
                    .retrieve()
                    .body(GeminiApiResponse.class);

            if (response == null || response.extractFirstText() == null) {
                throw new AiServiceUnavailableException("Không nhận được dữ liệu phản hồi từ mô hình AI.");
            }

            return cleanJsonMarkdown(response.extractFirstText());

        } catch (ResourceAccessException ex) {
            log.error("Gemini API call timed out or network connectivity failed (8s limit): {}", ex.getMessage());
            throw new AiServiceUnavailableException("Dịch vụ AI không thể phản hồi trong 8 giây. Vui lòng thử lại sau.", ex);
        } catch (RestClientException ex) {
            log.error("Gemini API returned error: {}", ex.getMessage());
            throw new AiServiceUnavailableException("Không thể kết nối đến máy chủ AI của Google. Vui lòng thử lại sau.", ex);
        }
    }

    private String cleanJsonMarkdown(String raw) {
        if (raw == null) {
            return "{}";
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }

    // Gemini API Request/Response Models
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeminiApiRequest {
        private List<GeminiContent> contents;
        private GeminiContent systemInstruction;
        private GeminiGenerationConfig generationConfig;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeminiContent {
        private String role;
        private List<GeminiPart> parts;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeminiPart {
        private String text;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeminiGenerationConfig {
        private Double temperature;
        private String responseMimeType;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GeminiApiResponse {
        private List<Candidate> candidates;

        public String extractFirstText() {
            if (candidates == null || candidates.isEmpty()) return null;
            Candidate c = candidates.get(0);
            if (c.getContent() == null || c.getContent().getParts() == null || c.getContent().getParts().isEmpty()) {
                return null;
            }
            return c.getContent().getParts().get(0).getText();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Candidate {
        private GeminiContent content;
        private String finishReason;
    }
}
