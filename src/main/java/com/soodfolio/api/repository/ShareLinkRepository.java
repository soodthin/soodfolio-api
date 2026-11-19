package com.soodfolio.api.repository;

import com.soodfolio.api.entity.ShareLink;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShareLinkRepository extends JpaRepository<ShareLink, UUID> {

    List<ShareLink> findByPortfolioIdOrderByCreatedAtDesc(UUID portfolioId);

    @EntityGraph(attributePaths = {"snapshot", "portfolio"})
    Optional<ShareLink> findByTokenHash(String tokenHash);

    Optional<ShareLink> findByIdAndPortfolioId(UUID id, UUID portfolioId);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE ShareLink s SET s.viewCount = s.viewCount + 1, s.updatedAt = :now " +
           "WHERE s.id = :id AND s.isRevoked = false " +
           "AND (s.expiresAt IS NULL OR s.expiresAt >= :now) " +
           "AND (s.maxViews IS NULL OR s.viewCount < s.maxViews)")
    int incrementViewCountIfAllowed(@Param("id") UUID id, @Param("now") LocalDateTime now);
}
