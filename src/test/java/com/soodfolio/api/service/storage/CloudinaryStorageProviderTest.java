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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("CloudinaryStorageProvider Unit Tests with MockRestServiceServer")
class CloudinaryStorageProviderTest {

    private static final String CLOUDINARY_URL = "cloudinary://123456789012345:my_api_secret_key@test-cloud-name";
    private static final long FIXED_TIMESTAMP = 1731500000L;

    private MockRestServiceServer mockServer;
    private CloudinaryStorageProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        provider = new CloudinaryStorageProvider(CLOUDINARY_URL, restClient, () -> FIXED_TIMESTAMP);
    }

    @Test
    @DisplayName("Should successfully upload image file to /image/upload endpoint and return secure_url")
    void shouldUploadImageSuccessfully() {
        byte[] content = "fake-image-bytes".getBytes(StandardCharsets.UTF_8);
        String filename = "avatar-123.png";
        String contentType = "image/png";
        String subDir = "images";

        String expectedEndpoint = "https://api.cloudinary.com/v1_1/test-cloud-name/image/upload";
        String mockResponseJson = """
                {
                    "secure_url": "https://res.cloudinary.com/test-cloud-name/image/upload/v1731500000/images/avatar-123.png",
                    "public_id": "avatar-123",
                    "format": "png"
                }
                """;

        mockServer.expect(requestTo(expectedEndpoint))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(mockResponseJson, MediaType.APPLICATION_JSON));

        String resultUrl = provider.store(content, filename, contentType, subDir);

        mockServer.verify();
        assertThat(resultUrl).isEqualTo("https://res.cloudinary.com/test-cloud-name/image/upload/v1731500000/images/avatar-123.png");
    }

    @Test
    @DisplayName("Should successfully upload resume PDF to /raw/upload endpoint and keep .pdf in public_id")
    void shouldUploadResumePdfSuccessfully() {
        byte[] content = "%PDF-1.7 resume content".getBytes(StandardCharsets.UTF_8);
        String filename = "my-resume-uuid.pdf";
        String contentType = "application/pdf";
        String subDir = "resumes";

        String expectedEndpoint = "https://api.cloudinary.com/v1_1/test-cloud-name/raw/upload";
        String mockResponseJson = """
                {
                    "secure_url": "https://res.cloudinary.com/test-cloud-name/raw/upload/v1731500000/resumes/my-resume-uuid.pdf",
                    "public_id": "my-resume-uuid.pdf",
                    "resource_type": "raw"
                }
                """;

        mockServer.expect(requestTo(expectedEndpoint))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(mockResponseJson, MediaType.APPLICATION_JSON));

        String resultUrl = provider.store(content, filename, contentType, subDir);

        mockServer.verify();
        assertThat(resultUrl).isEqualTo("https://res.cloudinary.com/test-cloud-name/raw/upload/v1731500000/resumes/my-resume-uuid.pdf");
    }

    @Test
    @DisplayName("Should throw RuntimeException when Cloudinary server returns 5xx error")
    void shouldThrowRuntimeExceptionOnCloudinaryServerError() {
        byte[] content = "test-bytes".getBytes(StandardCharsets.UTF_8);
        String filename = "photo.jpg";
        String contentType = "image/jpeg";
        String subDir = "images";

        String expectedEndpoint = "https://api.cloudinary.com/v1_1/test-cloud-name/image/upload";

        mockServer.expect(requestTo(expectedEndpoint))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        assertThatThrownBy(() -> provider.store(content, filename, contentType, subDir))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Lỗi tải tệp lên Cloudinary");

        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when CLOUDINARY_URL is invalid")
    void shouldThrowIllegalArgumentExceptionOnInvalidUrl() {
        assertThatThrownBy(() -> new CloudinaryStorageProvider("invalid-url"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Định dạng CLOUDINARY_URL không hợp lệ");

        assertThatThrownBy(() -> new CloudinaryStorageProvider("cloudinary://missing-at-sign"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new CloudinaryStorageProvider("cloudinary://nokey@cloud"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
