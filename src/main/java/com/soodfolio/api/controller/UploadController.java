package com.soodfolio.api.controller;

import com.soodfolio.api.dto.UploadDtos.UploadResponseDto;
import com.soodfolio.api.exception.InvalidCredentialsException;
import com.soodfolio.api.security.UserPrincipal;
import com.soodfolio.api.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/uploads")
@RequiredArgsConstructor
@Tag(name = "Uploads", description = "Media & File Upload APIs for Creators")
@SecurityRequirement(name = "bearerAuth")
public class UploadController {

    private final FileUploadService fileUploadService;

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload image (avatar, project cover/gallery, max 5MB, JPEG/PNG/WEBP/GIF)")
    public ResponseEntity<UploadResponseDto> uploadImage(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam("file") MultipartFile file
    ) {
        validateAuthenticated(userPrincipal);
        UploadResponseDto response = fileUploadService.uploadImage(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload resume / CV (PDF format only, max 10MB)")
    public ResponseEntity<UploadResponseDto> uploadResume(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam("file") MultipartFile file
    ) {
        validateAuthenticated(userPrincipal);
        UploadResponseDto response = fileUploadService.uploadResume(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Universal upload endpoint with type selector (image | resume)")
    public ResponseEntity<UploadResponseDto> uploadFile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "image") String type
    ) {
        validateAuthenticated(userPrincipal);
        UploadResponseDto response;
        if ("resume".equalsIgnoreCase(type) || "cv".equalsIgnoreCase(type)) {
            response = fileUploadService.uploadResume(file);
        } else {
            response = fileUploadService.uploadImage(file);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private void validateAuthenticated(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new InvalidCredentialsException("Yêu cầu đăng nhập để thực hiện tải lên tệp tin.");
        }
    }
}
