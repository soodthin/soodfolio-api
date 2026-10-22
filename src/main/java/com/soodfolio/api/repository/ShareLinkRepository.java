package com.soodfolio.api.repository;

import com.soodfolio.api.entity.ShareLink;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShareLinkRepository extends JpaRepository<ShareLink, UUID> {

    List<ShareLink> findByPortfolioIdOrderByCreatedAtDesc(UUID portfolioId);

    @EntityGraph(attributePaths = {"snapshot", "portfolio"})
    Optional<ShareLink> findByTokenHash(String tokenHash);

    Optional<ShareLink> findByIdAndPortfolioId(UUID id, UUID portfolioId);
}
