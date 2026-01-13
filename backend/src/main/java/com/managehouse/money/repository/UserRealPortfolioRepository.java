package com.managehouse.money.repository;

import com.managehouse.money.entity.UserRealPortfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRealPortfolioRepository extends JpaRepository<UserRealPortfolio, Long> {

    Optional<UserRealPortfolio> findTopByUserIdOrderByReportYearDescReportMonthDesc(Long userId);

    Optional<UserRealPortfolio> findByUserIdAndReportMonthAndReportYear(Long userId, Integer month, Integer year);

    List<UserRealPortfolio> findByUserIdOrderByReportYearDescReportMonthDesc(Long userId);

    void deleteByUserId(Long userId);
}
