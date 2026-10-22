package com.soodfolio.api.repository;

import com.soodfolio.api.entity.PortfolioSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PortfolioSnapshotRepository extends JpaRepository<PortfolioSnapshot, UUID> {

    List<PortfolioSnapshot> findByPortfolioIdOrderByVersionDesc(UUID portfolioId);

    Optional<PortfolioSnapshot> findByPortfolioIdAndVersion(UUID portfolioId, int version);

    Optional<PortfolioSnapshot> findTopByPortfolioIdOrderByVersionDesc(UUID portfolioId);
}
