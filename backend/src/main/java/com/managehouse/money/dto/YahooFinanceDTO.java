package com.managehouse.money.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para dados fundamentalistas do Yahoo Finance
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YahooFinanceDTO {

    private String ticker;

    // Indicadores fundamentalistas
    private Double trailingPE;          // P/L (Price/Earnings)
    private Double forwardPE;           // P/L projetado
    private Double priceToBook;         // P/VP (Price/Book Value)
    private Double dividendYield;       // DY (0.08 = 8%)
    private Double trailingEps;         // LPA (Lucro por Acao)
    private Double bookValue;           // Valor Patrimonial por Acao

    // Dados de mercado
    private Double regularMarketPrice;  // Preco atual
    private Double fiftyTwoWeekHigh;    // Maxima 52 semanas
    private Double fiftyTwoWeekLow;     // Minima 52 semanas
    private Long marketCap;             // Valor de mercado

    // Informacoes da empresa
    private String sector;              // Setor
    private String industry;            // Industria
    private String shortName;           // Nome curto

    // Controle
    private boolean dataAvailable;      // Se os dados foram obtidos com sucesso
    private String errorMessage;        // Mensagem de erro se falhou

    /**
     * Retorna o P/L formatado ou "N/A" se nao disponivel
     */
    public String getFormattedPE() {
        if (trailingPE == null) return "N/A";
        return String.format("%.2f", trailingPE);
    }

    /**
     * Retorna o P/VP formatado ou "N/A" se nao disponivel
     */
    public String getFormattedPB() {
        if (priceToBook == null) return "N/A";
        return String.format("%.2f", priceToBook);
    }

    /**
     * Retorna o DY em percentual formatado ou "N/A" se nao disponivel
     */
    public String getFormattedDY() {
        if (dividendYield == null) return "N/A";
        return String.format("%.2f%%", dividendYield * 100);
    }

    /**
     * Retorna o LPA formatado ou "N/A" se nao disponivel
     */
    public String getFormattedEPS() {
        if (trailingEps == null) return "N/A";
        return String.format("R$ %.2f", trailingEps);
    }

    /**
     * Calcula o preco teto baseado no LPA e P/L justo do setor
     */
    public BigDecimal calculateCeilingPrice(double fairPE) {
        if (trailingEps == null || trailingEps <= 0) {
            return null;
        }
        return BigDecimal.valueOf(trailingEps * fairPE);
    }
}
