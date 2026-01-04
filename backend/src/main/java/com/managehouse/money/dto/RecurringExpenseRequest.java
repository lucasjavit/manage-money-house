package com.managehouse.money.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RecurringExpenseRequest {
    private Long userId;
    private Long expenseTypeId;
    private BigDecimal monthlyAmount;
    private LocalDate startDate;
    private LocalDate endDate;
}

