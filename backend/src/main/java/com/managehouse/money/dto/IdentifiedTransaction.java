package com.managehouse.money.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdentifiedTransaction {
    private String description;
    private BigDecimal amount;
    private LocalDate date;
    private Long expenseTypeId;
    private String expenseTypeName;
    private String confidence; // "high", "medium", "low"
}

