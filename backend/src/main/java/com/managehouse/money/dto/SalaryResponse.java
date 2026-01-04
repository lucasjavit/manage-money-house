package com.managehouse.money.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalaryResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userColor;
    private BigDecimal fixedAmount; // Para salário fixo (Mariana)
    private BigDecimal hourlyRate; // Para salário variável (Lucas)
    private String currency; // "BRL" ou "USD"
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

