package com.managehouse.money.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySpendingData {

    private String month;                    // Formato: "2025-08"
    private BigDecimal total;                // Total gasto no mês
    private Integer transactionCount;        // Quantidade de transações
    private List<CategoryAmount> categories; // Gastos por categoria

    /**
     * Gasto por categoria em um mês específico
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryAmount {
        private String name;         // Nome da categoria
        private BigDecimal amount;   // Valor gasto na categoria
    }
}
