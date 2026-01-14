package com.managehouse.money.controller;

import com.managehouse.money.dto.*;
import com.managehouse.money.service.AssetAnalysisService;
import com.managehouse.money.service.AssetPriceService;
import com.managehouse.money.service.MarketDataService;
import com.managehouse.money.service.PersonalizedPortfolioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
@Slf4j
public class MarketDataController {

    private final MarketDataService marketDataService;
    private final AssetPriceService assetPriceService;
    private final AssetAnalysisService assetAnalysisService;
    private final PersonalizedPortfolioService personalizedPortfolioService;

    @GetMapping("/dashboard")
    public ResponseEntity<MarketDataDashboardResponse> getDashboard() {
        log.info("GET /api/market/dashboard");
        return ResponseEntity.ok(marketDataService.getDashboard());
    }

    @GetMapping("/forex")
    public ResponseEntity<ForexDataResponse> getForex() {
        log.info("GET /api/market/forex");
        return ResponseEntity.ok(marketDataService.getForexRates());
    }

    @GetMapping("/brazil")
    public ResponseEntity<BrazilianIndicesResponse> getBrazilianIndices() {
        log.info("GET /api/market/brazil");
        return ResponseEntity.ok(marketDataService.getBrazilianIndices());
    }

    @GetMapping("/us")
    public ResponseEntity<USIndicesResponse> getUSIndices() {
        log.info("GET /api/market/us");
        return ResponseEntity.ok(marketDataService.getUSIndices());
    }

    @GetMapping("/crypto")
    public ResponseEntity<CryptoDataResponse> getCrypto() {
        log.info("GET /api/market/crypto");
        return ResponseEntity.ok(marketDataService.getCryptoPrices());
    }

    @GetMapping("/portfolios")
    public ResponseEntity<List<InvestmentPortfolio>> getPortfolios() {
        log.info("GET /api/market/portfolios");
        List<InvestmentPortfolio> portfolios = getStaticPortfolios();

        // Enriquecer cada carteira com preços em tempo real
        for (InvestmentPortfolio portfolio : portfolios) {
            if (portfolio.getRecommendedAssets() != null) {
                assetPriceService.enrichWithPrices(portfolio.getRecommendedAssets());
            }
        }

        return ResponseEntity.ok(portfolios);
    }

    @GetMapping("/asset/{ticker}/analysis")
    public ResponseEntity<AssetAnalysisResponse> getAssetAnalysis(
            @PathVariable String ticker,
            @RequestParam String portfolioType) {
        log.info("GET /api/market/asset/{}/analysis?portfolioType={}", ticker, portfolioType);

        // Buscar o asset das carteiras estáticas
        RecommendedAsset asset = findAssetByTicker(ticker);
        if (asset == null) {
            log.warn("Ativo não encontrado: {}", ticker);
            return ResponseEntity.notFound().build();
        }

        // Enriquecer com preço atual
        assetPriceService.enrichWithPrices(List.of(asset));

        // Gerar análise
        AssetAnalysisResponse analysis = assetAnalysisService.analyzeAsset(asset, portfolioType);
        return ResponseEntity.ok(analysis);
    }

    /**
     * Gera uma carteira personalizada baseada no perfil de risco
     */
    @PostMapping("/personalized-portfolio")
    public ResponseEntity<InvestmentPortfolio> generatePersonalizedPortfolio(
            @RequestBody PersonalizedPortfolioRequest request) {
        log.info("POST /api/market/personalized-portfolio - UserId: {}, Perfil: {}",
                request.getUserId(), request.getRiskProfile());

        InvestmentPortfolio portfolio = personalizedPortfolioService
                .generatePortfolio(request.getRiskProfile(), request.getUserId());

        return ResponseEntity.ok(portfolio);
    }

