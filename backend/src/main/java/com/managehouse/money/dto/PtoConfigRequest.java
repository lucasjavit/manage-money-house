package com.managehouse.money.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PtoConfigRequest {
    private Long userId;
    private LocalDate baseDate;
    private BigDecimal initialBalance;
    private String country; // "BR" ou "US"
}
