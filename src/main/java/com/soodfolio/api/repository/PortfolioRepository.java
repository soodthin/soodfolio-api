package com.soodfolio.api.repository;

import com.soodfolio.api.entity.Portfolio;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, UUID>, PortfolioRepositoryCustom {

    @EntityGraph(attributePaths = {"user"})
    Optional<Portfolio> findByUsername(String username);

    boolean existsByUsername(String username);

    @EntityGraph(attributePaths = {"user"})
    Optional<Portfolio> findByUserId(UUID userId);
}
