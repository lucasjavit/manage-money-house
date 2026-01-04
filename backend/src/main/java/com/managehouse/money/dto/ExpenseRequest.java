package com.managehouse.money.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ExpenseRequest {
    private Long userId;
    private Long expenseTypeId;
    private BigDecimal amount;
    private Integer month;
    private Integer year;
}

