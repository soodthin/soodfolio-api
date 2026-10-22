package com.soodfolio.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

public class SnapshotDtos {

    @Data
    @Builder
    public static class CreateSnapshotRequest {
        @Size(max = 255, message = "Changelog must not exceed 255 characters")
        @Schema(description = "Description of changes in this version", example = "Updated work experience and added SoodFolio project")
        private String changelog;
    }

    @Data
    @Builder
    public static class SnapshotResponseDto {
        private UUID id;
        private UUID portfolioId;
        private int version;
        private String changelog;
        private LocalDateTime createdAt;
        private PortfolioResponseDto data;
    }

    @Data
    @Builder
    public static class SnapshotSummaryDto {
        private UUID id;
        private int version;
        private String changelog;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    public static class RollbackResponseDto {
        private String message;
        private int restoredVersion;
        private PortfolioResponseDto portfolio;
    }
}
