package com.managehouse.money.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIMonthlyAnalysisResponse {
    private String executiveSummary;
    private Integer financialHealthScore; // 0-100
    private List<Pattern> patternsDetected;
    private List<String> recommendations;
    private Prediction nextMonthPrediction;
    private Comparison comparison;
    private EconomicContextResponse economicContext; // Novo: contexto econômico
    private List<MonthlySpendingData> historicalData; // Novo: dados históricos para gráficos (6 meses)
    private HouseholdIncomeAnalysis householdIncome; // Novo: análise de renda da casa (Mariana + Lucas)

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pattern {
        private String type; // "temporal", "category", "trend", "anomaly"
        private String description;
        private String insight;
        private String icon;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Prediction {
        private BigDecimal predictedAmount;
        private Double confidence; // 0-1
        private String reasoning;
        private List<String> assumptions;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Comparison {
        private String vsLastMonth;
        private String vsAverage;
        private String trend; // "increasing", "decreasing", "stable"
    }
}
