package com.managehouse.money.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EconomicContextResponse {

    private IndicatorData ipca;
    private IndicatorData igpm;
    private SelicData selic;
    private ExchangeData usdBrl;
    private Double inflation12Months;

    /**
     * Dados de um indicador econômico (IPCA, IGP-M)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndicatorData {
        private Double value;           // Valor do indicador (%)
        private String period;          // Período (formato: "2026-01")
        private Double vsLastMonth;     // Variação vs mês anterior
    }

    /**
     * Dados da Taxa SELIC
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SelicData {
        private Double value;           // Taxa SELIC (%)
        private String lastUpdate;      // Data da última atualização
    }

    /**
     * Dados de câmbio (USD/BRL)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExchangeData {
        private Double value;           // Cotação (R$)
        private Double variation;       // Variação percentual (%)
    }
}
