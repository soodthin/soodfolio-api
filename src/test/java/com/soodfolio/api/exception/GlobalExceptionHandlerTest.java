package com.soodfolio.api.exception;

import com.soodfolio.api.dto.ErrorResponseDto;
import com.soodfolio.api.service.I18nService;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private I18nService i18nService;

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/portfolios/me");
    }

    @Test
    @DisplayName("Should return 409 Conflict when ObjectOptimisticLockingFailureException is handled")
    void shouldReturn409ConflictWhenObjectOptimisticLockingFailureExceptionHandled() {
        String conflictMessage = "Dữ liệu đã bị thay đổi bởi một phiên khác. Vui lòng tải lại trang và thử lại.";
        when(i18nService.getMessage("common.concurrency_conflict")).thenReturn(conflictMessage);

        ObjectOptimisticLockingFailureException ex =
                new ObjectOptimisticLockingFailureException("Portfolio", "portfolio-uuid-123");

        ResponseEntity<ErrorResponseDto> response = globalExceptionHandler.handleOptimisticLockingFailure(ex, request);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getError()).isEqualTo("Conflict");
        assertThat(response.getBody().getMessage()).isEqualTo(conflictMessage);
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/portfolios/me");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should return 409 Conflict when OptimisticLockException is handled")
    void shouldReturn409ConflictWhenOptimisticLockExceptionHandled() {
        String conflictMessage = "Data was modified by another session. Please reload and try again.";
        when(i18nService.getMessage("common.concurrency_conflict")).thenReturn(conflictMessage);

        OptimisticLockException ex = new OptimisticLockException("Row was updated or deleted by another transaction");

        ResponseEntity<ErrorResponseDto> response = globalExceptionHandler.handleOptimisticLockingFailure(ex, request);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getMessage()).isEqualTo(conflictMessage);
    }

    @Test
    @DisplayName("Should return 404 Not Found when ResourceNotFoundException is handled")
    void shouldReturn404NotFoundWhenResourceNotFoundExceptionHandled() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Project not found with id: 'abc'");
        when(i18nService.getMessage("Project not found with id: 'abc'"))
                .thenReturn("Project not found with id: 'abc'");

        ResponseEntity<ErrorResponseDto> response = globalExceptionHandler.handleBaseApiException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getError()).isEqualTo("Not Found");
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when BadCredentialsException is handled")
    void shouldReturn401UnauthorizedWhenBadCredentialsHandled() {
        when(i18nService.getMessage("auth.invalid_credentials"))
                .thenReturn("Email hoặc mật khẩu không chính xác.");

        ResponseEntity<ErrorResponseDto> response = globalExceptionHandler.handleBadCredentials(
                new BadCredentialsException("Bad credentials"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(401);
        assertThat(response.getBody().getMessage()).isEqualTo("Email hoặc mật khẩu không chính xác.");
    }

    @Test
    @DisplayName("Should return 403 Forbidden when AccessDeniedException is handled")
    void shouldReturn403ForbiddenWhenAccessDeniedHandled() {
        when(i18nService.getMessage("auth.access_denied"))
                .thenReturn("Bạn không có quyền truy cập vào tài nguyên này.");

        ResponseEntity<ErrorResponseDto> response = globalExceptionHandler.handleAccessDenied(
                new AccessDeniedException("Forbidden"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(403);
        assertThat(response.getBody().getMessage()).isEqualTo("Bạn không có quyền truy cập vào tài nguyên này.");
    }

    @Test
    @DisplayName("Should return 400 Bad Request when MaxUploadSizeExceededException is handled")
    void shouldReturn400BadRequestWhenMaxUploadSizeExceededHandled() {
        when(i18nService.getMessage("upload.server_size_exceeded"))
                .thenReturn("Kích thước tệp vượt quá giới hạn cấu hình của máy chủ.");

        org.springframework.web.multipart.MaxUploadSizeExceededException ex =
                new org.springframework.web.multipart.MaxUploadSizeExceededException(10485760);

        ResponseEntity<ErrorResponseDto> response = globalExceptionHandler.handleMaxUploadSizeExceeded(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("Kích thước tệp vượt quá giới hạn cấu hình của máy chủ.");
    }

    @Test
    @DisplayName("Should return 500 Internal Server Error when unexpected generic Exception occurs")
    void shouldReturn500InternalServerErrorWhenGenericExceptionHandled() {
        when(i18nService.getMessage("common.server_error"))
                .thenReturn("Đã xảy ra lỗi hệ thống. Vui lòng liên hệ quản trị viên.");

        RuntimeException ex = new RuntimeException("Unexpected NPE");

        ResponseEntity<ErrorResponseDto> response = globalExceptionHandler.handleGlobalException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getMessage()).isEqualTo("Đã xảy ra lỗi hệ thống. Vui lòng liên hệ quản trị viên.");
    }

    @Test
    @DisplayName("Should return 404 Not Found when NoResourceFoundException is handled")
    void shouldReturn404NotFoundWhenNoResourceFoundExceptionHandled() {
        org.springframework.web.servlet.resource.NoResourceFoundException ex =
                new org.springframework.web.servlet.resource.NoResourceFoundException(
                        org.springframework.http.HttpMethod.GET, "api/v1/unknown");

        ResponseEntity<ErrorResponseDto> response = globalExceptionHandler.handleNoResourceFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getError()).isEqualTo("Not Found");
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/portfolios/me");
    }
}

