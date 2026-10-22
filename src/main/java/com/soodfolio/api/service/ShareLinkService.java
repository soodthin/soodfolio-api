package com.soodfolio.api.service;

import com.soodfolio.api.dto.ShareLinkDtos.*;

import java.util.List;
import java.util.UUID;

public interface ShareLinkService {

    ShareLinkResponse createShareLink(UUID userId, CreateShareLinkRequest request);

    List<ShareLinkSummaryResponse> getShareLinks(UUID userId);

    void revokeShareLink(UUID userId, UUID linkId);

    TelemetryAnalyticsResponse getLinkAnalytics(UUID userId, UUID linkId);

    PublicDeckResponse getPublicDeck(String rawToken);

    void recordTelemetry(String rawToken, TelemetryBatchRequest request, String userAgent);
}
