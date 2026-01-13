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
public class AssetAnalysisResponse {
    private String ticker;
    private String name;
    private String type;
    private Double currentPrice;
    private Double ceilingPrice;
    private Double expectedDY;
    private String bias;
    private String rationale;           // Análise fundamentalista (já existe)
    private String aiAnalysis;          // Análise detalhada da IA
    private String investmentThesis;    // Por que investir
    private List<String> risks;         // Principais riscos
    private String shortTermOutlook;    // Perspectiva curto prazo
    private String sectorComparison;    // Comparação com setor
    private EconomicImpact economicImpact; // Impacto de SELIC/inflação

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EconomicImpact {
        private Double selic;
        private String selicImpact;     // "Positivo", "Negativo", "Neutro" + explicação
        private Double ipca;
        private String ipcaImpact;      // "Positivo", "Negativo", "Neutro" + explicação
    }
}
