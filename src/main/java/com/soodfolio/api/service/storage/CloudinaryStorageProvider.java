package com.soodfolio.api.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
public class CloudinaryStorageProvider implements FileStorageProvider {

    private final String apiKey;
    private final String apiSecret;
    private final String cloudName;
    private final RestClient restClient;
    private final Supplier<Long> timestampSupplier;

    public CloudinaryStorageProvider(String cloudinaryUrl) {
        this(cloudinaryUrl, buildDefaultRestClient(), () -> Instant.now().getEpochSecond());
    }

    public CloudinaryStorageProvider(String cloudinaryUrl, RestClient restClient) {
        this(cloudinaryUrl, restClient, () -> Instant.now().getEpochSecond());
    }

    public CloudinaryStorageProvider(String cloudinaryUrl, RestClient restClient, Supplier<Long> timestampSupplier) {
        if (cloudinaryUrl == null || !cloudinaryUrl.trim().startsWith("cloudinary://")) {
            throw new IllegalArgumentException("Định dạng CLOUDINARY_URL không hợp lệ: " + cloudinaryUrl);
        }

        String raw = cloudinaryUrl.trim().substring("cloudinary://".length());
        int atIndex = raw.lastIndexOf('@');
        if (atIndex == -1) {
            throw new IllegalArgumentException("CLOUDINARY_URL thiếu ký tự '@': " + cloudinaryUrl);
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
            throw new IllegalArgumentException("CLOUDINARY_URL thiếu thông tin xác thực api_key:api_secret");
        }

        this.apiKey = creds.substring(0, colonIndex);
        this.apiSecret = creds.substring(colonIndex + 1);
        this.cloudName = host;
        this.restClient = restClient != null ? restClient : buildDefaultRestClient();
        this.timestampSupplier = timestampSupplier != null ? timestampSupplier : () -> Instant.now().getEpochSecond();
    }

    @Override
    public String store(byte[] content, String filename, String contentType, String subDir) {
        boolean isRaw = "resumes".equalsIgnoreCase(subDir)
                || (contentType != null && "application/pdf".equalsIgnoreCase(contentType));

        String endpoint = isRaw
                ? "https://api.cloudinary.com/v1_1/" + cloudName + "/raw/upload"
                : "https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload";

        String publicId;
        if (isRaw) {
            // Giữ đuôi .pdf trong public_id để tránh lỗi gói Free của Cloudinary
            publicId = filename;
        } else {
            publicId = filename != null && filename.contains(".")
                    ? filename.substring(0, filename.lastIndexOf('.'))
                    : filename;
        }

        String folder = subDir;
        long timestamp = timestampSupplier.get();
        String signature = computeSignature(folder, publicId, timestamp);

        ByteArrayResource resource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);
        body.add("api_key", apiKey);
        body.add("timestamp", String.valueOf(timestamp));
        body.add("folder", folder);
        body.add("public_id", publicId);
        body.add("signature", signature);

        try {
            Map<String, Object> response = restClient.post()
                    .uri(endpoint)
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
        } catch (RestClientException e) {
            log.error("Cloudinary upload failed: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi tải tệp lên Cloudinary: " + e.getMessage(), e);
        }
    }

    private String computeSignature(String folder, String publicId, long timestamp) {
        // Sắp xếp alphabet các param (folder, public_id, timestamp), nối api_secret (không có & trước api_secret)
        String stringToSign = "folder=" + folder + "&public_id=" + publicId + "&timestamp=" + timestamp + apiSecret;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(stringToSign.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Thuật toán SHA-1 không khả dụng", e);
        }
    }

    private static RestClient buildDefaultRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(15));
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }
}
