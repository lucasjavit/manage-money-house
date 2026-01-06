package com.managehouse.money.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalaryConversionResponse {
    private Long id;
    private Long userId;
    private Integer month;
    private Integer year;
    private LocalDate conversionDate;
    private BigDecimal exchangeRate;
    private BigDecimal amountUSD;
    private BigDecimal vet;
    private BigDecimal finalAmountBRL;
}

