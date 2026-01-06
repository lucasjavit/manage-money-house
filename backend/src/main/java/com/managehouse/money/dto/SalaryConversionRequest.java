package com.managehouse.money.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalaryConversionRequest {
    private Long userId;
    private Integer month; // Mês de pagamento (1-12)
    private Integer year; // Ano de pagamento
    private LocalDate conversionDate; // Data da conversão
    private BigDecimal exchangeRate; // Cotação
    private BigDecimal amountUSD; // Valor do saque em USD
    private BigDecimal vet; // VET (Valor Efetivo da Taxa)
    private BigDecimal finalAmountBRL; // Valor convertido final em BRL
}

