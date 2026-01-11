package com.managehouse.money.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseAlertsResponse {
    private Integer month;
    private Integer year;
    private List<Alert> alerts;
    private Summary summary;
    private AIMonthlyAnalysisResponse aiAnalysis; // Análise AI completa do mês

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Alert {
        private String severity; // "critical", "warning", "normal"
        private String expenseTypeName;
        private BigDecimal currentValue;
        private BigDecimal averageValue;
        private BigDecimal maxHistoricalValue;
        private Double percentageAboveAverage;
        private Boolean isHistoricalMax;
        private String suggestion;
        private String icon;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private Integer totalAlerts;
        private Integer criticalCount;
        private Integer warningCount;
        private BigDecimal totalMonthSpent;
        private BigDecimal averageMonthSpent;
        private String overallStatus; // "good", "attention", "critical"
    }
}
