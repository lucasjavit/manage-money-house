package com.managehouse.money.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngestResponse {
    // "created", "duplicate" ou "needs_review" (IA não extraiu o valor)
    private String status;
    // id da BankTransaction gravada (null quando duplicate)
    private Long id;
    // id do Expense criado, quando destination = "house" e não precisa de revisão
    private Long expenseId;
    // Eco do que a IA extraiu, útil para o app mostrar/confirmar
    private BigDecimal amount;
    private Long suggestedExpenseTypeId;
    private boolean needsReview;
}
