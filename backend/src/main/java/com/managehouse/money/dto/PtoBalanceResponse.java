package com.managehouse.money.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PtoBalanceResponse {
    private LocalDate date;              // data de referência do cálculo
    private BigDecimal balance;          // saldo de PTO (com fração)
    private BigDecimal initialBalance;
    private BigDecimal accruedSinceBase; // acumulado desde a baseDate
    private BigDecimal usedVacationDays; // total de dias úteis já usados em férias
    private int daysToNextPto;           // dias corridos até ganhar +1 dia
    private BigDecimal fractionToNextPto;// 0..1 para a barra de progresso
    private String country;
}
