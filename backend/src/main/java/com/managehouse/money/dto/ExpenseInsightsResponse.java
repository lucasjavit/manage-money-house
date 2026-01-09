package com.managehouse.money.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseInsightsResponse {
    private BigDecimal totalSpent;
    private Integer totalTransactions;
    private BigDecimal averagePerTransaction;
    private String mostExpensiveDay;
    private String mostActiveCategory;
    private List<CategorySummary> categories;
    private List<TopExpense> topExpenses;
    private AIInsights aiInsights;
    private List<Trend> trends;
    private List<QuickStat> quickStats;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySummary {
        private String categoryName;
        private BigDecimal total;
        private Integer count;
        private Double percentage;
        private BigDecimal highestExpense;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopExpense {
        private String description;
        private BigDecimal amount;
        private String categoryName;
        private String date;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AIInsights {
        private String summary;
        private List<String> suggestions;
        private List<String> warnings;
        private String analysis;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Trend {
        private String description;
        private String value;
        private String type; // "increase", "decrease", "stable"
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuickStat {
        private String label;
        private String value;
        private String icon;
        private String color;
    }
}

