package com.managehouse.money.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.managehouse.money.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class MarketDataService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Cache para evitar rate limit das APIs externas (especialmente CoinGecko)
    private CryptoDataResponse cachedCryptoPrices = null;
    private Instant lastCryptoFetch = null;
    private static final long CACHE_DURATION_SECONDS = 60; // Cache por 1 minuto

    private ForexDataResponse cachedForexRates = null;
    private Instant lastForexFetch = null;

    public MarketDataDashboardResponse getDashboard() {
        log.info("Fetching market data dashboard");

        return MarketDataDashboardResponse.builder()
                .forex(getForexRates())
                .brazilianIndices(getBrazilianIndices())
                .usIndices(getUSIndices())
                .crypto(getCryptoPrices())
                .lastUpdate(Instant.now().toString())
                .build();
    }

    public ForexDataResponse getForexRates() {
        // Verificar cache
        if (cachedForexRates != null && lastForexFetch != null) {
            long secondsSinceLastFetch = Instant.now().getEpochSecond() - lastForexFetch.getEpochSecond();
            if (secondsSinceLastFetch < CACHE_DURATION_SECONDS) {
                log.debug("Returning cached forex rates ({}s old)", secondsSinceLastFetch);
                return cachedForexRates;
            }
        }

        try {
            log.info("Fetching forex rates from AwesomeAPI");
            String url = "https://economia.awesomeapi.com.br/json/last/USD-BRL,EUR-BRL,GBP-BRL,JPY-BRL";
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            ForexDataResponse result = ForexDataResponse.builder()
                    .usd(parseForexData(root.get("USDBRL"), "USD/BRL", "Dólar Americano"))
                    .eur(parseForexData(root.get("EURBRL"), "EUR/BRL", "Euro"))
                    .gbp(parseForexData(root.get("GBPBRL"), "GBP/BRL", "Libra Esterlina"))
                    .jpy(parseForexData(root.get("JPYBRL"), "JPY/BRL", "Iene Japonês"))
                    .build();

            // Atualizar cache
            cachedForexRates = result;
            lastForexFetch = Instant.now();

            return result;
        } catch (Exception e) {
            log.error("Error fetching forex rates: {}", e.getMessage());
            if (cachedForexRates != null) {
                log.warn("Returning stale cached forex rates due to API error");
                return cachedForexRates;
            }
            return getDefaultForexRates();
        }
    }

    public BrazilianIndicesResponse getBrazilianIndices() {
        // TODO: Integrate with Brapi.dev API when token is available
        // For now, returning mock data
        log.info("Returning mock Brazilian indices data");

        return BrazilianIndicesResponse.builder()
                .ibovespa(createMockIndex("^BVSP", "Ibovespa", new BigDecimal("128450.32"), 1.24))
                .ifix(createMockIndex("IFIX", "IFIX", new BigDecimal("3240.89"), -0.32))
                .idiv(createMockIndex("IDIV", "IDIV", new BigDecimal("8920.45"), 0.85))
                .build();
    }

    public USIndicesResponse getUSIndices() {
        // TODO: Integrate with Yahoo Finance or Brapi.dev API
        // For now, returning mock data
        log.info("Returning mock US indices data");

        return USIndicesResponse.builder()
                .sp500(createMockIndex("^GSPC", "S&P 500", new BigDecimal("4850.23"), 0.72))
                .nasdaq(createMockIndex("^IXIC", "Nasdaq", new BigDecimal("15230.87"), 1.15))
                .dow(createMockIndex("^DJI", "Dow Jones", new BigDecimal("38450.12"), 0.53))
                .build();
    }

    public CryptoDataResponse getCryptoPrices() {
        // Verificar cache para evitar rate limit
        if (cachedCryptoPrices != null && lastCryptoFetch != null) {
            long secondsSinceLastFetch = Instant.now().getEpochSecond() - lastCryptoFetch.getEpochSecond();
            if (secondsSinceLastFetch < CACHE_DURATION_SECONDS) {
                log.debug("Returning cached crypto prices ({}s old)", secondsSinceLastFetch);
                return cachedCryptoPrices;
            }
        }

        // Tentar Binance primeiro (mais confiavel, sem rate limit)
        CryptoDataResponse result = fetchFromBinance();
        if (result != null) {
            cachedCryptoPrices = result;
            lastCryptoFetch = Instant.now();
            return result;
        }

        // Fallback para CoinGecko
        result = fetchFromCoinGecko();
        if (result != null) {
            cachedCryptoPrices = result;
            lastCryptoFetch = Instant.now();
            return result;
        }

        // Se temos cache, retornar dados em cache mesmo que expirados
        if (cachedCryptoPrices != null) {
            log.warn("Returning stale cached crypto prices due to API errors");
            return cachedCryptoPrices;
        }

        return getDefaultCryptoPrices();
    }

    private CryptoDataResponse fetchFromBinance() {
        try {
            log.info("Fetching crypto prices from Binance API");
            // Binance API - buscar precos em BRL
            String url = "https://api.binance.com/api/v3/ticker/24hr?symbols=[\"BTCBRL\",\"ETHBRL\",\"BNBBRL\",\"USDTBRL\"]";
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            MarketIndexData bitcoin = null;
            MarketIndexData ethereum = null;
            List<MarketIndexData> otherCoins = new ArrayList<>();

            for (JsonNode ticker : root) {
                String symbol = ticker.get("symbol").asText();
                double lastPrice = ticker.get("lastPrice").asDouble();
                double priceChangePercent = ticker.get("priceChangePercent").asDouble();

                MarketIndexData data = MarketIndexData.builder()
                        .symbol(symbol.replace("BRL", ""))
                        .name(getFullName(symbol))
                        .value(BigDecimal.valueOf(lastPrice).setScale(2, RoundingMode.HALF_UP))
                        .change(priceChangePercent)
                        .trend(priceChangePercent > 0 ? "up" : priceChangePercent < 0 ? "down" : "neutral")
                        .lastUpdate(Instant.now().toString())
                        .build();

                switch (symbol) {
                    case "BTCBRL" -> bitcoin = data;
                    case "ETHBRL" -> ethereum = data;
                    default -> otherCoins.add(data);
                }
            }

            // Adicionar USDC com valor aproximado do USDT (stablecoins)
            if (!otherCoins.isEmpty()) {
                MarketIndexData usdt = otherCoins.stream()
                        .filter(c -> "USDT".equals(c.getSymbol()))
                        .findFirst()
                        .orElse(null);
                if (usdt != null) {
                    otherCoins.add(MarketIndexData.builder()
                            .symbol("USDC")
                            .name("USD Coin")
                            .value(usdt.getValue())
                            .change(usdt.getChange())
                            .trend(usdt.getTrend())
                            .lastUpdate(Instant.now().toString())
                            .build());
                }
            }

            log.info("Binance API success: BTC={}, ETH={}",
                    bitcoin != null ? bitcoin.getValue() : "null",
                    ethereum != null ? ethereum.getValue() : "null");

            return CryptoDataResponse.builder()
                    .bitcoin(bitcoin)
                    .ethereum(ethereum)
                    .otherCoins(otherCoins)
                    .build();
        } catch (Exception e) {
            log.warn("Binance API failed: {}", e.getMessage());
            return null;
        }
    }

    private CryptoDataResponse fetchFromCoinGecko() {
        try {
            log.info("Fetching crypto prices from CoinGecko (fallback)");
            String url = "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum,binancecoin,tether,usd-coin&vs_currencies=brl&include_24hr_change=true";
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            List<MarketIndexData> otherCoins = new ArrayList<>();
            otherCoins.add(parseCryptoData(root.get("binancecoin"), "BNB", "Binance Coin"));
            otherCoins.add(parseCryptoData(root.get("tether"), "USDT", "Tether"));
            otherCoins.add(parseCryptoData(root.get("usd-coin"), "USDC", "USD Coin"));

            log.info("CoinGecko API success");

            return CryptoDataResponse.builder()
                    .bitcoin(parseCryptoData(root.get("bitcoin"), "BTC", "Bitcoin"))
                    .ethereum(parseCryptoData(root.get("ethereum"), "ETH", "Ethereum"))
                    .otherCoins(otherCoins)
                    .build();
        } catch (Exception e) {
            log.warn("CoinGecko API failed: {}", e.getMessage());
            return null;
        }
    }

    private String getFullName(String symbol) {
        return switch (symbol) {
            case "BTCBRL" -> "Bitcoin";
            case "ETHBRL" -> "Ethereum";
            case "BNBBRL" -> "Binance Coin";
            case "USDTBRL" -> "Tether";
            default -> symbol;
        };
    }

    private MarketIndexData parseForexData(JsonNode node, String symbol, String name) {
        double bid = node.get("bid").asDouble();
        double pctChange = node.get("pctChange").asDouble();
        String createDate = node.get("create_date").asText();

        return MarketIndexData.builder()
                .symbol(symbol)
                .name(name)
                .value(BigDecimal.valueOf(bid).setScale(4, RoundingMode.HALF_UP))
                .change(pctChange)
                .trend(pctChange > 0 ? "up" : pctChange < 0 ? "down" : "neutral")
                .lastUpdate(createDate)
                .build();
    }

    private MarketIndexData parseCryptoData(JsonNode node, String symbol, String name) {
        double brl = node.get("brl").asDouble();
        double change24h = node.get("brl_24h_change").asDouble();

        return MarketIndexData.builder()
                .symbol(symbol)
                .name(name)
                .value(BigDecimal.valueOf(brl).setScale(2, RoundingMode.HALF_UP))
                .change(change24h)
                .trend(change24h > 0 ? "up" : change24h < 0 ? "down" : "neutral")
                .lastUpdate(Instant.now().toString())
                .build();
    }

    private MarketIndexData createMockIndex(String symbol, String name, BigDecimal value, double change) {
        return MarketIndexData.builder()
                .symbol(symbol)
                .name(name)
                .value(value)
                .change(change)
                .trend(change > 0 ? "up" : change < 0 ? "down" : "neutral")
                .lastUpdate(Instant.now().toString())
                .build();
    }

    private ForexDataResponse getDefaultForexRates() {
        return ForexDataResponse.builder()
                .usd(createMockIndex("USD/BRL", "Dólar Americano", new BigDecimal("5.37"), -0.5))
                .eur(createMockIndex("EUR/BRL", "Euro", new BigDecimal("6.27"), 0.34))
                .gbp(createMockIndex("GBP/BRL", "Libra Esterlina", new BigDecimal("7.23"), 0.51))
                .jpy(createMockIndex("JPY/BRL", "Iene Japonês", new BigDecimal("0.034"), 0.0))
                .build();
    }

    private CryptoDataResponse getDefaultCryptoPrices() {
        List<MarketIndexData> otherCoins = new ArrayList<>();
        otherCoins.add(createMockIndex("BNB", "Binance Coin", new BigDecimal("4821.73"), -1.78));
        otherCoins.add(createMockIndex("USDT", "Tether", new BigDecimal("5.37"), 0.03));
        otherCoins.add(createMockIndex("USDC", "USD Coin", new BigDecimal("5.37"), -0.01));

        return CryptoDataResponse.builder()
                .bitcoin(createMockIndex("BTC", "Bitcoin", new BigDecimal("485779"), -0.46))
                .ethereum(createMockIndex("ETH", "Ethereum", new BigDecimal("16633.23"), -0.44))
                .otherCoins(otherCoins)
                .build();
    }

    /**
     * Retorna as carteiras de investimento com ativos recomendados
     */
    public List<InvestmentPortfolio> getPortfolios() {
        List<InvestmentPortfolio> portfolios = new ArrayList<>();

        // 1. Carteira de Valor
        portfolios.add(InvestmentPortfolio.builder()
                .name("Carteira de Valor")
                .description("Foca em empresas consolidadas e subavaliadas pelo mercado.")
                .strategy("Buy and Hold, análise fundamentalista")
                .riskLevel("Moderado")
                .icon("💎")
                .suggestedComposition(Arrays.asList(
                        PortfolioAsset.builder().type("Ações").percentage(60).description("Ações Blue Chips").build(),
                        PortfolioAsset.builder().type("FIIs").percentage(30).description("Fundos Imobiliários").build(),
                        PortfolioAsset.builder().type("Renda Fixa").percentage(10).description("Tesouro Direto").build()
                ))
                .recommendedAssets(Arrays.asList(
                        RecommendedAsset.builder().rank(1).ticker("VALE3").name("Vale").type("Ação")
                                .expectedDY(9.5).entryPrice(58.00).ceilingPrice(72.00).targetAllocation(10.0)
                                .rationale("P/L 5-6x, líder global minério de ferro").build(),
                        RecommendedAsset.builder().rank(2).ticker("BBAS3").name("Banco do Brasil").type("Ação")
                                .expectedDY(10.2).entryPrice(26.00).ceilingPrice(32.00).targetAllocation(10.0)
                                .rationale("P/VP < 1.0, ROE 15%+, dividendos regulares").build(),
                        RecommendedAsset.builder().rank(3).ticker("ITUB4").name("Itaú Unibanco").type("Ação")
                                .expectedDY(6.5).entryPrice(30.00).ceilingPrice(38.00).targetAllocation(10.0)
                                .rationale("Maior banco privado LATAM, ROE 20%+").build(),
                        RecommendedAsset.builder().rank(4).ticker("PETR4").name("Petrobras").type("Ação")
                                .expectedDY(15.0).entryPrice(36.00).ceilingPrice(45.00).targetAllocation(10.0)
                                .rationale("P/L 3-4x, alta geração de caixa").build(),
                        RecommendedAsset.builder().rank(5).ticker("EGIE3").name("Engie Brasil").type("Ação")
                                .expectedDY(7.5).entryPrice(40.00).ceilingPrice(48.00).targetAllocation(10.0)
                                .rationale("Setor elétrico regulado, receita previsível").build(),
                        RecommendedAsset.builder().rank(6).ticker("TAEE11").name("Taesa").type("Ação")
                                .expectedDY(9.0).entryPrice(34.00).ceilingPrice(40.00).targetAllocation(10.0)
                                .rationale("Transmissão de energia, dividendos mensais").build(),
                        RecommendedAsset.builder().rank(7).ticker("HGLG11").name("CSHG Logística").type("FII")
                                .expectedDY(8.5).entryPrice(155.00).ceilingPrice(175.00).targetAllocation(10.0)
                                .rationale("Maior FII de galpões logísticos").build(),
                        RecommendedAsset.builder().rank(8).ticker("MXRF11").name("Maxi Renda").type("FII")
                                .expectedDY(11.0).entryPrice(9.50).ceilingPrice(11.00).targetAllocation(10.0)
                                .rationale("FII de papel diversificado, gestão sólida").build(),
                        RecommendedAsset.builder().rank(9).ticker("VISC11").name("Vinci Shopping Centers").type("FII")
                                .expectedDY(8.0).entryPrice(105.00).ceilingPrice(120.00).targetAllocation(10.0)
                                .rationale("Shoppings alto padrão, vacância baixa").build(),
                        RecommendedAsset.builder().rank(10).ticker("IPCA+2035").name("Tesouro IPCA+ 2035").type("Renda Fixa")
                                .expectedDY(null).entryPrice(null).ceilingPrice(null).targetAllocation(10.0)
                                .rationale("Proteção inflação, rentabilidade real garantida").build()
                ))
                .characteristics(Arrays.asList("Foco em fundamentos sólidos", "Horizonte de longo prazo"))
                .build());

        // 2. Carteira de Dividendos
        portfolios.add(InvestmentPortfolio.builder()
                .name("Carteira de Dividendos")
                .description("Prioriza renda passiva através de dividendos consistentes.")
                .strategy("Renda passiva, reinvestimento de proventos")
                .riskLevel("Baixo a Moderado")
                .icon("💰")
                .suggestedComposition(Arrays.asList(
                        PortfolioAsset.builder().type("Ações").percentage(40).description("Ações High DY").build(),
                        PortfolioAsset.builder().type("FIIs").percentage(40).description("FIIs de Tijolo e Papel").build(),
                        PortfolioAsset.builder().type("Renda Fixa").percentage(20).description("CDBs e LCIs").build()
                ))
                .recommendedAssets(Arrays.asList(
                        RecommendedAsset.builder().rank(1).ticker("TAEE11").name("Taesa").type("Ação")
                                .expectedDY(9.5).entryPrice(34.00).ceilingPrice(40.00).targetAllocation(10.0)
                                .rationale("DY 9-10%, pagamento mensal").build(),
                        RecommendedAsset.builder().rank(2).ticker("TRPL4").name("Transmissão Paulista").type("Ação")
                                .expectedDY(8.5).entryPrice(24.00).ceilingPrice(28.00).targetAllocation(10.0)
                                .rationale("Setor regulado, dividendos mensais").build(),
                        RecommendedAsset.builder().rank(3).ticker("BBSE3").name("BB Seguridade").type("Ação")
                                .expectedDY(8.0).entryPrice(32.00).ceilingPrice(38.00).targetAllocation(10.0)
                                .rationale("Payout >90%, dividendos trimestrais").build(),
                        RecommendedAsset.builder().rank(4).ticker("CPLE6").name("Copel").type("Ação")
                                .expectedDY(7.5).entryPrice(9.50).ceilingPrice(11.50).targetAllocation(10.0)
                                .rationale("Elétrica privatizada, DY 6-8%").build(),
                        RecommendedAsset.builder().rank(5).ticker("HGLG11").name("CSHG Logística").type("FII")
                                .expectedDY(9.0).entryPrice(155.00).ceilingPrice(175.00).targetAllocation(10.0)
                                .rationale("Maior FII galpões, DY 9-11%").build(),
                        RecommendedAsset.builder().rank(6).ticker("KNRI11").name("Kinea Renda Imob").type("FII")
                                .expectedDY(10.5).entryPrice(130.00).ceilingPrice(150.00).targetAllocation(10.0)
                                .rationale("FII híbrido, gestão Kinea/Itaú").build(),
                        RecommendedAsset.builder().rank(7).ticker("PVBI11").name("VBI Prime Properties").type("FII")
                                .expectedDY(8.5).entryPrice(85.00).ceilingPrice(100.00).targetAllocation(10.0)
                                .rationale("Lajes corporativas AAA").build(),
                        RecommendedAsset.builder().rank(8).ticker("XPML11").name("XP Malls").type("FII")
                                .expectedDY(9.5).entryPrice(95.00).ceilingPrice(110.00).targetAllocation(10.0)
                                .rationale("Shoppings regionais, DY 9-11%").build(),
                        RecommendedAsset.builder().rank(9).ticker("MXRF11").name("Maxi Renda").type("FII")
                                .expectedDY(11.5).entryPrice(9.50).ceilingPrice(11.00).targetAllocation(10.0)
                                .rationale("FII papel diversificado, DY 11%+").build(),
                        RecommendedAsset.builder().rank(10).ticker("CDB-CDI").name("CDB 100% CDI").type("Renda Fixa")
                                .expectedDY(null).entryPrice(null).ceilingPrice(null).targetAllocation(10.0)
                                .rationale("Liquidez diária, proteção FGC").build()
                ))
                .characteristics(Arrays.asList("Pagamento mensal de dividendos", "Fluxo de caixa previsível"))
                .build());

        // 3. Carteira Internacional
        portfolios.add(InvestmentPortfolio.builder()
                .name("Carteira Internacional")
                .description("Diversificação geográfica através de ETFs.")
                .strategy("Diversificação geográfica, exposição cambial")
                .riskLevel("Moderado a Alto")
                .icon("🌎")
                .suggestedComposition(Arrays.asList(
                        PortfolioAsset.builder().type("ETFs S&P 500").percentage(50).description("VOO, SPY, IVV").build(),
                        PortfolioAsset.builder().type("ETFs Mundo").percentage(30).description("VT, ACWI").build(),
                        PortfolioAsset.builder().type("REITs").percentage(20).description("FIIs US").build()
                ))
                .recommendedAssets(Arrays.asList(
                        RecommendedAsset.builder().rank(1).ticker("IVVB11").name("iShares S&P 500").type("ETF")
                                .expectedDY(1.3).entryPrice(280.00).ceilingPrice(320.00).targetAllocation(25.0)
                                .rationale("Replica S&P 500, negociado na B3").build(),
                        RecommendedAsset.builder().rank(2).ticker("WRLD11").name("iShares MSCI World").type("ETF")
                                .expectedDY(1.0).entryPrice(45.00).ceilingPrice(55.00).targetAllocation(20.0)
                                .rationale("Diversificação global").build(),
                        RecommendedAsset.builder().rank(3).ticker("GOLD11").name("ETF Ouro").type("ETF")
                                .expectedDY(null).entryPrice(10.00).ceilingPrice(12.00).targetAllocation(10.0)
                                .rationale("Hedge contra inflação e crise").build()
                ))
                .characteristics(Arrays.asList("Proteção cambial (USD)", "Diversificação geográfica"))
                .build());

        // 4. Carteira Small Caps
        portfolios.add(InvestmentPortfolio.builder()
                .name("Carteira Small Caps")
                .description("Foco em empresas de menor capitalização com alto potencial.")
                .strategy("Crescimento agressivo, alta volatilidade")
                .riskLevel("Alto")
                .icon("🚀")
                .suggestedComposition(Arrays.asList(
                        PortfolioAsset.builder().type("Small Caps").percentage(70).description("Empresas em crescimento").build(),
                        PortfolioAsset.builder().type("Mid Caps").percentage(20).description("Empresas em consolidação").build(),
                        PortfolioAsset.builder().type("Renda Fixa").percentage(10).description("Proteção mínima").build()
                ))
                .recommendedAssets(Arrays.asList(
                        RecommendedAsset.builder().rank(1).ticker("LWSA3").name("Locaweb").type("Ação")
                                .expectedDY(0.5).entryPrice(4.50).ceilingPrice(6.50).targetAllocation(12.0)
                                .rationale("Líder em hospedagem e e-commerce para PMEs").build(),
                        RecommendedAsset.builder().rank(2).ticker("RDOR3").name("Rede D'Or").type("Ação")
                                .expectedDY(0.8).entryPrice(26.00).ceilingPrice(32.00).targetAllocation(12.0)
                                .rationale("Maior rede de hospitais privados").build(),
                        RecommendedAsset.builder().rank(3).ticker("SIMH3").name("Simpar").type("Ação")
                                .expectedDY(2.5).entryPrice(6.00).ceilingPrice(8.50).targetAllocation(10.0)
                                .rationale("Holding de mobilidade (Movida, JSL)").build(),
                        RecommendedAsset.builder().rank(4).ticker("RENT3").name("Localiza").type("Ação")
                                .expectedDY(2.5).entryPrice(42.00).ceilingPrice(55.00).targetAllocation(10.0)
                                .rationale("Líder em aluguel de veículos").build()
                ))
                .characteristics(Arrays.asList("Alto potencial de retorno", "Alta volatilidade"))
                .build());

        // 5. Carteira de Renda Fixa
        portfolios.add(InvestmentPortfolio.builder()
                .name("Carteira de Renda Fixa")
                .description("Foco em segurança e previsibilidade de retornos.")
                .strategy("Preservação de capital, liquidez e rentabilidade real")
                .riskLevel("Baixo")
                .icon("🏦")
                .suggestedComposition(Arrays.asList(
                        PortfolioAsset.builder().type("Tesouro Direto").percentage(50).description("Títulos Públicos").build(),
                        PortfolioAsset.builder().type("CDBs/LCIs/LCAs").percentage(30).description("Títulos Bancários").build(),
                        PortfolioAsset.builder().type("Debêntures").percentage(20).description("Crédito Privado").build()
                ))
                .recommendedAssets(Arrays.asList(
                        RecommendedAsset.builder().rank(1).ticker("TESOURO SELIC 2029").name("Tesouro Selic 2029").type("Renda Fixa")
                                .expectedDY(null).entryPrice(null).ceilingPrice(null).targetAllocation(20.0)
                                .rationale("Liquidez diária, segurança máxima, rentabilidade atrelada à SELIC. Ideal para reserva de emergência.").build(),
                        RecommendedAsset.builder().rank(2).ticker("IPCA+ 2035").name("Tesouro IPCA+ 2035").type("Renda Fixa")
                                .expectedDY(null).entryPrice(null).ceilingPrice(null).targetAllocation(20.0)
                                .rationale("Proteção contra inflação, rentabilidade real garantida. Ideal para aposentadoria e metas de longo prazo.").build(),
                        RecommendedAsset.builder().rank(3).ticker("PREFIXADO 2027").name("Tesouro Prefixado 2027").type("Renda Fixa")
                                .expectedDY(null).entryPrice(null).ceilingPrice(null).targetAllocation(10.0)
                                .rationale("Taxa fixa conhecida no momento da compra. Bom para cenários de queda de juros.").build(),
                        RecommendedAsset.builder().rank(4).ticker("CDB 120% CDI").name("CDB 120% CDI").type("Renda Fixa")
                                .expectedDY(null).entryPrice(null).ceilingPrice(null).targetAllocation(15.0)
                                .rationale("Rentabilidade superior ao CDI, proteção do FGC até R$250mil. Bancos médios oferecem melhores taxas.").build(),
                        RecommendedAsset.builder().rank(5).ticker("LCI 95% CDI").name("LCI 95% CDI").type("Renda Fixa")
                                .expectedDY(null).entryPrice(null).ceilingPrice(null).targetAllocation(15.0)
                                .rationale("Isento de IR para pessoa física. Equivale a ~111% CDI em CDB. Proteção FGC.").build(),
                        RecommendedAsset.builder().rank(6).ticker("LCA 94% CDI").name("LCA 94% CDI").type("Renda Fixa")
                                .expectedDY(null).entryPrice(null).ceilingPrice(null).targetAllocation(10.0)
                                .rationale("Isento de IR, lastro em crédito do agronegócio. Boa diversificação de emissor.").build(),
                        RecommendedAsset.builder().rank(7).ticker("DEBENTURE INFRA").name("Debênture Incentivada").type("Renda Fixa")
                                .expectedDY(null).entryPrice(null).ceilingPrice(null).targetAllocation(10.0)
                                .rationale("Isenta de IR, financia projetos de infraestrutura. Maior risco de crédito, maior retorno.").build()
                ))
                .characteristics(Arrays.asList("Baixo risco", "Previsibilidade", "Proteção contra inflação", "Liquidez variada"))
                .build());

        // 6. Carteira Cripto
        portfolios.add(InvestmentPortfolio.builder()
                .name("Carteira de Criptomoedas")
                .description("Exposição a ativos digitais.")
                .strategy("Buy and Hold, DCA")
                .riskLevel("Muito Alto")
                .icon("₿")
                .suggestedComposition(Arrays.asList(
                        PortfolioAsset.builder().type("Bitcoin").percentage(50).description("BTC").build(),
                        PortfolioAsset.builder().type("Ethereum").percentage(30).description("ETH").build(),
                        PortfolioAsset.builder().type("Altcoins").percentage(20).description("Outros").build()
                ))
                .recommendedAssets(Arrays.asList(
                        RecommendedAsset.builder().rank(1).ticker("BTC").name("Bitcoin").type("Cripto")
                                .expectedDY(null).entryPrice(350000.00).ceilingPrice(500000.00).targetAllocation(50.0)
                                .rationale("Reserva de valor digital, oferta limitada").build(),
                        RecommendedAsset.builder().rank(2).ticker("ETH").name("Ethereum").type("Cripto")
                                .expectedDY(null).entryPrice(15000.00).ceilingPrice(22000.00).targetAllocation(30.0)
                                .rationale("Plataforma de smart contracts líder").build(),
                        RecommendedAsset.builder().rank(3).ticker("SOL").name("Solana").type("Cripto")
                                .expectedDY(null).entryPrice(700.00).ceilingPrice(1200.00).targetAllocation(10.0)
                                .rationale("Blockchain de alta performance").build()
                ))
                .characteristics(Arrays.asList("Altíssima volatilidade", "Potencial de retornos exponenciais"))
                .build());

        return portfolios;
    }
}
