package com.managehouse.money.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HouseholdIncomeAnalysis {
    // Current Month Income
    private BigDecimal marianaIncome;
    private BigDecimal lucasGrossIncome;
    private BigDecimal lucasNetIncome;  // After deductions & debt
    private BigDecimal totalHouseholdIncome;

    // Expenses
    private BigDecimal totalExpenses;
    private BigDecimal savings;  // Income - Expenses
    private Double savingsRate;  // (Savings / Income) * 100

    // Ratios
    private Double expenseToIncomeRatio;  // (Expenses / Income) * 100

    // Stability
    private Double incomeStabilityScore;  // 0-100 (100 = very stable)
    private String incomeStabilityStatus; // "Estável", "Moderado", "Volátil"

    // Historical (6 months)
    private List<MonthlyIncomeData> historicalData;

    // Budget Recommendations
    private String budgetStatus; // "Excelente", "Bom", "Atenção", "Crítico"
    private List<String> recommendations;
}
