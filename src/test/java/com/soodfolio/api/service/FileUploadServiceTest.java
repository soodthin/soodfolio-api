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

    @BeforeEach
    void setUp() {
        fileUploadService = new FileUploadServiceImpl(tempDir.toString());
    }

    @Test
    @DisplayName("Should successfully upload a valid image file")
    void shouldUploadImageSuccessfully() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "profile-avatar.jpg",
                "image/jpeg",
                "fake-image-binary-data".getBytes()
        );

        UploadResponseDto response = fileUploadService.uploadImage(file);

        assertThat(response).isNotNull();
        assertThat(response.getOriginalFileName()).isEqualTo("profile-avatar.jpg");
        assertThat(response.getContentType()).isEqualTo("image/jpeg");
        assertThat(response.getUrl()).startsWith("/uploads/images/");
        assertThat(response.getFileName()).endsWith(".jpg");

        Path savedFile = tempDir.resolve("images").resolve(response.getFileName());
        assertThat(Files.exists(savedFile)).isTrue();
    }

    @Test
    @DisplayName("Should successfully upload a valid PDF resume file")
    void shouldUploadResumeSuccessfully() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "my_cv_2026.pdf",
                "application/pdf",
                "%PDF-1.5 fake pdf content".getBytes()
        );

        UploadResponseDto response = fileUploadService.uploadResume(file);

        assertThat(response).isNotNull();
        assertThat(response.getOriginalFileName()).isEqualTo("my_cv_2026.pdf");
        assertThat(response.getContentType()).isEqualTo("application/pdf");
        assertThat(response.getUrl()).startsWith("/uploads/resumes/");
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
                "malware".getBytes()
        );

        assertThatThrownBy(() -> fileUploadService.uploadImage(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Phần mở rộng tệp không được hỗ trợ");
    }

    @Test
    @DisplayName("Should throw BadRequestException when image exceeds max allowed size (5MB)")
    void shouldThrowBadRequestWhenImageExceedsMaxSize() {
        byte[] oversizedData = new byte[6 * 1024 * 1024]; // 6 MB
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
                "safe content".getBytes()
        );

        UploadResponseDto response = fileUploadService.uploadImage(file);

        assertThat(response.getFileName()).doesNotContain("..");
        assertThat(response.getFileName()).doesNotContain("/");
        assertThat(response.getFileName()).doesNotContain("\\");
        Path savedFile = tempDir.resolve("images").resolve(response.getFileName());
        assertThat(Files.exists(savedFile)).isTrue();
    }
}
