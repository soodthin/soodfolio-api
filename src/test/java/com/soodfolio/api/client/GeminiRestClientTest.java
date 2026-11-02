package com.soodfolio.api.client;

import com.soodfolio.api.exception.AiServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@DisplayName("GeminiRestClient 100% Offline Unit Tests")
class GeminiRestClientTest {

    private static final String TEST_API_KEY = "test-gemini-api-key-12345";
    private static final String TEST_MODEL = "gemini-3.5-flash-lite";
    private static final String BASE_URL = "https://generativelanguage.googleapis.com";
    private static final String EXPECTED_URI = BASE_URL + "/v1beta/models/" + TEST_MODEL + ":generateContent?key=" + TEST_API_KEY;

    private MockRestServiceServer mockServer;
    private GeminiRestClient geminiRestClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        geminiRestClient = new GeminiRestClient(restClient, TEST_API_KEY, TEST_MODEL);
    }

    @Test
    @DisplayName("Should parse candidates[0].content.parts[0].text successfully offline")
    void shouldGenerateContentSuccessfullyOffline() {
        String mockGeminiResponse = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"metricXyz\\": \\"Accomplished X, measured by Y, by doing Z\\"}"
                          }
                        ],
                        "role": "model"
                      },
                      "finishReason": "STOP"
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo(EXPECTED_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.contents[0].parts[0].text").value("Raw text prompt"))
                .andExpect(jsonPath("$.systemInstruction.parts[0].text").value("System instructions"))
                .andExpect(jsonPath("$.generationConfig.temperature").value(0.2))
                .andExpect(jsonPath("$.generationConfig.responseMimeType").value("application/json"))
                .andRespond(withSuccess(mockGeminiResponse, MediaType.APPLICATION_JSON));

        String result = geminiRestClient.generateContent("System instructions", "Raw text prompt");

        mockServer.verify();
        assertThat(result).isEqualTo("{\"metricXyz\": \"Accomplished X, measured by Y, by doing Z\"}");
    }

    @Test
    @DisplayName("Should strip markdown code fences (```json ... ```) from Gemini response")
    void shouldStripMarkdownCodeFencesFromResponse() {
        String mockResponseWithFences = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "```json\\n{\\n  \\"recommendedTemplateId\\": \\"terminal\\",\\n  \\"reason\\": \\"Phù hợp DevOps\\"\\n}\\n```"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo(EXPECTED_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(mockResponseWithFences, MediaType.APPLICATION_JSON));

        String result = geminiRestClient.generateContent("sys", "user");

        mockServer.verify();
        assertThat(result)
                .doesNotStartWith("```json")
                .doesNotEndWith("```")
                .contains("\"recommendedTemplateId\": \"terminal\"");
    }

    @Test
    @DisplayName("Should throw AiServiceUnavailableException immediately when API key is missing or blank")
    void shouldThrowWhenApiKeyIsMissingOrBlank() {
        GeminiRestClient clientWithoutKey = new GeminiRestClient(RestClient.builder().build(), "", TEST_MODEL);
        GeminiRestClient clientWithNullKey = new GeminiRestClient(RestClient.builder().build(), null, TEST_MODEL);

        assertThatThrownBy(() -> clientWithoutKey.generateContent("sys", "user"))
                .isInstanceOf(AiServiceUnavailableException.class)
                .hasMessageContaining("chưa được cấu hình khóa truy cập");

        assertThatThrownBy(() -> clientWithNullKey.generateContent("sys", "user"))
                .isInstanceOf(AiServiceUnavailableException.class)
                .hasMessageContaining("chưa được cấu hình khóa truy cập");
    }

    @Test
    @DisplayName("Should throw AiServiceUnavailableException on network failure or 8s timeout")
    void shouldThrowAiServiceUnavailableExceptionOnTimeoutOrNetworkFailure() {
        mockServer.expect(requestTo(EXPECTED_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withException(new SocketTimeoutException("Read timed out after 8000ms")));

        assertThatThrownBy(() -> geminiRestClient.generateContent("sys", "user"))
                .isInstanceOf(AiServiceUnavailableException.class)
                .hasMessageContaining("không thể phản hồi trong 8 giây");

        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw AiServiceUnavailableException on general I/O network failure")
    void shouldThrowAiServiceUnavailableExceptionOnGeneralIoFailure() {
        mockServer.expect(requestTo(EXPECTED_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withException(new IOException("Connection reset by peer")));

        assertThatThrownBy(() -> geminiRestClient.generateContent("sys", "user"))
                .isInstanceOf(AiServiceUnavailableException.class)
                .hasMessageContaining("không thể phản hồi trong 8 giây");

        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw AiServiceUnavailableException on Google API 500 error")
    void shouldThrowAiServiceUnavailableExceptionOnServerError() {
        mockServer.expect(requestTo(EXPECTED_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        assertThatThrownBy(() -> geminiRestClient.generateContent("sys", "user"))
                .isInstanceOf(AiServiceUnavailableException.class)
                .hasMessageContaining("Không thể kết nối đến máy chủ AI của Google");

        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw AiServiceUnavailableException when candidates array is empty")
    void shouldThrowWhenCandidatesListIsEmpty() {
        String mockEmptyResponse = "{\"candidates\": []}";

        mockServer.expect(requestTo(EXPECTED_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(mockEmptyResponse, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> geminiRestClient.generateContent("sys", "user"))
                .isInstanceOf(AiServiceUnavailableException.class)
                .hasMessageContaining("Không nhận được dữ liệu phản hồi từ mô hình AI");

        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw AiServiceUnavailableException when parts array is empty or content is null")
    void shouldThrowWhenPartsListIsEmpty() {
        String mockResponseNoParts = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": []
                      }
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo(EXPECTED_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(mockResponseNoParts, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> geminiRestClient.generateContent("sys", "user"))
                .isInstanceOf(AiServiceUnavailableException.class)
                .hasMessageContaining("Không nhận được dữ liệu phản hồi từ mô hình AI");

        mockServer.verify();
    }
}
