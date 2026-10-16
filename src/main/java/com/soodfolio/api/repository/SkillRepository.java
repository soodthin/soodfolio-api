package com.soodfolio.api.repository;

import com.soodfolio.api.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SkillRepository extends JpaRepository<Skill, UUID> {
    List<Skill> findByPortfolioIdOrderByCategoryAscOrderIndexAsc(UUID portfolioId);
    List<Skill> findByPortfolioIdOrderByOrderIndexAsc(UUID portfolioId);
    Optional<Skill> findByIdAndPortfolioId(UUID id, UUID portfolioId);
    int countByPortfolioId(UUID portfolioId);
}
