package com.managehouse.money.dto;

import lombok.Data;

@Data
public class CompanyCategoryRequest {
    private Long userId;
    private String name;
    private String type; // "CONTA" ou "IMPOSTO"
}
