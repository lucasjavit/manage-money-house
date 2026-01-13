package com.managehouse.money.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "portfolio_analyses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String portfolioName;

    @Column(nullable = false)
    private String ticker;

    @Column(nullable = false)
    private String assetName;

    @Column(nullable = false)
    private String assetType;

    @Column
    private Double currentPrice;

    @Column
    private Double ceilingPrice;

    @Column(nullable = false)
    private String recommendation; // MANTER, SUBSTITUIR, OBSERVAR

    @Column(columnDefinition = "TEXT")
    private String analysisText; // Análise detalhada da IA

    @Column(columnDefinition = "TEXT")
    private String substitutionSuggestion; // Sugestão de substituto se recomendação for SUBSTITUIR

    @Column
    private Integer confidenceScore; // 0-100

    @Column(nullable = false)
    private LocalDateTime analysisDate;

    @Column
    private LocalDateTime nextReviewDate; // Data da próxima revisão

    @Column
    private Boolean isActive; // Se esta análise ainda é a mais recente

    @PrePersist
    protected void onCreate() {
        if (analysisDate == null) {
            analysisDate = LocalDateTime.now();
        }
        if (nextReviewDate == null) {
            nextReviewDate = analysisDate.plusDays(10);
        }
        if (isActive == null) {
            isActive = true;
        }
    }
}
