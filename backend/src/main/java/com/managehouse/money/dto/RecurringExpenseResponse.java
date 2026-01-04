package com.managehouse.money.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class RecurringExpenseResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userColor;
    private Long expenseTypeId;
    private String expenseTypeName;
    private BigDecimal monthlyAmount;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
}

