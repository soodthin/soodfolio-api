package com.soodfolio.api.dto;

import lombok.*;

public class AnalyticsDtos {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecordViewRequestDto {
        private String slideId;
        private String referrer;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ViewCountResponseDto {
        private long totalViews;
    }
}
