package com.managehouse.money.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrazilianIndicesResponse {
    private MarketIndexData ibovespa;
    private MarketIndexData ifix;
    private MarketIndexData idiv;
}
