package com.soodfolio.api.repository;

import com.soodfolio.api.entity.ShareLinkTelemetry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShareLinkTelemetryRepository extends JpaRepository<ShareLinkTelemetry, UUID> {

    List<ShareLinkTelemetry> findByShareLinkId(UUID shareLinkId);

    List<ShareLinkTelemetry> findByShareLinkIdOrderByCreatedAtDesc(UUID shareLinkId);
}
