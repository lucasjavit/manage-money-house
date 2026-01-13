package com.managehouse.money.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealPortfolioSummaryDTO {
    private Long id;
    private Long userId;
    private Integer reportMonth;
    private Integer reportYear;

    private BigDecimal totalStocks;
    private BigDecimal totalFiis;
    private BigDecimal totalFixedIncome;
    private BigDecimal totalFunds;
    private BigDecimal totalDividends;
    private BigDecimal grandTotal;

    private String aiAnalysis;
    private LocalDateTime uploadedAt;
    private LocalDateTime analyzedAt;

    // Score de saude da carteira
    private BigDecimal healthScore;
    private HealthScoreDetails healthScoreDetails;

    private List<PositionDTO> positions;
    private List<DividendDTO> dividends;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HealthScoreDetails {
        private BigDecimal diversificationScore;     // Diversificacao entre classes (0-25)
        private BigDecimal concentrationScore;       // Risco de concentracao (0-25)
        private BigDecimal qualityScore;             // Qualidade dos ativos (0-25)
        private BigDecimal riskScore;                // Nivel de risco geral (0-25)
        private String overallStatus;                // EXCELENTE, BOM, REGULAR, RUIM, CRITICO
        private String mainStrength;                 // Ponto forte principal
        private String mainWeakness;                 // Ponto fraco principal
        private List<String> recommendations;       // Recomendacoes de melhoria
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PositionDTO {
        private Long id;
        private String ticker;
        private String name;
        private String assetType;
        private String assetSubtype;
        private String institution;
        private BigDecimal quantity;
        private BigDecimal closePrice;
        private BigDecimal totalValue;
        private String maturityDate;

        // Campos de analise individual de IA
        private String aiRecommendation; // MANTER, VENDER, COMPRAR_MAIS
        private String aiAnalysis; // Justificativa da IA
        private String aiMainReason; // Frase curta principal
        private String aiRiskLevel; // BAIXO, MEDIO, ALTO
        private BigDecimal aiConfidenceScore; // 0.00 a 1.00
        private BigDecimal aiCeilingPrice; // Preco teto
        private String aiBias; // Vies: COMPRA, VENDA, NEUTRO
        private String aiAnalyzedAt; // Data da analise
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DividendDTO {
        private Long id;
        private String ticker;
        private String productName;
        private String paymentDate;
        private String eventType;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal netValue;
    }
}
