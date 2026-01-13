package com.managehouse.money.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketDataDashboardResponse {
    private ForexDataResponse forex;
    private BrazilianIndicesResponse brazilianIndices;
    private USIndicesResponse usIndices;
    private CryptoDataResponse crypto;
    private String lastUpdate; // Overall dashboard timestamp
}
