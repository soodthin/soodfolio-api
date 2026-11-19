package com.soodfolio.api.controller;

import com.soodfolio.api.dto.UploadDtos.UploadResponseDto;
import com.soodfolio.api.exception.BadRequestException;
import com.soodfolio.api.exception.GlobalExceptionHandler;
import com.soodfolio.api.security.UserPrincipal;
import com.soodfolio.api.service.FileUploadService;
import com.soodfolio.api.service.I18nService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UploadControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FileUploadService fileUploadService;

    @Mock
    private I18nService i18nService;

    @InjectMocks
    private UploadController uploadController;

    private UserPrincipal testPrincipal;

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
        testPrincipal = new UserPrincipal(
                UUID.randomUUID(),
                "creator@soodfolio.dev",
                "password",
                "Creator",
                org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_USER")
        );

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                if ("true".equalsIgnoreCase(webRequest.getHeader("X-Anonymous"))) {
                    return null;
                }
                return testPrincipal;
            }
        };

        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler(i18nService);

        mockMvc = MockMvcBuilders.standaloneSetup(uploadController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .setControllerAdvice(exceptionHandler)
                .build();

        lenient().when(i18nService.getMessage(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("POST /uploads/image - 201 Created on valid PNG image with real magic bytes")
    void shouldUploadPngImageSuccessfully() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", MediaType.IMAGE_PNG_VALUE, REAL_PNG_BYTES);

        UploadResponseDto responseDto = UploadResponseDto.builder()
                .url("/api/v1/uploads/images/test-uuid.png")
                .fileName("test-uuid.png")
                .originalFileName("avatar.png")
                .contentType("image/png")
                .size((long) REAL_PNG_BYTES.length)
                .build();

        when(fileUploadService.uploadImage(any())).thenReturn(responseDto);

        mockMvc.perform(multipart("/uploads/image").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value(startsWith("/api/v1/uploads/images/")))
                .andExpect(jsonPath("$.url").value("/api/v1/uploads/images/test-uuid.png"))
                .andExpect(jsonPath("$.contentType").value("image/png"));
    }

    @Test
    @DisplayName("POST /uploads/image - 201 Created on valid JPEG image with real magic bytes")
    void shouldUploadJpegImageSuccessfully() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, REAL_JPEG_BYTES);

        UploadResponseDto responseDto = UploadResponseDto.builder()
                .url("/api/v1/uploads/images/photo-uuid.jpg")
                .fileName("photo-uuid.jpg")
                .originalFileName("photo.jpg")
                .contentType("image/jpeg")
                .size((long) REAL_JPEG_BYTES.length)
                .build();

        when(fileUploadService.uploadImage(any())).thenReturn(responseDto);

        mockMvc.perform(multipart("/uploads/image").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value(startsWith("/api/v1/uploads/images/")))
                .andExpect(jsonPath("$.url").value("/api/v1/uploads/images/photo-uuid.jpg"))
                .andExpect(jsonPath("$.contentType").value("image/jpeg"));
    }

    @Test
    @DisplayName("POST /uploads/image - 201 Created on valid GIF image with real magic bytes")
    void shouldUploadGifImageSuccessfully() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "animation.gif", MediaType.IMAGE_GIF_VALUE, REAL_GIF_BYTES);

        UploadResponseDto responseDto = UploadResponseDto.builder()
                .url("/api/v1/uploads/images/anim-uuid.gif")
                .fileName("anim-uuid.gif")
                .originalFileName("animation.gif")
                .contentType("image/gif")
                .size((long) REAL_GIF_BYTES.length)
                .build();

        when(fileUploadService.uploadImage(any())).thenReturn(responseDto);

        mockMvc.perform(multipart("/uploads/image").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value(startsWith("/api/v1/uploads/images/")))
                .andExpect(jsonPath("$.url").value("/api/v1/uploads/images/anim-uuid.gif"));
    }

    @Test
    @DisplayName("POST /uploads/image - 201 Created on valid WEBP image with real magic bytes")
    void shouldUploadWebpImageSuccessfully() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "banner.webp", "image/webp", REAL_WEBP_BYTES);

        UploadResponseDto responseDto = UploadResponseDto.builder()
                .url("/api/v1/uploads/images/banner-uuid.webp")
                .fileName("banner-uuid.webp")
                .originalFileName("banner.webp")
                .contentType("image/webp")
                .size((long) REAL_WEBP_BYTES.length)
                .build();

        when(fileUploadService.uploadImage(any())).thenReturn(responseDto);

        mockMvc.perform(multipart("/uploads/image").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value(startsWith("/api/v1/uploads/images/")))
                .andExpect(jsonPath("$.url").value("/api/v1/uploads/images/banner-uuid.webp"));
    }

    @Test
    @DisplayName("POST /uploads/image - 401 Unauthorized when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", MediaType.IMAGE_PNG_VALUE, REAL_PNG_BYTES);

        mockMvc.perform(multipart("/uploads/image")
                        .file(file)
                        .header("X-Anonymous", "true"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /uploads/image - 400 Bad Request on MIME/Extension spoofing rejection")
    void shouldReturn400OnMimeSpoofing() throws Exception {
        MockMultipartFile spoofedFile = new MockMultipartFile(
                "file", "malicious.png", MediaType.IMAGE_PNG_VALUE, "<script>alert(1)</script>".getBytes());

        when(fileUploadService.uploadImage(any()))
                .thenThrow(new BadRequestException("Cấu trúc dữ liệu tệp (Magic Bytes) không khớp với định dạng cho phép: .png"));

        mockMvc.perform(multipart("/uploads/image").file(spoofedFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Cấu trúc dữ liệu tệp (Magic Bytes) không khớp với định dạng cho phép: .png"));
    }

    @Test
    @DisplayName("POST /uploads/resume - 201 Created on valid resume PDF with real magic bytes")
    void shouldUploadResumeSuccessfully() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cv.pdf", MediaType.APPLICATION_PDF_VALUE, REAL_PDF_BYTES);

        UploadResponseDto responseDto = UploadResponseDto.builder()
                .url("/api/v1/uploads/resumes/resume-uuid.pdf")
                .fileName("resume-uuid.pdf")
                .originalFileName("cv.pdf")
                .contentType("application/pdf")
                .size((long) REAL_PDF_BYTES.length)
                .build();

        when(fileUploadService.uploadResume(any())).thenReturn(responseDto);

        mockMvc.perform(multipart("/uploads/resume").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value(startsWith("/api/v1/uploads/resumes/")))
                .andExpect(jsonPath("$.url").value("/api/v1/uploads/resumes/resume-uuid.pdf"));
    }

    @Test
    @DisplayName("POST /uploads with type=resume - 201 Created delegates to resume upload")
    void shouldUploadUniversalResumeSuccessfully() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cv.pdf", MediaType.APPLICATION_PDF_VALUE, REAL_PDF_BYTES);

        UploadResponseDto responseDto = UploadResponseDto.builder()
                .url("/api/v1/uploads/resumes/resume-uuid.pdf")
                .build();

        when(fileUploadService.uploadResume(any())).thenReturn(responseDto);

        mockMvc.perform(multipart("/uploads")
                        .file(file)
                        .param("type", "resume"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value(startsWith("/api/v1/uploads/resumes/")))
                .andExpect(jsonPath("$.url").value("/api/v1/uploads/resumes/resume-uuid.pdf"));
    }

    @Test
    @DisplayName("POST /uploads with type=image - 201 Created delegates to image upload")
    void shouldUploadUniversalImageSuccessfully() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.png", MediaType.IMAGE_PNG_VALUE, REAL_PNG_BYTES);

        UploadResponseDto responseDto = UploadResponseDto.builder()
                .url("/api/v1/uploads/images/universal-uuid.png")
                .build();

        when(fileUploadService.uploadImage(any())).thenReturn(responseDto);

        mockMvc.perform(multipart("/uploads")
                        .file(file)
                        .param("type", "image"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value(startsWith("/api/v1/uploads/images/")))
                .andExpect(jsonPath("$.url").value("/api/v1/uploads/images/universal-uuid.png"));
    }
}
