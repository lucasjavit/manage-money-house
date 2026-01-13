package com.managehouse.money.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CryptoDataResponse {
    private MarketIndexData bitcoin;
    private MarketIndexData ethereum;
    private List<MarketIndexData> otherCoins; // BNB, USDT, USDC
}
