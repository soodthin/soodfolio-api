package com.soodfolio.api.dto;

import lombok.*;

public class UploadDtos {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UploadResponseDto {
        private String url;
        private String originalFileName;
        private String fileName;
        private String contentType;
        private long size;
    }
}
