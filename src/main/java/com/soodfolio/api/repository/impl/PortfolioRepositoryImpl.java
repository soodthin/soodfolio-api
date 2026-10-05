package com.soodfolio.api.repository.impl;

import com.soodfolio.api.entity.Portfolio;
import com.soodfolio.api.repository.PortfolioRepositoryCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class PortfolioRepositoryImpl implements PortfolioRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Portfolio> findActivePortfolioWithFullDetails(String username) {
        String jpql = "SELECT DISTINCT p FROM Portfolio p " +
                      "LEFT JOIN FETCH p.socialLinks " +
                      "WHERE p.username = :username AND p.isDeleted = false";
        try {
            Portfolio portfolio = entityManager.createQuery(jpql, Portfolio.class)
                    .setParameter("username", username)
                    .getSingleResult();
            return Optional.ofNullable(portfolio);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
