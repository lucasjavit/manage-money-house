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
public class PtoConfigResponse {
    private Long id;
    private Long userId;
    private LocalDate baseDate;
    private BigDecimal initialBalance;
    private String country;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
