package com.soodfolio.api.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "cloudinary")
public class CloudinaryFileStorageProvider implements FileStorageProvider {

    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;
    private final String baseUrl;
    private final String folderPrefix;
    private final RestClient restClient;

    public CloudinaryFileStorageProvider(
            @Value("${app.storage.cloudinary.cloud-name:}") String cloudName,
            @Value("${app.storage.cloudinary.api-key:}") String apiKey,
            @Value("${app.storage.cloudinary.api-secret:}") String apiSecret,
            @Value("${app.storage.cloudinary.base-url:https://api.cloudinary.com}") String baseUrl,
            @Value("${app.storage.cloudinary.folder-prefix:soodfolio}") String folderPrefix,
            @Value("${app.storage.cloudinary.url:}") String cloudinaryUrl
    ) {
        String resolvedCloudName = cloudName;
        String resolvedApiKey = apiKey;
        String resolvedApiSecret = apiSecret;

        if ((resolvedCloudName == null || resolvedCloudName.isBlank()
                || resolvedApiKey == null || resolvedApiKey.isBlank()
                || resolvedApiSecret == null || resolvedApiSecret.isBlank())
                && cloudinaryUrl != null && !cloudinaryUrl.isBlank()) {
            ParsedCredentials parsed = parseCloudinaryUrl(cloudinaryUrl);
            resolvedCloudName = parsed.cloudName();
            resolvedApiKey = parsed.apiKey();
            resolvedApiSecret = parsed.apiSecret();
        }

        if (resolvedCloudName == null || resolvedCloudName.isBlank()
                || resolvedApiKey == null || resolvedApiKey.isBlank()
                || resolvedApiSecret == null || resolvedApiSecret.isBlank()) {
            throw new IllegalArgumentException("Cloudinary configuration incomplete: cloudName, apiKey, and apiSecret are required.");
        }

        this.cloudName = resolvedCloudName.trim();
        this.apiKey = resolvedApiKey.trim();
        this.apiSecret = resolvedApiSecret.trim();
        this.baseUrl = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl.replaceAll("/+$", "") : "https://api.cloudinary.com";
        this.folderPrefix = (folderPrefix != null && !folderPrefix.isBlank()) ? folderPrefix.trim() : "soodfolio";
        this.restClient = buildDefaultRestClient();
    }

    public CloudinaryFileStorageProvider(
            String cloudName,
            String apiKey,
            String apiSecret,
            String folderPrefix,
            String baseUrl,
            RestClient restClient
    ) {
        if (cloudName == null || cloudName.isBlank()
                || apiKey == null || apiKey.isBlank()
                || apiSecret == null || apiSecret.isBlank()) {
            throw new IllegalArgumentException("Cloudinary configuration incomplete: cloudName, apiKey, and apiSecret are required.");
        }

        this.cloudName = cloudName.trim();
        this.apiKey = apiKey.trim();
        this.apiSecret = apiSecret.trim();
        this.folderPrefix = (folderPrefix != null && !folderPrefix.isBlank()) ? folderPrefix.trim() : "soodfolio";
        this.baseUrl = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl.replaceAll("/+$", "") : "https://api.cloudinary.com";
        this.restClient = restClient != null ? restClient : buildDefaultRestClient();
    }

    @Override
    public String store(byte[] content, String subDir, String fileName, String contentType, boolean isImage) {
        String resourceType = isImage ? "image" : "raw";
        String endpoint = baseUrl + "/v1_1/" + cloudName + "/" + resourceType + "/upload";

        String publicId;
        if (isImage) {
            publicId = (fileName != null && fileName.contains("."))
                    ? fileName.substring(0, fileName.lastIndexOf('.'))
                    : fileName;
        } else {
            // CV PDF uses resource_type=raw and keeps .pdf extension in public_id
            publicId = fileName;
        }

        String folder = (folderPrefix != null && !folderPrefix.isBlank())
                ? folderPrefix + "/" + subDir
                : subDir;

        ByteArrayResource resource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);
        body.add("folder", folder);
        body.add("public_id", publicId);

        try {
            Map<String, Object> response = restClient.post()
                    .uri(endpoint)
                    .headers(headers -> headers.setBasicAuth(apiKey, apiSecret))
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response == null) {
                throw new RuntimeException("Phản hồi rỗng từ máy chủ Cloudinary.");
            }

            String secureUrl = (String) response.get("secure_url");
            if (secureUrl != null && !secureUrl.isBlank()) {
                log.info("File uploaded to Cloudinary successfully: {}", secureUrl);
                return secureUrl;
            }

            String url = (String) response.get("url");
            if (url != null && !url.isBlank()) {
                log.info("File uploaded to Cloudinary successfully (url): {}", url);
                return url;
            }

            throw new RuntimeException("Phản hồi Cloudinary thiếu thuộc tính secure_url hoặc url.");
        } catch (RestClientResponseException e) {
            log.error("Cloudinary upload failed: status={}", e.getStatusCode().value());
            throw new RuntimeException("Lỗi tải tệp lên Cloudinary: status=" + e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            log.error("Cloudinary connection failed: {}", e.getMessage());
            throw new RuntimeException("Lỗi kết nối máy chủ Cloudinary: " + e.getMessage(), e);
        }
    }

    private static ParsedCredentials parseCloudinaryUrl(String cloudinaryUrl) {
        if (cloudinaryUrl == null || !cloudinaryUrl.trim().startsWith("cloudinary://")) {
            throw new IllegalArgumentException("Invalid CLOUDINARY_URL format");
        }

        String raw = cloudinaryUrl.trim().substring("cloudinary://".length());
        int atIndex = raw.lastIndexOf('@');
        if (atIndex == -1) {
            throw new IllegalArgumentException("Invalid CLOUDINARY_URL format");
        }

        String creds = raw.substring(0, atIndex);
        String host = raw.substring(atIndex + 1);
        if (host.contains("/")) {
            host = host.substring(0, host.indexOf('/'));
        }
        if (host.contains("?")) {
            host = host.substring(0, host.indexOf('?'));
        }

        int colonIndex = creds.indexOf(':');
        if (colonIndex == -1) {
            throw new IllegalArgumentException("Invalid CLOUDINARY_URL format");
        }

        String key = creds.substring(0, colonIndex);
        String secret = creds.substring(colonIndex + 1);

        if (key.isBlank() || secret.isBlank() || host.isBlank()) {
            throw new IllegalArgumentException("Invalid CLOUDINARY_URL format");
        }

        return new ParsedCredentials(host.trim(), key.trim(), secret.trim());
    }

    private static RestClient buildDefaultRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(15));
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    private record ParsedCredentials(String cloudName, String apiKey, String apiSecret) {}
}
