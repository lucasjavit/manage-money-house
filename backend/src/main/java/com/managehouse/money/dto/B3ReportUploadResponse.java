package com.managehouse.money.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class B3ReportUploadResponse {
    private Integer reportMonth;
    private Integer reportYear;
    private List<StockPosition> stocks;
    private List<FiiPosition> fiis;
    private List<FixedIncomePosition> fixedIncome;
    private List<FundPosition> funds;
    private List<DividendReceived> dividends;
    private PortfolioTotals totals;
    private String aiAnalysis;
    private String errorMessage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockPosition {
        private String ticker;
        private String name;
        private String type; // ON, PN, PNB
        private BigDecimal quantity;
        private BigDecimal closePrice;
        private BigDecimal totalValue;
        private String institution;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FiiPosition {
        private String ticker;
        private String name;
        private BigDecimal quantity;
        private BigDecimal closePrice;
        private BigDecimal totalValue;
        private String institution;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FixedIncomePosition {
        private String product;
        private String productType; // CDB, LCA, LCI, DEBENTURE
        private String institution;
        private String maturityDate;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FundPosition {
        private String ticker;
        private String name;
        private String fundType; // FIAGRO, FII, etc
        private BigDecimal quantity;
        private BigDecimal closePrice;
        private BigDecimal totalValue;
        private String institution;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DividendReceived {
        private String ticker;
        private String productName;
        private String paymentDate;
        private String eventType; // Dividendo, JCP, Rendimento
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal netValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PortfolioTotals {
        private BigDecimal stocks;
        private BigDecimal fiis;
        private BigDecimal fixedIncome;
        private BigDecimal funds;
        private BigDecimal dividends;
        private BigDecimal grandTotal;
    }
}
