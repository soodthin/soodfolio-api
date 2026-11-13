package com.soodfolio.api.service.storage;

import com.soodfolio.api.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Slf4j
public class LocalFileStorageProvider implements FileStorageProvider {

    private final Path rootUploadPath;

    public LocalFileStorageProvider(Path rootUploadPath) {
        this.rootUploadPath = rootUploadPath;
    }

    @Override
    public String store(byte[] content, String filename, String contentType, String subDir) {
        try {
            Path targetDir = rootUploadPath.resolve(subDir).normalize();
            if (!targetDir.startsWith(rootUploadPath)) {
                throw new BadRequestException("Đường dẫn lưu trữ không hợp lệ: " + subDir);
            }
            Files.createDirectories(targetDir);

            Path targetFile = targetDir.resolve(filename).normalize();
            if (!targetFile.startsWith(targetDir)) {
                throw new BadRequestException("Đường dẫn tệp không hợp lệ: " + filename);
            }

            Files.write(targetFile, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            String relativeUrl = "/api/v1/uploads/" + subDir + "/" + filename;
            log.info("File stored locally: {}", relativeUrl);
            return relativeUrl;
        } catch (IOException e) {
            log.error("Failed to store file locally: ", e);
            throw new RuntimeException("Lỗi trong quá trình lưu trữ tệp cục bộ: " + e.getMessage(), e);
        }
    }
}
