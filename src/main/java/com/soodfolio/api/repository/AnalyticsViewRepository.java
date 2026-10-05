package com.soodfolio.api.repository;

import com.soodfolio.api.entity.AnalyticsView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AnalyticsViewRepository extends JpaRepository<AnalyticsView, UUID> {
    List<AnalyticsView> findByPortfolioId(UUID portfolioId);
}
