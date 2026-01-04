package com.managehouse.money.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalaryCalculationResponse {
    private BigDecimal hourlyRate; // Valor por hora em USD
    private Integer workingDays; // Dias úteis do mês
    private Integer hoursPerDay; // Horas por dia (assumindo 8 horas)
    private BigDecimal totalHours; // Total de horas no mês
    private BigDecimal totalAmount; // Total em USD (bruto)
    private BigDecimal totalAmountBRL; // Total convertido para BRL (bruto)
    private BigDecimal totalDeductions; // Total de descontos (boletos) em BRL
    private BigDecimal netSalaryBRL; // Salário líquido (bruto - descontos) em BRL
    private BigDecimal exchangeRate; // Taxa de câmbio USD para BRL
    private String currency; // "USD"
    private Integer month;
    private Integer year;
}

