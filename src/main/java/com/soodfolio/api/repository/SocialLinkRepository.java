package com.soodfolio.api.repository;

import com.soodfolio.api.entity.SocialLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SocialLinkRepository extends JpaRepository<SocialLink, UUID> {
    List<SocialLink> findByPortfolioIdOrderByOrderIndexAsc(UUID portfolioId);
    Optional<SocialLink> findFirstByPortfolioIdAndPlatformIgnoreCase(UUID portfolioId, String platform);
}

