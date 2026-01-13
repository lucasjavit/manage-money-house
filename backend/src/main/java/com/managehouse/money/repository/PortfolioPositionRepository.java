package com.managehouse.money.repository;

import com.managehouse.money.entity.PortfolioPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PortfolioPositionRepository extends JpaRepository<PortfolioPosition, Long> {

    List<PortfolioPosition> findByPortfolioId(Long portfolioId);

    List<PortfolioPosition> findByPortfolioIdAndAssetType(Long portfolioId, String assetType);

    void deleteByPortfolioId(Long portfolioId);
}
