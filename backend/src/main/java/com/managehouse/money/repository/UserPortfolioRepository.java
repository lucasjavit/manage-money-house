package com.managehouse.money.repository;

import com.managehouse.money.entity.UserPortfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPortfolioRepository extends JpaRepository<UserPortfolio, Long> {

    Optional<UserPortfolio> findByUserId(Long userId);

    Optional<UserPortfolio> findByUserIdAndRiskProfile(Long userId, String riskProfile);

    void deleteByUserId(Long userId);
}
