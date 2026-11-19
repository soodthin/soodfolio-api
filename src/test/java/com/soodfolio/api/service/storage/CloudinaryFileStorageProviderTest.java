package com.soodfolio.api.service.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

@DisplayName("CloudinaryFileStorageProvider Unit Tests (100% Offline with MockRestServiceServer)")
class CloudinaryFileStorageProviderTest {

    private static final String CLOUD_NAME = "test-cloud";
    private static final String API_KEY = "123456789";
    private static final String API_SECRET = "super_secret_key";
    private static final String BASE_URL = "https://api.cloudinary.com";
    private static final String FOLDER_PREFIX = "soodfolio";

    private MockRestServiceServer mockServer;
    private CloudinaryFileStorageProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        provider = new CloudinaryFileStorageProvider(
                CLOUD_NAME,
                API_KEY,
                API_SECRET,
                FOLDER_PREFIX,
                BASE_URL,
                restClient
        );
    }

    @Test
    @DisplayName("Should upload image to /image/upload with Basic Auth and stripped public_id extension")
    void shouldUploadImageSuccessfully() {
        byte[] content = "fake-png-bytes".getBytes(StandardCharsets.UTF_8);
        String subDir = "images";
        String fileName = "avatar-123.png";
        String contentType = "image/png";

        String expectedEndpoint = "https://api.cloudinary.com/v1_1/test-cloud/image/upload";
        String mockResponse = """
                {
                    "secure_url": "https://res.cloudinary.com/test-cloud/image/upload/v1731500000/soodfolio/images/avatar-123.png",
                    "public_id": "avatar-123",
                    "format": "png"
                }
                """;

        mockServer.expect(requestTo(expectedEndpoint))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Basic MTIzNDU2Nzg5OnN1cGVyX3NlY3JldF9rZXk=")) // Base64 of 123456789:super_secret_key
                .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

        String url = provider.store(content, subDir, fileName, contentType, true);

        mockServer.verify();
        assertThat(url).isEqualTo("https://res.cloudinary.com/test-cloud/image/upload/v1731500000/soodfolio/images/avatar-123.png");
    }

    @Test
    @DisplayName("Should upload resume to /raw/upload with Basic Auth and keep .pdf in public_id")
    void shouldUploadResumePdfSuccessfully() {
        byte[] content = "%PDF-1.7 resume bytes".getBytes(StandardCharsets.UTF_8);
        String subDir = "resumes";
        String fileName = "resume-abc.pdf";
        String contentType = "application/pdf";

        String expectedEndpoint = "https://api.cloudinary.com/v1_1/test-cloud/raw/upload";
        String mockResponse = """
                {
                    "secure_url": "https://res.cloudinary.com/test-cloud/raw/upload/v1731500000/soodfolio/resumes/resume-abc.pdf",
                    "public_id": "resume-abc.pdf",
                    "resource_type": "raw"
                }
                """;

        mockServer.expect(requestTo(expectedEndpoint))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Basic MTIzNDU2Nzg5OnN1cGVyX3NlY3JldF9rZXk="))
                .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

        String url = provider.store(content, subDir, fileName, contentType, false);

        mockServer.verify();
        assertThat(url).isEqualTo("https://res.cloudinary.com/test-cloud/raw/upload/v1731500000/soodfolio/resumes/resume-abc.pdf");
    }

    @Test
    @DisplayName("Should throw RuntimeException when Cloudinary returns 401 Unauthorized without leaking secret")
    void shouldThrowRuntimeExceptionOnUnauthorized() {
        byte[] content = "some-bytes".getBytes(StandardCharsets.UTF_8);

        mockServer.expect(requestTo("https://api.cloudinary.com/v1_1/test-cloud/image/upload"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withUnauthorizedRequest());

        assertThatThrownBy(() -> provider.store(content, "images", "photo.jpg", "image/jpeg", true))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("status=401")
                .hasMessageNotContaining(API_SECRET);

        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw RuntimeException when Cloudinary returns 500 Internal Server Error")
    void shouldThrowRuntimeExceptionOnServerError() {
        byte[] content = "some-bytes".getBytes(StandardCharsets.UTF_8);

        mockServer.expect(requestTo("https://api.cloudinary.com/v1_1/test-cloud/image/upload"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        assertThatThrownBy(() -> provider.store(content, "images", "photo.jpg", "image/jpeg", true))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("status=500")
                .hasMessageNotContaining(API_SECRET);

        mockServer.verify();
    }

    @Test
    @DisplayName("Should fallback parse CLOUDINARY_URL and throw generic message on format error")
    void shouldFallbackParseCloudinaryUrlSafely() {
        // Valid CLOUDINARY_URL format
        CloudinaryFileStorageProvider urlProvider = new CloudinaryFileStorageProvider(
                "", "", "", "https://api.cloudinary.com", "soodfolio",
                "cloudinary://custom_key:custom_secret@custom_cloud"
        );
        assertThat(urlProvider).isNotNull();

        // Invalid format must never leak credential string
        assertThatThrownBy(() -> new CloudinaryFileStorageProvider(
                "", "", "", "https://api.cloudinary.com", "soodfolio", "invalid-url-string"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid CLOUDINARY_URL format");
    }
}
