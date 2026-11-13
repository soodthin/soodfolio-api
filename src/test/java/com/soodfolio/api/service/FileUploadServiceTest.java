package com.soodfolio.api.service;

import com.soodfolio.api.dto.UploadDtos.UploadResponseDto;
import com.soodfolio.api.exception.BadRequestException;
import com.soodfolio.api.service.impl.FileUploadServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileUploadServiceTest {

    @TempDir
    Path tempDir;

    private FileUploadServiceImpl fileUploadService;

    // Real Magic Bytes Fixtures
    private static final byte[] REAL_JPEG_BYTES = new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01
    };

    private static final byte[] REAL_PNG_BYTES = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
    };

    private static final byte[] REAL_GIF_BYTES = new byte[]{
            'G', 'I', 'F', '8', '9', 'a', 0x01, 0x00, 0x01, 0x00, (byte) 0x80, 0x00
    };

    private static final byte[] REAL_WEBP_BYTES = new byte[]{
            'R', 'I', 'F', 'F', 0x24, 0x00, 0x00, 0x00,
            'W', 'E', 'B', 'P', 'V', 'P', '8', ' '
    };

    private static final byte[] REAL_PDF_BYTES = new byte[]{
            '%', 'P', 'D', 'F', '-', '1', '.', '7', 0x0A,
            '%', (byte) 0xE2, (byte) 0xE3, (byte) 0xCF, (byte) 0xD3
    };

    @BeforeEach
    void setUp() {
        fileUploadService = new FileUploadServiceImpl(tempDir.toString());
    }

    @Test
    @DisplayName("Should successfully upload a valid JPEG image file with real magic bytes")
    void shouldUploadJpegImageSuccessfully() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "profile-avatar.jpg",
                "image/jpeg",
                REAL_JPEG_BYTES
        );

        UploadResponseDto response = fileUploadService.uploadImage(file);

        assertThat(response).isNotNull();
        assertThat(response.getOriginalFileName()).isEqualTo("profile-avatar.jpg");
        assertThat(response.getContentType()).isEqualTo("image/jpeg");
        assertThat(response.getUrl()).startsWith("/api/v1/uploads/images/");
        assertThat(response.getFileName()).endsWith(".jpg");

        Path savedFile = tempDir.resolve("images").resolve(response.getFileName());
        assertThat(Files.exists(savedFile)).isTrue();
    }

    @Test
    @DisplayName("Should successfully upload a valid PNG image file with real magic bytes")
    void shouldUploadPngImageSuccessfully() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "brand-logo.png",
                "image/png",
                REAL_PNG_BYTES
        );

        UploadResponseDto response = fileUploadService.uploadImage(file);

        assertThat(response).isNotNull();
        assertThat(response.getOriginalFileName()).isEqualTo("brand-logo.png");
        assertThat(response.getContentType()).isEqualTo("image/png");
        assertThat(response.getUrl()).startsWith("/api/v1/uploads/images/");
        assertThat(response.getFileName()).endsWith(".png");

        Path savedFile = tempDir.resolve("images").resolve(response.getFileName());
        assertThat(Files.exists(savedFile)).isTrue();
    }

    @Test
    @DisplayName("Should successfully upload a valid GIF image file with real magic bytes")
    void shouldUploadGifImageSuccessfully() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "project-demo.gif",
                "image/gif",
                REAL_GIF_BYTES
        );

        UploadResponseDto response = fileUploadService.uploadImage(file);

        assertThat(response).isNotNull();
        assertThat(response.getOriginalFileName()).isEqualTo("project-demo.gif");
        assertThat(response.getContentType()).isEqualTo("image/gif");
        assertThat(response.getUrl()).startsWith("/api/v1/uploads/images/");
        assertThat(response.getFileName()).endsWith(".gif");

        Path savedFile = tempDir.resolve("images").resolve(response.getFileName());
        assertThat(Files.exists(savedFile)).isTrue();
    }

    @Test
    @DisplayName("Should successfully upload a valid WEBP image file with real magic bytes")
    void shouldUploadWebpImageSuccessfully() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "gallery-cover.webp",
                "image/webp",
                REAL_WEBP_BYTES
        );

        UploadResponseDto response = fileUploadService.uploadImage(file);

        assertThat(response).isNotNull();
        assertThat(response.getOriginalFileName()).isEqualTo("gallery-cover.webp");
        assertThat(response.getContentType()).isEqualTo("image/webp");
        assertThat(response.getUrl()).startsWith("/api/v1/uploads/images/");
        assertThat(response.getFileName()).endsWith(".webp");

        Path savedFile = tempDir.resolve("images").resolve(response.getFileName());
        assertThat(Files.exists(savedFile)).isTrue();
    }

    @Test
    @DisplayName("Should successfully upload a valid PDF resume file with real magic bytes")
    void shouldUploadPdfResumeSuccessfully() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "my_cv_2026.pdf",
                "application/pdf",
                REAL_PDF_BYTES
        );

        UploadResponseDto response = fileUploadService.uploadResume(file);

        assertThat(response).isNotNull();
        assertThat(response.getOriginalFileName()).isEqualTo("my_cv_2026.pdf");
        assertThat(response.getContentType()).isEqualTo("application/pdf");
        assertThat(response.getUrl()).startsWith("/api/v1/uploads/resumes/");
        assertThat(response.getFileName()).endsWith(".pdf");

        Path savedFile = tempDir.resolve("resumes").resolve(response.getFileName());
        assertThat(Files.exists(savedFile)).isTrue();
    }

    @Test
    @DisplayName("Should throw BadRequestException when uploaded file is empty")
    void shouldThrowBadRequestWhenFileIsEmpty() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.png",
                "image/png",
                new byte[0]
        );

        assertThatThrownBy(() -> fileUploadService.uploadImage(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("không được để trống");
    }

    @Test
    @DisplayName("Should throw BadRequestException when image has disallowed MIME type")
    void shouldThrowBadRequestWhenImageHasInvalidMimeType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "script.sh",
                "application/x-sh",
                "echo 'hacked'".getBytes()
        );

        assertThatThrownBy(() -> fileUploadService.uploadImage(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Định dạng ảnh không hợp lệ");
    }

    @Test
    @DisplayName("Should throw BadRequestException when image has disallowed extension")
    void shouldThrowBadRequestWhenImageHasInvalidExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "image.exe",
                "image/jpeg",
                REAL_JPEG_BYTES
        );

        assertThatThrownBy(() -> fileUploadService.uploadImage(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Phần mở rộng tệp không được hỗ trợ");
    }

    @Test
    @DisplayName("Should throw BadRequestException when image exceeds max allowed size (5MB)")
    void shouldThrowBadRequestWhenImageExceedsMaxSize() {
        byte[] oversizedData = new byte[6 * 1024 * 1024]; // 6 MB
        System.arraycopy(REAL_PNG_BYTES, 0, oversizedData, 0, REAL_PNG_BYTES.length);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.png",
                "image/png",
                oversizedData
        );

        assertThatThrownBy(() -> fileUploadService.uploadImage(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Kích thước tệp vượt quá");
    }

    @Test
    @DisplayName("Should throw BadRequestException when resume is not a PDF")
    void shouldThrowBadRequestWhenResumeIsNotPdf() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "doc data".getBytes()
        );

        assertThatThrownBy(() -> fileUploadService.uploadResume(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Định dạng CV / Resume không hợp lệ");
    }

    @Test
    @DisplayName("Should sanitize filename and prevent path traversal attack")
    void shouldSanitizeFilenameAgainstPathTraversal() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../../../../etc/passwd.png",
                "image/png",
                REAL_PNG_BYTES
        );

        UploadResponseDto response = fileUploadService.uploadImage(file);

        assertThat(response.getFileName()).doesNotContain("..");
        assertThat(response.getFileName()).doesNotContain("/");
        assertThat(response.getFileName()).doesNotContain("\\");
        Path savedFile = tempDir.resolve("images").resolve(response.getFileName());
        assertThat(Files.exists(savedFile)).isTrue();
    }

    @Test
    @DisplayName("Should throw BadRequestException when SVG file is uploaded")
    void shouldRejectSvgUpload() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "malicious.svg",
                "image/svg+xml",
                "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>".getBytes()
        );

        assertThatThrownBy(() -> fileUploadService.uploadImage(file))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Should throw BadRequestException when MIME spoofing: HTML script content with .png extension")
    void shouldRejectMimeSpoofingScriptContentWithPngExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "<script>alert('xss')</script>".getBytes()
        );

        assertThatThrownBy(() -> fileUploadService.uploadImage(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Magic Bytes");
    }

    @Test
    @DisplayName("Should throw BadRequestException when MIME spoofing: PHP payload with .jpg extension")
    void shouldRejectMimeSpoofingPhpContentWithJpgExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "shell.jpg",
                "image/jpeg",
                "<?php system($_GET['cmd']); ?>".getBytes()
        );

        assertThatThrownBy(() -> fileUploadService.uploadImage(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Magic Bytes");
    }

    @Test
    @DisplayName("Should throw BadRequestException when MIME spoofing: Bash script with .pdf extension")
    void shouldRejectMimeSpoofingBashScriptWithPdfExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                "#!/bin/bash\nrm -rf /".getBytes()
        );

        assertThatThrownBy(() -> fileUploadService.uploadResume(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Magic Bytes");
    }

    @Test
    @DisplayName("Should throw BadRequestException when Extension spoofing: PNG bytes disguised with .jpg extension")
    void shouldRejectExtensionSpoofingPngBytesWithJpgExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "spoofed.jpg",
                "image/jpeg",
                REAL_PNG_BYTES
        );

        assertThatThrownBy(() -> fileUploadService.uploadImage(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Magic Bytes");
    }

    @Test
    @DisplayName("Should throw BadRequestException when Extension spoofing: GIF bytes disguised with .webp extension")
    void shouldRejectExtensionSpoofingGifBytesWithWebpExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "spoofed.webp",
                "image/webp",
                REAL_GIF_BYTES
        );

        assertThatThrownBy(() -> fileUploadService.uploadImage(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Magic Bytes");
    }

    @Test
    @DisplayName("Should throw BadRequestException when magic bytes do not match declared extension")
    void shouldRejectMismatchedMagicBytes() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fake.jpg",
                "image/jpeg",
                "NOT_A_JPEG".getBytes()
        );

        assertThatThrownBy(() -> fileUploadService.uploadImage(file))
                .isInstanceOf(BadRequestException.class);
    }
}