    /**
     * Busca a carteira personalizada salva do usuario
     */
    @GetMapping("/personalized-portfolio/{userId}")
    public ResponseEntity<InvestmentPortfolio> getMyPortfolio(@PathVariable Long userId) {
        log.info("GET /api/market/personalized-portfolio/{}", userId);

        return personalizedPortfolioService.getUserPortfolio(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private RecommendedAsset findAssetByTicker(String ticker) {
        List<InvestmentPortfolio> portfolios = getStaticPortfolios();
        for (InvestmentPortfolio portfolio : portfolios) {
            if (portfolio.getRecommendedAssets() != null) {
                for (RecommendedAsset asset : portfolio.getRecommendedAssets()) {
                    if (asset.getTicker().equalsIgnoreCase(ticker)) {
                        return asset;
                    }
                }
            }
        }
        return null;
    }

    private List<InvestmentPortfolio> getStaticPortfolios() {
        List<InvestmentPortfolio> portfolios = new ArrayList<>();

        // 1. Carteira de Valor
        portfolios.add(InvestmentPortfolio.builder()
                .name("Carteira de Valor")
                .description("Foca em empresas consolidadas e subavaliadas pelo mercado, com fundamentos sólidos e potencial de valorização no longo prazo.")
                .strategy("Buy and Hold, análise fundamentalista, foco em P/L, P/VP e dividend yield")
                .riskLevel("Moderado")
                .icon("💎")
                .suggestedComposition(Arrays.asList(
                        PortfolioAsset.builder().type("Ações").percentage(60).description("Ações Blue Chips").build(),
                        PortfolioAsset.builder().type("FIIs").percentage(30).description("Fundos Imobiliários").build(),
                        PortfolioAsset.builder().type("Renda Fixa").percentage(10).description("Tesouro Direto").build()
                ))
                .recommendedAssets(Arrays.asList(
                        RecommendedAsset.builder().rank(1)
                                .ticker("VALE3").name("Vale").type("Ação")
                                .expectedDY(9.5).entryPrice(58.00).ceilingPrice(72.00).targetAllocation(10.0)
                                .rationale("P/L 5-6x, líder global minério de ferro, forte geração de caixa").build(),
                        RecommendedAsset.builder().rank(2)
                                .ticker("BBAS3").name("Banco do Brasil").type("Ação")
                                .expectedDY(10.2).entryPrice(26.00).ceilingPrice(32.00).targetAllocation(10.0)
                                .rationale("P/VP < 1.0, ROE 15%+, dividendos regulares, banco mais seguro").build(),
                        RecommendedAsset.builder().rank(3)
                                .ticker("ITUB4").name("Itaú Unibanco").type("Ação")
                                .expectedDY(6.5).entryPrice(30.00).ceilingPrice(38.00).targetAllocation(10.0)
                                .rationale("Maior banco privado LATAM, ROE 20%+, solidez financeira").build(),
                        RecommendedAsset.builder().rank(4)
                                .ticker("PETR4").name("Petrobras").type("Ação")
                                .expectedDY(15.0).entryPrice(36.00).ceilingPrice(45.00).targetAllocation(10.0)
                                .rationale("P/L 3-4x, alta geração de caixa, dividendos extraordinários").build(),
                        RecommendedAsset.builder().rank(5)
                                .ticker("EGIE3").name("Engie Brasil").type("Ação")
                                .expectedDY(7.5).entryPrice(40.00).ceilingPrice(48.00).targetAllocation(10.0)
                                .rationale("Setor elétrico regulado, receita previsível, baixa volatilidade").build(),
                        RecommendedAsset.builder().rank(6)
                                .ticker("TAEE11").name("Taesa").type("Ação")
                                .expectedDY(9.0).entryPrice(34.00).ceilingPrice(40.00).targetAllocation(10.0)
                                .rationale("Transmissão de energia, dividendos mensais, proteção inflacionária").build(),
                        RecommendedAsset.builder().rank(7)
                                .ticker("HGLG11").name("CSHG Logística").type("FII")
                                .expectedDY(8.5).entryPrice(155.00).ceilingPrice(175.00).targetAllocation(10.0)
                                .rationale("Maior FII de galpões logísticos, contratos longos, vacância baixa").build(),
                        RecommendedAsset.builder().rank(8)
                                .ticker("MXRF11").name("Maxi Renda").type("FII")
                                .expectedDY(11.0).entryPrice(9.50).ceilingPrice(11.00).targetAllocation(10.0)
                                .rationale("FII de papel diversificado, gestão sólida, DY atrativo").build(),
                        RecommendedAsset.builder().rank(9)
                                .ticker("VISC11").name("Vinci Shopping Centers").type("FII")
                                .expectedDY(8.0).entryPrice(105.00).ceilingPrice(120.00).targetAllocation(10.0)
                                .rationale("Shoppings alto padrão, vacância baixa, gestão ativa").build(),
                        RecommendedAsset.builder().rank(10)
                                .ticker("IPCA+2035").name("Tesouro IPCA+ 2035").type("Renda Fixa")
                                .expectedDY(null).entryPrice(null).ceilingPrice(null).targetAllocation(10.0)
                                .rationale("Proteção inflação, rentabilidade real garantida, risco soberano").build()
                ))
                .characteristics(Arrays.asList(
                        "Foco em fundamentos sólidos",
                        "Horizonte de longo prazo (5+ anos)",
                        "Menor volatilidade que growth stocks",
                        "Empresas com vantagens competitivas",
                        "Rebalanceamento anual"
                ))
                .build());

        // 2. Carteira de Dividendos
        portfolios.add(InvestmentPortfolio.builder()
                .name("Carteira de Dividendos")
                .description("Prioriza renda passiva através de ações e FIIs pagadores de dividendos consistentes, ideal para quem busca fluxo de caixa mensal.")
                .strategy("Renda passiva, reinvestimento de proventos, diversificação setorial")
                .riskLevel("Baixo a Moderado")
                .icon("💰")
                .suggestedComposition(Arrays.asList(
                        PortfolioAsset.builder().type("Ações").percentage(40).description("Ações High Dividend Yield").build(),
                        PortfolioAsset.builder().type("FIIs").percentage(40).description("FIIs de Tijolo e Papel").build(),
                        PortfolioAsset.builder().type("Renda Fixa").percentage(20).description("CDBs e LCIs").build()
                ))
                .recommendedAssets(Arrays.asList(
                        RecommendedAsset.builder().rank(1)
                                .ticker("TAEE11").name("Taesa").type("Ação")
                                .expectedDY(9.5).entryPrice(34.00).ceilingPrice(40.00).targetAllocation(10.0)
                                .rationale("DY 9-10%, pagamento mensal, transmissão regulada, baixa volatilidade").build(),
                        RecommendedAsset.builder().rank(2)
                                .ticker("TRPL4").name("Transmissão Paulista").type("Ação")
                                .expectedDY(8.5).entryPrice(24.00).ceilingPrice(28.00).targetAllocation(10.0)
                                .rationale("Setor regulado, dividendos mensais, receita indexada à inflação").build(),
                        RecommendedAsset.builder().rank(3)
                                .ticker("BBSE3").name("BB Seguridade").type("Ação")
                                .expectedDY(8.0).entryPrice(32.00).ceilingPrice(38.00).targetAllocation(10.0)
                                .rationale("Payout >90%, dividendos trimestrais, negócio defensivo").build(),
                        RecommendedAsset.builder().rank(4)
                                .ticker("CPLE6").name("Copel").type("Ação")
                                .expectedDY(7.5).entryPrice(9.50).ceilingPrice(11.50).targetAllocation(10.0)
                                .rationale("Elétrica privatizada, dividendos expressivos, DY 6-8%").build(),
                        RecommendedAsset.builder().rank(5)
                                .ticker("HGLG11").name("CSHG Logística").type("FII")
                                .expectedDY(9.0).entryPrice(155.00).ceilingPrice(175.00).targetAllocation(10.0)
                                .rationale("Maior FII galpões, DY 9-11%, contratos longos, vacância baixa").build(),
                        RecommendedAsset.builder().rank(6)
                                .ticker("KNRI11").name("Kinea Renda Imob").type("FII")
                                .expectedDY(10.5).entryPrice(130.00).ceilingPrice(150.00).targetAllocation(10.0)
                                .rationale("FII híbrido, gestão Kinea/Itaú, DY 10-12%, diversificado").build(),
                        RecommendedAsset.builder().rank(7)
                                .ticker("PVBI11").name("VBI Prime Properties").type("FII")
                                .expectedDY(8.5).entryPrice(85.00).ceilingPrice(100.00).targetAllocation(10.0)
                                .rationale("Lajes corporativas AAA, inquilinos premium, DY 8-10%").build(),
                        RecommendedAsset.builder().rank(8)
                                .ticker("XPML11").name("XP Malls").type("FII")
                                .expectedDY(9.5).entryPrice(95.00).ceilingPrice(110.00).targetAllocation(10.0)
                                .rationale("Shoppings regionais, DY 9-11%, gestão XP Asset").build(),
                        RecommendedAsset.builder().rank(9)
                                .ticker("MXRF11").name("Maxi Renda").type("FII")
                                .expectedDY(11.5).entryPrice(9.50).ceilingPrice(11.00).targetAllocation(10.0)
                                .rationale("FII papel diversificado, gestão sólida, DY atrativo 11%+").build(),
                        RecommendedAsset.builder().rank(10)
                                .ticker("CDB-CDI").name("CDB 100% CDI").type("Renda Fixa")
                                .expectedDY(null).entryPrice(null).ceilingPrice(null).targetAllocation(10.0)
                                .rationale("Liquidez diária, proteção FGC, rendimento pós-fixado").build()
                ))
                .characteristics(Arrays.asList(
                        "Pagamento mensal de dividendos",
                        "Empresas com histórico consistente",
                        "Diversificação entre setores",
                        "Fluxo de caixa previsível",
                        "Proteção contra inflação"
                ))
                .build());

        // 3. Carteira Internacional
        portfolios.add(InvestmentPortfolio.builder()
                .name("Carteira Internacional")
                .description("Diversificação geográfica através de ETFs e ações americanas, com exposição ao dólar e economia global.")
                .strategy("Diversificação geográfica, exposição cambial, investimento em empresas globais")
                .riskLevel("Moderado a Alto")
                .icon("🌎")
                .suggestedComposition(Arrays.asList(
                        PortfolioAsset.builder().type("ETFs S&P 500").percentage(50).description("VOO, SPY, IVV").build(),
                        PortfolioAsset.builder().type("ETFs Mundo").percentage(30).description("VT, ACWI").build(),
                        PortfolioAsset.builder().type("REITs").percentage(20).description("Fundos Imobiliários US").build()
                ))
                .recommendedAssets(Arrays.asList(
                        RecommendedAsset.builder().rank(1)
                                .ticker("IVVB11").name("iShares S&P 500").type("ETF")
                                .expectedDY(1.3).entryPrice(280.00).ceilingPrice(320.00).targetAllocation(25.0)
                                .rationale("Replica S&P 500, negociado em BRL na B3, baixa taxa (0.20% a.a.), exposição às 500 maiores empresas dos EUA, liquidez").build(),
                        RecommendedAsset.builder().rank(2)
                                .ticker("WRLD11").name("iShares MSCI World").type("ETF")
                                .expectedDY(1.0).entryPrice(45.00).ceilingPrice(55.00).targetAllocation(20.0)
                                .rationale("Diversificação global (23 países desenvolvidos), ~1600 empresas, gestão passiva, taxa 0.45% a.a.").build(),
                        RecommendedAsset.builder().rank(3)
                                .ticker("GOLD11").name("ETF Ouro (It Now)").type("ETF")
                                .expectedDY(null).entryPrice(10.00).ceilingPrice(12.00).targetAllocation(10.0)
                                .rationale("Hedge contra inflação e crise, descorrelacionado de ações, lastro em ouro físico, proteção em momentos de volatilidade").build(),
                        RecommendedAsset.builder().rank(4)
                                .ticker("VT").name("Vanguard Total World Stock").type("ETF Internacional")
                                .expectedDY(1.8).entryPrice(null).ceilingPrice(null).targetAllocation(20.0)
                                .rationale("Compra via Avenue/Inter, +9000 ações globais (desenvolvidos + emergentes), máxima diversificação, taxa 0.07% a.a.").build(),
                        RecommendedAsset.builder().rank(5)
                                .ticker("VOO").name("Vanguard S&P 500").type("ETF Internacional")
                                .expectedDY(1.5).entryPrice(null).ceilingPrice(null).targetAllocation(15.0)
                                .rationale("Compra via corretora internacional, menor taxa (0.03% a.a.), exposição direta em USD, dividendos em USD").build(),
                        RecommendedAsset.builder().rank(6)
                                .ticker("Tesouro Selic").name("Tesouro Selic").type("Renda Fixa")
                                .expectedDY(null).entryPrice(null).ceilingPrice(null).targetAllocation(10.0)
                                .rationale("Proteção em BRL, liquidez para rebalanceamento cambial, hedge parcial contra desvalorização do dólar").build()
                ))
                .characteristics(Arrays.asList(
                        "Proteção cambial (USD)",
                        "Acesso a empresas globais",
                        "Diversificação geográfica",
                        "Baixo custo através de ETFs",
                        "Tributação simplificada"
                ))
                .build());

        // 4. Carteira Small Caps
        portfolios.add(InvestmentPortfolio.builder()
                .name("Carteira Small Caps")
                .description("Foco em empresas de menor capitalização com alto potencial de crescimento, adequada para perfil agressivo e horizonte longo.")
                .strategy("Crescimento agressivo, alta volatilidade, seleção criteriosa de empresas emergentes")
                .riskLevel("Alto")
                .icon("🚀")
                .suggestedComposition(Arrays.asList(
                        PortfolioAsset.builder().type("Small Caps").percentage(70).description("Ações de empresas em crescimento").build(),
                        PortfolioAsset.builder().type("Mid Caps").percentage(20).description("Empresas em consolidação").build(),
                        PortfolioAsset.builder().type("Renda Fixa").percentage(10).description("Proteção mínima").build()
                ))
                .recommendedAssets(Arrays.asList(
                        RecommendedAsset.builder().rank(1)
                                .ticker("LWSA3").name("Locaweb").type("Ação")
                                .expectedDY(0.5).entryPrice(4.50).ceilingPrice(6.50).targetAllocation(12.0)
                                .rationale("Líder em hospedagem e e-commerce para PMEs, receita recorrente (SaaS), crescimento 20%+ a.a., mercado digital em expansão").build(),
                        RecommendedAsset.builder().rank(2)
                                .ticker("RDOR3").name("Rede D'Or").type("Ação")
                                .expectedDY(0.8).entryPrice(26.00).ceilingPrice(32.00).targetAllocation(12.0)
                                .rationale("Maior rede de hospitais privados do Brasil, aquisições estratégicas, setor defensivo com crescimento, plano de expansão agressivo").build(),
                        RecommendedAsset.builder().rank(3)
                                .ticker("SIMH3").name("Simpar").type("Ação")
                                .expectedDY(2.5).entryPrice(6.00).ceilingPrice(8.50).targetAllocation(10.0)
                                .rationale("Holding de mobilidade (Movida, JSL, Vamos), crescimento via M&A, beneficiada por renovação de frota e logística").build(),
                        RecommendedAsset.builder().rank(4)
                                .ticker("AURE3").name("Auren Energia").type("Ação")
                                .expectedDY(6.5).entryPrice(11.00).ceilingPrice(14.00).targetAllocation(10.0)
                                .rationale("Geração renovável (solar e eólica), expansão em energia limpa, contratos de longo prazo, ESG, setor em crescimento").build(),
                        RecommendedAsset.builder().rank(5)
                                .ticker("MDIA3").name("M.Dias Branco").type("Ação")
                                .expectedDY(3.2).entryPrice(28.00).ceilingPrice(35.00).targetAllocation(10.0)
                                .rationale("Líder em massas e biscoitos no Nordeste, expansão nacional, margens em recuperação, poder de pricing, setor defensivo").build(),
                        RecommendedAsset.builder().rank(6)
                                .ticker("BPAN4").name("Banco Pan").type("Ação")
                                .expectedDY(4.0).entryPrice(7.00).ceilingPrice(10.00).targetAllocation(8.0)
                                .rationale("Foco em crédito consignado e parcerias digitais, crescimento agressivo, turnaround em curso, ROE em alta").build(),
                        RecommendedAsset.builder().rank(7)
                                .ticker("CMIN3").name("CSN Mineração").type("Ação")
                                .expectedDY(8.0).entryPrice(5.00).ceilingPrice(7.00).targetAllocation(8.0)
                                .rationale("Exportador de minério de ferro, leverage operacional ao preço do minério, menor que VALE3, potencial de valorização").build(),
                        RecommendedAsset.builder().rank(8)
                                .ticker("ARZZ3").name("Arezzo").type("Ação")
                                .expectedDY(2.0).entryPrice(52.00).ceilingPrice(68.00).targetAllocation(10.0)
                                .rationale("Marcas fortes (Arezzo, Schutz, Vans), omnichannel bem executado, expansão internacional, ROIC elevado, gestão de excelência").build(),
                        RecommendedAsset.builder().rank(9)
                                .ticker("RENT3").name("Localiza").type("Ação")
                                .expectedDY(2.5).entryPrice(42.00).ceilingPrice(55.00).targetAllocation(10.0)
                                .rationale("Líder em aluguel de veículos, consolidação do setor, sinergias com Unidas, crescimento via seminovos, economia de escala").build(),
                        RecommendedAsset.builder().rank(10)
                                .ticker("Tesouro Selic").name("Tesouro Selic").type("Renda Fixa")
                                .expectedDY(null).entryPrice(null).ceilingPrice(null).targetAllocation(10.0)
                                .rationale("Proteção mínima, liquidez para aproveitar oportunidades em quedas, rebalanceamento tático").build()
                ))
                .characteristics(Arrays.asList(
                        "Alto potencial de retorno",
                        "Alta volatilidade",
                        "Requer análise detalhada",
                        "Menor liquidez que blue chips",
                        "Horizonte de longo prazo obrigatório"
                ))
                .build());

        // 5. Carteira de Renda Fixa
        portfolios.add(InvestmentPortfolio.builder()
                .name("Carteira de Renda Fixa")
                .description("Focada em preservação de capital e previsibilidade de retornos através de títulos públicos e privados de baixo risco.")
                .strategy("Preservação de capital, diversificação de vencimentos, aproveitamento de juros compostos")
                .riskLevel("Baixo")
                .icon("🏦")
                .suggestedComposition(Arrays.asList(
                        PortfolioAsset.builder().type("Tesouro").percentage(50).description("Tesouro Selic e IPCA+").build(),
                        PortfolioAsset.builder().type("CDBs").percentage(30).description("CDBs com garantia do FGC").build(),
                        PortfolioAsset.builder().type("LCI/LCA").percentage(20).description("Isentos de IR").build()
                ))
                .recommendedAssets(Arrays.asList(
                        RecommendedAsset.builder().rank(1)
                                .ticker("SELIC-2029").name("Tesouro Selic 2029").type("Renda Fixa")
                                .expectedDY(null).entryPrice(null).ceilingPrice(null).targetAllocation(30.0)
                                .rationale("Liquidez diária, pós-fixado ~13% a.a., risco soberano, reserva emergência").build(),
                        RecommendedAsset.builder().rank(2)
                                .ticker("IPCA+2035").name("Tesouro IPCA+ 2035").type("Renda Fixa")
                                .expectedDY(null).entryPrice(null).ceilingPrice(null).targetAllocation(25.0)
                                .rationale("Proteção inflação, IPCA + 6% a.a., médio prazo, risco soberano").build(),
                        RecommendedAsset.builder().rank(3)
                                .ticker("CDB-110CDI").name("CDB 110% CDI").type("Renda Fixa")
                                .expectedDY(null).entryPrice(null).ceilingPrice(null).targetAllocation(20.0)
                                .rationale("Rentabilidade CDI+10%, garantia FGC R$250k, bancos grandes").build(),
                        RecommendedAsset.builder().rank(4)
                                .ticker("LCI-95CDI").name("LCI 95% CDI").type("Renda Fixa")
                                .expectedDY(null).entryPrice(null).ceilingPrice(null).targetAllocation(15.0)
                                .rationale("Isento IR (líquido ~108% CDI), garantia FGC, lastro imobiliário").build(),
                        RecommendedAsset.builder().rank(5)
                                .ticker("DEB-INFRA").name("Debênture Infra AAA").type("Renda Fixa")
                                .expectedDY(null).entryPrice(null).ceilingPrice(null).targetAllocation(10.0)
                                .rationale("Isento IR, rating AAA, IPCA + 6%, projetos infraestrutura").build()
                ))
                .characteristics(Arrays.asList(
                        "Baixíssimo risco",
                        "Liquidez diária (Tesouro Selic)",
                        "Proteção contra inflação (IPCA+)",
                        "Garantia do FGC até R$ 250k",
                        "Ideal para reserva de emergência"
                ))
                .build());

        // 6. Carteira Cripto
        portfolios.add(InvestmentPortfolio.builder()
                .name("Carteira de Criptomoedas")
                .description("Exposição a ativos digitais com foco em Bitcoin e Ethereum, incluindo stablecoins para liquidez e gerenciamento de risco.")
                .strategy("Buy and Hold, Dollar Cost Averaging (DCA), rebalanceamento trimestral")
                .riskLevel("Muito Alto")
                .icon("₿")
                .suggestedComposition(Arrays.asList(
                        PortfolioAsset.builder().type("Bitcoin").percentage(50).description("BTC - Reserve Digital").build(),
                        PortfolioAsset.builder().type("Ethereum").percentage(30).description("ETH - Smart Contracts").build(),
                        PortfolioAsset.builder().type("Stablecoins").percentage(10).description("USDT, USDC").build(),
                        PortfolioAsset.builder().type("Altcoins").percentage(10).description("Projetos selecionados").build()
                ))
                .recommendedAssets(Arrays.asList(
                        RecommendedAsset.builder().rank(1)
                                .ticker("BTC").name("Bitcoin").type("Cripto")
                                .expectedDY(null).entryPrice(350000.00).ceilingPrice(500000.00).targetAllocation(50.0)
                                .rationale("Reserva de valor digital, oferta limitada (21M), adoção institucional crescente, 'ouro digital', menor volatilidade relativa, liquidez global").build(),
                        RecommendedAsset.builder().rank(2)
                                .ticker("ETH").name("Ethereum").type("Cripto")
                                .expectedDY(null).entryPrice(15000.00).ceilingPrice(22000.00).targetAllocation(30.0)
                                .rationale("Plataforma de smart contracts líder, base de DeFi e NFTs, transição para PoS (menor consumo energético), queima de tokens (deflacionário), ecossistema robusto").build(),
                        RecommendedAsset.builder().rank(3)
                                .ticker("USDC").name("USD Coin").type("Stablecoin")
                                .expectedDY(null).entryPrice(5.50).ceilingPrice(null).targetAllocation(10.0)
                                .rationale("Stablecoin lastreada 1:1 em USD, auditoria regular pela Circle, liquidez para rebalanceamento e compras em quedas, menor risco regulatório que USDT").build(),
                        RecommendedAsset.builder().rank(4)
                                .ticker("SOL").name("Solana").type("Cripto")
                                .expectedDY(null).entryPrice(700.00).ceilingPrice(1200.00).targetAllocation(5.0)
                                .rationale("Blockchain de alta performance (50k TPS), ecossistema DeFi crescente, custos baixos, alternativa ao Ethereum para aplicações escaláveis").build(),
                        RecommendedAsset.builder().rank(5)
                                .ticker("LINK").name("Chainlink").type("Cripto")
                                .expectedDY(null).entryPrice(70.00).ceilingPrice(120.00).targetAllocation(3.0)
                                .rationale("Líder em oráculos descentralizados, infraestrutura crítica para DeFi, parcerias com SWIFT e grandes bancos, casos de uso reais").build(),
                        RecommendedAsset.builder().rank(6)
                                .ticker("MATIC").name("Polygon").type("Cripto")
                                .expectedDY(null).entryPrice(2.50).ceilingPrice(5.00).targetAllocation(2.0)
                                .rationale("Solução de layer 2 para Ethereum, adoção corporativa (Disney, Starbucks), escalabilidade, custos reduzidos, integração com Ethereum").build()
                ))
                .characteristics(Arrays.asList(
                        "Altíssima volatilidade",
                        "Descorrelacionado de ativos tradicionais",
                        "Potencial de retornos exponenciais",
                        "Risco de perda total",
                        "Custódia própria recomendada"
                ))
                .build());

        return portfolios;
    }
}
