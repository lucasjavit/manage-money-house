package com.managehouse.money.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userColor;
    private Long expenseTypeId;
    private String expenseTypeName;
    private BigDecimal amount;
    private Integer month;
    private Integer year;
    private Long recurringExpenseId;
    private LocalDateTime createdAt;
}

