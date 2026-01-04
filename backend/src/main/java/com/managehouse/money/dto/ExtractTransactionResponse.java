package com.managehouse.money.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtractTransactionResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userColor;
    private Long expenseTypeId;
    private String expenseTypeName;
    private String description;
    private BigDecimal amount;
    private LocalDate transactionDate;
    private LocalDateTime createdAt;
}

