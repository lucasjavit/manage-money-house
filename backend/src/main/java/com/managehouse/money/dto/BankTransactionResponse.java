package com.managehouse.money.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankTransactionResponse {
    private Long id;
    private BigDecimal amount;
    private String description;
    private LocalDate transactionDate;
    private String sourcePackage;
    private String rawText;
    private boolean needsReview;
}
