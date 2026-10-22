package com.soodfolio.api.controller;

import com.soodfolio.api.dto.UploadDtos.UploadResponseDto;
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
    @DisplayName("POST /uploads/image - 201 Created on valid image file")
    void shouldUploadImageSuccessfully() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", MediaType.IMAGE_PNG_VALUE, "dummy image data".getBytes());

        UploadResponseDto responseDto = UploadResponseDto.builder()
                .url("/uploads/images/test-uuid.png")
                .fileName("test-uuid.png")
                .originalFileName("avatar.png")
                .contentType("image/png")
                .size(16L)
                .build();

        when(fileUploadService.uploadImage(any())).thenReturn(responseDto);

        mockMvc.perform(multipart("/uploads/image").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value("/uploads/images/test-uuid.png"))
                .andExpect(jsonPath("$.contentType").value("image/png"));
    }

    @Test
    @DisplayName("POST /uploads/image - 401 Unauthorized when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", MediaType.IMAGE_PNG_VALUE, "dummy".getBytes());

        mockMvc.perform(multipart("/uploads/image")
                        .file(file)
                        .header("X-Anonymous", "true"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /uploads/resume - 201 Created on valid resume PDF")
    void shouldUploadResumeSuccessfully() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cv.pdf", MediaType.APPLICATION_PDF_VALUE, "%PDF-1.4 mock".getBytes());

        UploadResponseDto responseDto = UploadResponseDto.builder()
                .url("/uploads/resumes/resume-uuid.pdf")
                .fileName("resume-uuid.pdf")
                .originalFileName("cv.pdf")
                .contentType("application/pdf")
                .size(14L)
                .build();

        when(fileUploadService.uploadResume(any())).thenReturn(responseDto);

        mockMvc.perform(multipart("/uploads/resume").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value("/uploads/resumes/resume-uuid.pdf"));
    }

    @Test
    @DisplayName("POST /uploads with type=resume - 201 Created delegates to resume upload")
    void shouldUploadUniversalResumeSuccessfully() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cv.pdf", MediaType.APPLICATION_PDF_VALUE, "mock".getBytes());

        UploadResponseDto responseDto = UploadResponseDto.builder()
                .url("/uploads/resumes/resume-uuid.pdf")
                .build();

        when(fileUploadService.uploadResume(any())).thenReturn(responseDto);

        mockMvc.perform(multipart("/uploads")
                        .file(file)
                        .param("type", "resume"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value("/uploads/resumes/resume-uuid.pdf"));
    }
}
