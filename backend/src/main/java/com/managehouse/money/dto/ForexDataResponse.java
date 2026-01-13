package com.managehouse.money.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForexDataResponse {
    private MarketIndexData usd;
    private MarketIndexData eur;
    private MarketIndexData gbp;
    private MarketIndexData jpy;
}
