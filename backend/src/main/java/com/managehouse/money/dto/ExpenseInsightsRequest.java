package com.managehouse.money.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseInsightsRequest {
    private Long userId;
    private Integer month;
    private Integer year;
    private List<TransactionSummary> transactions;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionSummary {
        private String description;
        private java.math.BigDecimal amount;
        private String expenseTypeName;
        private String date;
    }
}

