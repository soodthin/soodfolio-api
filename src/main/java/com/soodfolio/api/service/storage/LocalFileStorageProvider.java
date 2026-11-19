package com.soodfolio.api.service.storage;

import com.soodfolio.api.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageProvider implements FileStorageProvider {

    private final Path rootUploadPath;

    public LocalFileStorageProvider(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this(Paths.get(uploadDir).toAbsolutePath().normalize());
    }

    public LocalFileStorageProvider(Path rootUploadPath) {
        this.rootUploadPath = rootUploadPath;
    }

    @Override
    public String store(byte[] content, String subDir, String fileName, String contentType, boolean isImage) {
        try {
            Path targetDir = rootUploadPath.resolve(subDir).normalize();
            if (!targetDir.startsWith(rootUploadPath)) {
                throw new BadRequestException("Đường dẫn lưu trữ không hợp lệ: " + subDir);
            }
            Files.createDirectories(targetDir);

            Path targetFile = targetDir.resolve(fileName).normalize();
            if (!targetFile.startsWith(targetDir)) {
                throw new BadRequestException("Đường dẫn tệp không hợp lệ: " + fileName);
            }

            Files.write(targetFile, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            String relativeUrl = "/api/v1/uploads/" + subDir + "/" + fileName;
            log.info("File stored locally: {}", relativeUrl);
            return relativeUrl;
        } catch (IOException e) {
            log.error("Failed to store file locally: ", e);
            throw new RuntimeException("Lỗi trong quá trình lưu trữ tệp cục bộ: " + e.getMessage(), e);
        }
    }
}
