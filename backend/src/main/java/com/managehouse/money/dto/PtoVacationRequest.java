package com.managehouse.money.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PtoVacationRequest {
    private Long userId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
}
