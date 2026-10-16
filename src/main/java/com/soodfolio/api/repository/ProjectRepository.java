package com.soodfolio.api.repository;

import com.soodfolio.api.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    List<Project> findByPortfolioIdOrderByOrderIndexAsc(UUID portfolioId);
    Optional<Project> findByIdAndPortfolioId(UUID id, UUID portfolioId);
    int countByPortfolioId(UUID portfolioId);
}
