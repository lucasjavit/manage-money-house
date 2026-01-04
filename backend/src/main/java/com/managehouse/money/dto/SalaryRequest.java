package com.managehouse.money.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalaryRequest {
    private Long userId;
    private BigDecimal fixedAmount; // Para salário fixo (Mariana)
    private BigDecimal hourlyRate; // Para salário variável (Lucas)
    private String currency; // "BRL" ou "USD"
}

