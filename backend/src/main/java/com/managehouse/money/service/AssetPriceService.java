package com.managehouse.money.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.managehouse.money.dto.RecommendedAsset;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AssetPriceService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Cache de preços (ticker -> preço) com TTL de 5 minutos
    private final Map<String, CachedPrice> priceCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutos

    // Mapeamento de símbolos de cripto para IDs do CoinGecko
    private static final Map<String, String> CRYPTO_ID_MAP = new HashMap<>() {{
        put("BTC", "bitcoin");
        put("ETH", "ethereum");
        put("SOL", "solana");
        put("LINK", "chainlink");
        put("MATIC", "matic-network");
        put("USDC", "usd-coin");
        put("USDT", "tether");
        put("BNB", "binancecoin");
    }};


    private static class CachedPrice {
        Double price;
        long timestamp;

        CachedPrice(Double price) {
            this.price = price;
            this.timestamp = Instant.now().toEpochMilli();
        }

        boolean isExpired() {
            return Instant.now().toEpochMilli() - timestamp > CACHE_TTL_MS;
        }
    }

    /**
     * Buscar preços de múltiplas ações/FIIs brasileiras via Yahoo Finance (gratuito)
     */
    public Map<String, Double> getBrazilianAssetPrices(List<String> tickers) {
        Map<String, Double> prices = new HashMap<>();

        if (tickers == null || tickers.isEmpty()) {
            return prices;
        }

        // Verificar cache primeiro
        List<String> tickersToFetch = tickers.stream()
                .filter(t -> {
                    CachedPrice cached = priceCache.get(t);
                    if (cached != null && !cached.isExpired()) {
                        prices.put(t, cached.price);
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        if (tickersToFetch.isEmpty()) {
            log.debug("Todos os preços obtidos do cache");
            return prices;
        }

        // Buscar cada ticker individualmente via Yahoo Finance
        for (String ticker : tickersToFetch) {
            Double price = getBrazilianAssetPrice(ticker);
            if (price != null) {
                prices.put(ticker, price);
            }
        }

        return prices;
    }

    /**
     * Buscar preço de ação ou FII brasileira via Google Finance (scraping)
     */
    public Double getBrazilianAssetPrice(String ticker) {
        // Verificar cache primeiro
        CachedPrice cached = priceCache.get(ticker);
        if (cached != null && !cached.isExpired()) {
            return cached.price;
        }

        // Tentar Google Finance primeiro
        Double price = fetchFromGoogleFinance(ticker);

        // Fallback para Yahoo Finance
        if (price == null) {
            price = fetchFromYahoo(ticker);
        }

        if (price != null) {
            priceCache.put(ticker, new CachedPrice(price));
        }
        return price;
    }

    private Double fetchFromGoogleFinance(String ticker) {
        try {
            String cleanTicker = ticker.replace(".SA", "");
            String url = "https://www.google.com/finance/quote/" + cleanTicker + ":BVMF";

            log.debug("Buscando preço de {} via Google Finance", cleanTicker);

            // Usar HttpURLConnection para definir User-Agent
            java.net.URL urlObj = new java.net.URL(url);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) urlObj.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Extrair preço usando regex: data-last-price="74.74"
            String html = response.toString();
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("data-last-price=\"([0-9.]+)\"");
            java.util.regex.Matcher matcher = pattern.matcher(html);

            if (matcher.find()) {
                double price = Double.parseDouble(matcher.group(1));
                log.info("Preço de {} via Google Finance: R$ {}", cleanTicker, price);
                return price;
            }

            log.warn("Preço não encontrado para {} via Google Finance", ticker);
        } catch (Exception e) {
            log.debug("Erro ao buscar preço de {} via Google Finance: {}", ticker, e.getMessage());
        }
        return null;
    }

    private Double fetchFromYahoo(String ticker) {
        try {
            String yahooTicker = ticker.contains(".SA") ? ticker : ticker + ".SA";
            String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + yahooTicker + "?interval=1d&range=1d";

            log.debug("Buscando preço de {} via Yahoo Finance", yahooTicker);
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            JsonNode chart = root.path("chart").path("result");
            if (chart.isArray() && chart.size() > 0) {
                JsonNode meta = chart.get(0).path("meta");
                if (meta.has("regularMarketPrice")) {
                    double price = meta.get("regularMarketPrice").asDouble();
                    log.info("Preço de {} via Yahoo: R$ {}", ticker, price);
                    return price;
                }
            }
            log.warn("Preço não encontrado para {} via Yahoo Finance", ticker);
        } catch (Exception e) {
            log.debug("Erro ao buscar preço de {} via Yahoo: {}", ticker, e.getMessage());
        }
        return null;
    }

    /**
     * Buscar preço de criptomoeda via CoinGecko (API gratuita)
     */
    public Double getCryptoPrice(String symbol) {
        try {
            String coinId = CRYPTO_ID_MAP.get(symbol.toUpperCase());
            if (coinId == null) {
                log.warn("Símbolo de cripto não mapeado: {}", symbol);
                return null;
            }

            String url = "https://api.coingecko.com/api/v3/simple/price?ids=" + coinId + "&vs_currencies=brl";

            log.debug("Buscando preço de {} via CoinGecko", symbol);
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            JsonNode coinData = root.get(coinId);
            if (coinData != null && coinData.has("brl")) {
                double price = coinData.get("brl").asDouble();
                log.debug("Preço de {}: R$ {}", symbol, price);
                return price;
            }

            log.warn("Preço não encontrado para {}", symbol);
        } catch (Exception e) {
            log.warn("Erro ao buscar preço de {}: {}", symbol, e.getMessage());
        }
        return null;
    }

    /**
     * Enriquecer lista de ativos com preços em tempo real e calcular viés
     */
    public List<RecommendedAsset> enrichWithPrices(List<RecommendedAsset> assets) {
        if (assets == null || assets.isEmpty()) {
            return assets;
        }

        for (RecommendedAsset asset : assets) {
            try {
                // Buscar preço baseado no tipo de ativo
                String type = asset.getType();
                if (type != null) {
                    switch (type) {
                        case "Ação":
                        case "FII":
                            Double stockPrice = getBrazilianAssetPrice(asset.getTicker());
                            asset.setCurrentPrice(stockPrice);
                            break;
                        case "Cripto":
                        case "Stablecoin":
                            Double cryptoPrice = getCryptoPrice(asset.getTicker());
                            asset.setCurrentPrice(cryptoPrice);
                            break;
                        case "ETF":
                            // ETFs brasileiros também via Brapi
                            Double etfPrice = getBrazilianAssetPrice(asset.getTicker());
                            asset.setCurrentPrice(etfPrice);
                            break;
                        case "ETF Internacional":
                            // ETFs internacionais - por enquanto não temos API
                            asset.setCurrentPrice(null);
                            break;
                        case "Renda Fixa":
                            // Renda fixa não tem preço de mercado tradicional
                            asset.setCurrentPrice(null);
                            break;
                        default:
                            log.warn("Tipo de ativo não reconhecido: {}", type);
                            asset.setCurrentPrice(null);
                    }
                }

                // Calcular viés automaticamente
                calculateBias(asset);

            } catch (Exception e) {
                log.error("Erro ao processar ativo {}: {}", asset.getTicker(), e.getMessage());
                asset.setCurrentPrice(null);
                asset.setBias("-");
            }
        }

        return assets;
    }

    /**
     * Calcular viés de compra baseado em preço atual vs preço-teto
     * Regra: se preço atual < preço-teto => "Comprar", senão => "Aguardar"
     */
    private void calculateBias(RecommendedAsset asset) {
        if (asset.getCurrentPrice() != null && asset.getCeilingPrice() != null) {
            if (asset.getCurrentPrice() < asset.getCeilingPrice()) {
                asset.setBias("Comprar");
            } else {
                asset.setBias("Aguardar");
            }
        } else if (asset.getCeilingPrice() == null) {
            // Sem preço-teto definido (ex: renda fixa)
            asset.setBias("-");
        } else {
            // Preço atual não disponível
            asset.setBias("-");
        }
    }
}
