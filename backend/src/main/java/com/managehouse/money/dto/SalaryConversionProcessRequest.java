package com.managehouse.money.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalaryConversionProcessRequest {
    private String text; // Texto completo da conversão
    private Integer month; // Mês de pagamento (1-12)
    private Integer year; // Ano de pagamento
}

