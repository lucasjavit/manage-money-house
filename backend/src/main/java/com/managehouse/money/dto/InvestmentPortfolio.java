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
public class InvestmentPortfolio {
    private String name;        // "Carteira de Valor"
    private String description; // "Foca em empresas subavaliadas..."
    private String strategy;    // "Buy and Hold, análise fundamentalista..."
    private String riskLevel;   // "Moderado", "Alto", "Baixo"
    private List<PortfolioAsset> suggestedComposition;
    private List<String> characteristics;
    private List<RecommendedAsset> recommendedAssets; // Ativos específicos recomendados
    private String icon;        // Emoji or icon name
}
