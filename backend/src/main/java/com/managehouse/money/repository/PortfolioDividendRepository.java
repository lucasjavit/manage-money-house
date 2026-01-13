package com.managehouse.money.repository;

import com.managehouse.money.entity.PortfolioDividend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PortfolioDividendRepository extends JpaRepository<PortfolioDividend, Long> {

    List<PortfolioDividend> findByPortfolioId(Long portfolioId);

    void deleteByPortfolioId(Long portfolioId);
}
