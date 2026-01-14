package com.managehouse.money.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "portfolio_positions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private UserRealPortfolio portfolio;

    @Column(nullable = false)
    private String ticker;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String assetType; // ACAO, FII, CDB, LCA, LCI, DEBENTURE, FUNDO

    private String assetSubtype; // ON, PN, PNB, PNA (para acoes)

    private String institution;

    @Column(precision = 18, scale = 8)
    private BigDecimal quantity;

    @Column(precision = 18, scale = 2)
    private BigDecimal closePrice;

    @Column(precision = 18, scale = 2)
    private BigDecimal totalValue;

    private LocalDate maturityDate; // Vencimento (para renda fixa)

    // Campos para analise individual de IA
    @Column(length = 20)
    private String aiRecommendation; // MANTER, VENDER, COMPRAR_MAIS

    @Column(columnDefinition = "TEXT")
    private String aiAnalysis; // Justificativa da IA

    private String aiMainReason; // Frase curta principal

    @Column(length = 10)
    private String aiRiskLevel; // BAIXO, MEDIO, ALTO

    @Column(precision = 3, scale = 2)
    private BigDecimal aiConfidenceScore; // 0.00 a 1.00

    @Column(precision = 18, scale = 2)
    private BigDecimal aiCeilingPrice; // Preco teto calculado pela IA

    @Column(length = 20)
    private String aiBias; // Vies: COMPRA, VENDA, NEUTRO

    @Column(columnDefinition = "TEXT")
    private String aiValuationAnalysis; // Analise detalhada dos indicadores de valuation

    private LocalDateTime aiAnalyzedAt; // Data da analise

    // Dados fundamentalistas do Yahoo Finance (salvos para exibir no tooltip)
    @Column(precision = 10, scale = 2)
    private BigDecimal yahooTrailingPE; // P/L do Yahoo Finance

    @Column(precision = 10, scale = 2)
    private BigDecimal yahooPriceToBook; // P/VP do Yahoo Finance

    @Column(precision = 10, scale = 4)
    private BigDecimal yahooDividendYield; // DY do Yahoo Finance (0.08 = 8%)

    @Column(length = 100)
    private String yahooSector; // Setor do Yahoo Finance
}
