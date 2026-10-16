package com.soodfolio.api.service;

import com.soodfolio.api.dto.UploadDtos.UploadResponseDto;
import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {
    UploadResponseDto uploadImage(MultipartFile file);
    UploadResponseDto uploadResume(MultipartFile file);
}
