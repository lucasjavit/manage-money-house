package com.managehouse.money.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyIncomeData {
    private String month; // "2025-08"
    private BigDecimal marianaIncome;
    private BigDecimal lucasIncome;
    private BigDecimal totalIncome;
    private BigDecimal expenses;
    private BigDecimal savings;
}
