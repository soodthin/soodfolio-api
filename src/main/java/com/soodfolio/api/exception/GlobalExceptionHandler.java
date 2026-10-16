package com.soodfolio.api.exception;

import com.soodfolio.api.dto.ErrorResponseDto;
import com.soodfolio.api.service.I18nService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final I18nService i18nService;

    // 1. LSP & OCP: Catch any BaseApiException subclass generically with localization support
    @ExceptionHandler(BaseApiException.class)
    public ResponseEntity<ErrorResponseDto> handleBaseApiException(
            BaseApiException ex, HttpServletRequest request) {
        log.warn("Application business exception [{}]: {}", ex.getStatus(), ex.getMessage());
        String localizedMessage = i18nService.getMessage(ex.getMessage());
        return buildErrorResponse(ex.getStatus(), localizedMessage, request);
    }

    // 2. 400 Bad Request - Validation Failures (@Valid) with localized error messages
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });

        ErrorResponseDto errorDto = ErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(i18nService.getMessage("common.validation_failed"))
                .path(request.getRequestURI())
                .validationErrors(validationErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDto);
    }

    // 3. 401 Unauthorized - BadCredentialsException from Spring Security
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDto> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, i18nService.getMessage("auth.invalid_credentials"), request);
    }

    // 4. 403 Forbidden - AccessDeniedException
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, i18nService.getMessage("auth.access_denied"), request);
    }

    // 5. 400 Bad Request - File Upload size exceeded
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponseDto> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.warn("Uploaded file exceeds maximum allowed size: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, i18nService.getMessage("upload.server_size_exceeded"), request);
    }

    // 6. 500 Internal Server Error - Catch-all for unhandled unexpected exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGlobalException(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled server error: ", ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, i18nService.getMessage("common.server_error"), request);
    }

    private ResponseEntity<ErrorResponseDto> buildErrorResponse(
            HttpStatus status, String message, HttpServletRequest request) {
        ErrorResponseDto errorDto = ErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(status).body(errorDto);
    }
}
