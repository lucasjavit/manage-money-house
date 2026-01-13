package com.managehouse.money.repository;

import com.managehouse.money.entity.PortfolioAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioAnalysisRepository extends JpaRepository<PortfolioAnalysis, Long> {

    // Buscar última análise ativa de um ticker específico
    Optional<PortfolioAnalysis> findByTickerAndIsActiveTrue(String ticker);

    // Buscar todas as análises ativas de uma carteira
    List<PortfolioAnalysis> findByPortfolioNameAndIsActiveTrueOrderByAnalysisDateDesc(String portfolioName);

    // Buscar análises que precisam de revisão (nextReviewDate <= agora)
    List<PortfolioAnalysis> findByIsActiveTrueAndNextReviewDateLessThanEqual(LocalDateTime date);

    // Buscar histórico de análises de um ticker
    List<PortfolioAnalysis> findByTickerOrderByAnalysisDateDesc(String ticker);

    // Buscar todas as análises ativas
    List<PortfolioAnalysis> findByIsActiveTrueOrderByAnalysisDateDesc();

    // Buscar análises com recomendação SUBSTITUIR
    List<PortfolioAnalysis> findByRecommendationAndIsActiveTrueOrderByAnalysisDateDesc(String recommendation);

    // Desativar análises anteriores de um ticker
    @Modifying
    @Query("UPDATE PortfolioAnalysis p SET p.isActive = false WHERE p.ticker = :ticker AND p.isActive = true")
    void deactivatePreviousAnalyses(@Param("ticker") String ticker);

    // Contar análises pendentes de revisão
    @Query("SELECT COUNT(p) FROM PortfolioAnalysis p WHERE p.isActive = true AND p.nextReviewDate <= :date")
    long countPendingReviews(@Param("date") LocalDateTime date);
}
