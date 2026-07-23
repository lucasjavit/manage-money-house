package com.managehouse.money.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PtoVacationResponse {
    private Long id;
    private Long userId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
    private int businessDaysUsed; // dias úteis (seg-sex menos feriados) desse período
    private LocalDateTime createdAt;
}
