package com.managehouse.money.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtractProcessResponse {
    private List<IdentifiedTransaction> transactions;
    private String rawText;
    private String error;
}

