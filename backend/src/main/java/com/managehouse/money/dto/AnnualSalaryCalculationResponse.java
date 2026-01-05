package com.managehouse.money.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnualSalaryCalculationResponse {
    private BigDecimal hourlyRate; // Valor por hora em USD
    private Integer year;
    private Integer totalWorkingDays; // Total de dias úteis no ano
    private Integer hoursPerDay; // Horas por dia (8 horas)
    private Integer totalHours; // Total de horas no ano
    private BigDecimal totalAmountUSD; // Total em USD (bruto)
    private BigDecimal totalAmountBRL; // Total convertido para BRL (bruto)
    private BigDecimal totalDeductions; // Total de descontos (boletos) em BRL no ano
    private BigDecimal totalLucasDebt; // Dívida total do Lucas para Mariana no ano (se positivo, Lucas deve; se negativo, Mariana deve; se zero, quites)
    private BigDecimal netSalaryBRL; // Salário líquido anual (bruto - descontos - dívida) em BRL
    private BigDecimal exchangeRate; // Taxa de câmbio USD para BRL
    private String currency; // "USD"
}

