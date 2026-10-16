package com.soodfolio.api.repository;

import com.soodfolio.api.entity.Education;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EducationRepository extends JpaRepository<Education, UUID> {
    List<Education> findByPortfolioIdOrderByOrderIndexAsc(UUID portfolioId);
    Optional<Education> findByIdAndPortfolioId(UUID id, UUID portfolioId);
    int countByPortfolioId(UUID portfolioId);
}
