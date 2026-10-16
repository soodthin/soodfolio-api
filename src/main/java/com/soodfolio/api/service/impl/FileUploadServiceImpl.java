package com.soodfolio.api.service.impl;

import com.soodfolio.api.dto.UploadDtos.UploadResponseDto;
import com.soodfolio.api.exception.BadRequestException;
import com.soodfolio.api.service.FileUploadService;
import com.soodfolio.api.service.I18nService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileUploadServiceImpl implements FileUploadService {

    private static final List<String> ALLOWED_IMAGE_MIMES = List.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = List.of(
            ".jpg",
            ".jpeg",
            ".png",
            ".webp",
            ".gif"
    );

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5 MB

    private static final List<String> ALLOWED_RESUME_MIMES = List.of(
            "application/pdf"
    );

    private static final List<String> ALLOWED_RESUME_EXTENSIONS = List.of(
            ".pdf"
    );

    private static final long MAX_RESUME_SIZE = 10 * 1024 * 1024; // 10 MB

    private final Path rootUploadPath;
    private final I18nService i18nService;

    public FileUploadServiceImpl(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this(uploadDir, null);
    }

    @Autowired
    public FileUploadServiceImpl(
            @Value("${app.upload.dir:uploads}") String uploadDir,
            @Autowired(required = false) I18nService i18nService
    ) {
        this.rootUploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.i18nService = i18nService;
    }

    @Override
    public UploadResponseDto uploadImage(MultipartFile file) {
        return processUpload(
                file,
                "images",
                ALLOWED_IMAGE_MIMES,
                ALLOWED_IMAGE_EXTENSIONS,
                MAX_IMAGE_SIZE,
                "upload.image_invalid",
                "Định dạng ảnh không hợp lệ. Chỉ hỗ trợ JPEG, PNG, WEBP, GIF."
        );
    }

    @Override
    public UploadResponseDto uploadResume(MultipartFile file) {
        return processUpload(
                file,
                "resumes",
                ALLOWED_RESUME_MIMES,
                ALLOWED_RESUME_EXTENSIONS,
                MAX_RESUME_SIZE,
                "upload.resume_invalid",
                "Định dạng CV / Resume không hợp lệ. Chỉ hỗ trợ PDF."
        );
    }

    private UploadResponseDto processUpload(
            MultipartFile file,
            String subDirName,
            List<String> allowedMimes,
            List<String> allowedExtensions,
            long maxSizeBytes,
            String mimeCode,
            String mimeFallback
    ) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException(msg("upload.file_empty", "Tệp tải lên không được để trống"));
        }

        if (file.getSize() > maxSizeBytes) {
            long maxMb = maxSizeBytes / (1024 * 1024);
            throw new BadRequestException(msg("upload.size_exceeded", "Kích thước tệp vượt quá giới hạn tối đa ({0}MB)", maxMb));
        }

        String contentType = file.getContentType();
        if (contentType == null || !allowedMimes.contains(contentType.toLowerCase())) {
            throw new BadRequestException(msg(mimeCode, mimeFallback));
        }

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        String extension = getFileExtension(originalFileName).toLowerCase();
        if (!allowedExtensions.contains(extension)) {
            throw new BadRequestException(msg("upload.extension_unsupported", "Phần mở rộng tệp không được hỗ trợ: {0}", extension));
        }

        // Anti-Path Traversal & Collision: Random UUID naming
        String newFileName = UUID.randomUUID() + extension;

        try {
            Path targetDir = rootUploadPath.resolve(subDirName).normalize();
            if (!targetDir.startsWith(rootUploadPath)) {
                throw new BadRequestException(msg("upload.storage_error", "Đường dẫn lưu trữ không hợp lệ: {0}", "Invalid path"));
            }
            Files.createDirectories(targetDir);

            Path targetFile = targetDir.resolve(newFileName).normalize();
            if (!targetFile.startsWith(targetDir)) {
                throw new BadRequestException(msg("upload.storage_error", "Đường dẫn tệp không hợp lệ: {0}", "Invalid path"));
            }

            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);

            String relativeUrl = "/uploads/" + subDirName + "/" + newFileName;

            log.info("File uploaded successfully: {} -> {}", originalFileName, relativeUrl);

            return UploadResponseDto.builder()
                    .url(relativeUrl)
                    .originalFileName(originalFileName)
                    .fileName(newFileName)
                    .contentType(contentType)
                    .size(file.getSize())
                    .build();
        } catch (IOException e) {
            log.error("Failed to store file: ", e);
            throw new RuntimeException(msg("upload.storage_error", "Lỗi trong quá trình lưu trữ tệp: {0}", e.getMessage()), e);
        }
    }

    private String msg(String code, String fallback, Object... args) {
        if (i18nService != null) {
            return i18nService.getMessage(code, args);
        }
        return MessageFormat.format(fallback, args);
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1 || lastDot == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDot);
    }
}
