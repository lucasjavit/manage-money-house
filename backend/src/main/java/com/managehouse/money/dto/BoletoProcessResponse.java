package com.managehouse.money.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoletoProcessResponse {
    private String description; // Nome/descrição do boleto
    private BigDecimal amount; // Valor
    private LocalDate dueDate; // Data de vencimento
    private String error; // Erro se houver
}

