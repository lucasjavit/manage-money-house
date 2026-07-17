package com.managehouse.money.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ExpenseRequest {
    // Nulo cria um novo lançamento; preenchido edita o lançamento existente.
    private Long id;
    private Long userId;
    private Long expenseTypeId;
    private BigDecimal amount;
    private Integer month;
    private Integer year;
    private String description;
}

