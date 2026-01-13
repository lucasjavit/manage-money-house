package com.managehouse.money.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendedAsset {
    private Integer rank;           // Posição na carteira (1, 2, 3...)
    private String ticker;          // "PETR4", "ITUB4", "BTC"
    private String name;            // "Petrobras", "Itaú Unibanco", "Bitcoin"
    private String type;            // "Ação", "FII", "Cripto", "Renda Fixa", "ETF"
    private Double expectedDY;      // Dividend Yield esperado (ex: 8.5 = 8.5%), null para renda fixa
    private Double entryPrice;      // Preço sugerido de entrada (R$)
    private Double currentPrice;    // Preço atual em tempo real (preenchido pela API)
    private Double ceilingPrice;    // Preço-teto para compra (R$)
    private Double targetAllocation; // Percentual sugerido na carteira (ex: 10.0 = 10%)
    private String bias;            // "Comprar" ou "Aguardar" (calculado: currentPrice < ceilingPrice)
    private String rationale;       // Motivo da recomendação baseado em análise fundamentalista
}
