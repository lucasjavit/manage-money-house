package com.managehouse.money.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompanyCategoryResponse {
    private Long id;
    private String name;
    private String type;
}
