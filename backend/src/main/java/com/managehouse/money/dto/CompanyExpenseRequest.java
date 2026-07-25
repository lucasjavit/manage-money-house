package com.managehouse.money.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CompanyExpenseRequest {
    private Long userId;
    private Long categoryId;
    private String description;
    private BigDecimal amount;
    private LocalDate dueDate;
    private Integer month;
    private Integer year;
}
