package com.managehouse.money.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SalaryDeductionRequest {
    private Long userId;
    private String description;
    private BigDecimal amount;
    private LocalDate dueDate;
    private Integer month;
    private Integer year;
}

