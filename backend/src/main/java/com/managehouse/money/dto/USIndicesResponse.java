package com.managehouse.money.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class USIndicesResponse {
    private MarketIndexData sp500;
    private MarketIndexData nasdaq;
    private MarketIndexData dow;
}
