package com.managehouse.money.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketIndexData {
    private String symbol;      // "USD/BRL", "^BVSP", "BTC"
    private String name;        // "Dólar Americano", "Ibovespa", "Bitcoin"
    private BigDecimal value;   // Current value
    private Double change;      // % change (24h or daily)
    private String trend;       // "up", "down", "neutral"
    private String lastUpdate;  // ISO timestamp
}
