package com.managehouse.money.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompanyExpenseResponse {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private String categoryType;
    private String description;
    private BigDecimal amount;
    private LocalDate dueDate;
    private Integer month;
    private Integer year;
    private LocalDateTime createdAt;
}
