package com.managehouse.money.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioAsset {
    private String type;        // "Ações", "FIIs", "Renda Fixa", "Cripto"
    private int percentage;     // 40 (represents 40%)
    private String description; // "Ações de empresas consolidadas"
}
