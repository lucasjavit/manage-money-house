package com.managehouse.money.dto;

import lombok.Data;

@Data
public class SalaryCalculationRequest {
    private Long userId;
    private Integer month; // 1-12
    private Integer year;
}

