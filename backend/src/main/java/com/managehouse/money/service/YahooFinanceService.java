package com.managehouse.money.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.managehouse.money.dto.YahooFinanceDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service para buscar dados fundamentalistas do Yahoo Finance
 * Implementa cache de 1 hora por ticker para evitar rate limiting
 * Obtem crumb e cookies para autenticacao automatica
 */
@Slf4j
@Service
public class YahooFinanceService {

    private static final Duration CACHE_DURATION = Duration.ofHours(1);
    private static final Duration CRUMB_DURATION = Duration.ofMinutes(30);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final Map<String, CachedData> cache = new ConcurrentHashMap<>();

    // Crumb e cookies para autenticacao
    private String crumb;
    private String cookies;
    private Instant crumbObtainedAt;

    public YahooFinanceService() {
        this.webClient = WebClient.builder()
                .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .defaultHeader("Accept", "*/*")
                .defaultHeader("Accept-Language", "en-US,en;q=0.9")
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Obtem crumb e cookies do Yahoo Finance para autenticacao
     */
    private synchronized void refreshCrumbIfNeeded() {
        // Verificar se crumb ainda e valido
        if (crumb != null && crumbObtainedAt != null &&
                Instant.now().isBefore(crumbObtainedAt.plus(CRUMB_DURATION))) {
            return;
        }

        log.info("Obtendo novo crumb do Yahoo Finance...");

        try {
            // Passo 1: Fazer requisicao inicial para obter cookies
            ClientResponse initialResponse = webClient.get()
                    .uri("https://fc.yahoo.com")
                    .exchangeToMono(response -> reactor.core.publisher.Mono.just(response))
                    .block(Duration.ofSeconds(10));

            if (initialResponse == null) {
                log.error("Falha ao obter resposta inicial do Yahoo");
                return;
            }

            // Extrair cookies da resposta
            MultiValueMap<String, ResponseCookie> responseCookies = initialResponse.cookies();
            StringBuilder cookieBuilder = new StringBuilder();
            responseCookies.forEach((name, cookieList) -> {
                for (ResponseCookie cookie : cookieList) {
                    if (cookieBuilder.length() > 0) cookieBuilder.append("; ");
                    cookieBuilder.append(cookie.getName()).append("=").append(cookie.getValue());
                }
            });

            // Passo 2: Obter o crumb usando os cookies
            String crumbResponse = webClient.get()
                    .uri("https://query1.finance.yahoo.com/v1/test/getcrumb")
                    .header(HttpHeaders.COOKIE, cookieBuilder.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(10));

            if (crumbResponse != null && !crumbResponse.isEmpty() && !crumbResponse.contains("error")) {
                this.crumb = crumbResponse.trim();
                this.cookies = cookieBuilder.toString();
                this.crumbObtainedAt = Instant.now();
                log.info("Crumb obtido com sucesso: {}", crumb.substring(0, Math.min(5, crumb.length())) + "...");
            } else {
                log.warn("Crumb vazio ou invalido recebido: {}", crumbResponse);
            }

        } catch (Exception e) {
            log.error("Erro ao obter crumb do Yahoo Finance: {}", e.getMessage());
        }
    }

    /**
     * Busca dados fundamentalistas de uma acao brasileira
     * @param ticker Codigo do ativo (ex: BBAS3, VALE3)
     * @return DTO com dados fundamentalistas ou dados vazios se falhar
     */
    public YahooFinanceDTO getStockFundamentals(String ticker) {
        // Normalizar ticker (remover .SA se já tiver)
        String normalizedTicker = ticker.replace(".SA", "").toUpperCase();
        String yahooTicker = normalizedTicker + ".SA";

        // Verificar cache
        CachedData cached = cache.get(normalizedTicker);
        if (cached != null && !cached.isExpired()) {
            log.debug("Cache hit para {}", normalizedTicker);
            return cached.getData();
        }

        log.info("Buscando dados do Yahoo Finance para {}", yahooTicker);

        // Tentar multiplas abordagens em sequencia
        YahooFinanceDTO dto = null;

        // Tentativa 1: API v7 com crumb (metodo oficial)
        dto = tryWithCrumb(yahooTicker, normalizedTicker);
        if (dto != null && dto.isDataAvailable()) {
            cache.put(normalizedTicker, new CachedData(dto, Instant.now()));
            return dto;
        }

        // Tentativa 2: API v8 chart (alternativa que as vezes funciona)
        dto = tryChartAPI(yahooTicker, normalizedTicker);
        if (dto != null && dto.isDataAvailable()) {
            cache.put(normalizedTicker, new CachedData(dto, Instant.now()));
            return dto;
        }

        // Tentativa 3: API simples sem crumb (query2)
        dto = trySimpleAPI(yahooTicker, normalizedTicker);
        if (dto != null && dto.isDataAvailable()) {
            cache.put(normalizedTicker, new CachedData(dto, Instant.now()));
            return dto;
        }

        log.warn("Todas as tentativas falharam para {}", yahooTicker);
        return createEmptyDTO(normalizedTicker, "Dados indisponiveis apos multiplas tentativas");
    }

    /**
     * Tentativa 1: Usar API v7 com crumb
     */
    private YahooFinanceDTO tryWithCrumb(String yahooTicker, String normalizedTicker) {
        try {
            refreshCrumbIfNeeded();

            if (crumb == null || cookies == null) {
                log.debug("Crumb nao disponivel, pulando tentativa com crumb");
                return null;
            }

            String url = String.format(
                    "https://query1.finance.yahoo.com/v7/finance/quote?symbols=%s&crumb=%s",
                    yahooTicker, crumb
            );

            String response = webClient.get()
                    .uri(url)
                    .header(HttpHeaders.COOKIE, cookies)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(10));

            log.debug("Resposta v7 com crumb para {}: sucesso", yahooTicker);
            return parseQuoteResponse(response, normalizedTicker);

        } catch (Exception e) {
            log.debug("Tentativa com crumb falhou para {}: {}", yahooTicker, e.getMessage());
            this.crumb = null;
            this.crumbObtainedAt = null;
            return null;
        }
    }

    /**
     * Tentativa 2: Usar API v8 chart (retorna dados basicos)
     */
    private YahooFinanceDTO tryChartAPI(String yahooTicker, String normalizedTicker) {
        try {
            String url = String.format(
                    "https://query1.finance.yahoo.com/v8/finance/chart/%s?interval=1d&range=1d",
                    yahooTicker
            );

            String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(10));

            log.debug("Resposta chart API para {}: sucesso", yahooTicker);
            return parseChartResponse(response, normalizedTicker);

        } catch (Exception e) {
            log.debug("Tentativa chart API falhou para {}: {}", yahooTicker, e.getMessage());
            return null;
        }
    }

    /**
     * Tentativa 3: API simples sem autenticacao (query2)
     */
    private YahooFinanceDTO trySimpleAPI(String yahooTicker, String normalizedTicker) {
        try {
            String url = String.format(
                    "https://query2.finance.yahoo.com/v7/finance/quote?symbols=%s",
                    yahooTicker
            );

            String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(10));

            log.debug("Resposta query2 para {}: sucesso", yahooTicker);
            return parseQuoteResponse(response, normalizedTicker);

        } catch (Exception e) {
            log.debug("Tentativa query2 falhou para {}: {}", yahooTicker, e.getMessage());
            return null;
        }
    }

    /**
     * Parseia resposta da API chart (v8)
     */
    private YahooFinanceDTO parseChartResponse(String jsonResponse, String ticker) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode chart = root.path("chart").path("result").get(0);

            if (chart == null || chart.isMissingNode()) {
                return null;
            }

            JsonNode meta = chart.path("meta");
            Double regularMarketPrice = extractDoubleSimple(meta, "regularMarketPrice");
            Double fiftyTwoWeekHigh = extractDoubleSimple(meta, "fiftyTwoWeekHigh");
            Double fiftyTwoWeekLow = extractDoubleSimple(meta, "fiftyTwoWeekLow");

            // Chart API nao retorna P/L, P/VP, DY - mas retorna preco
            return YahooFinanceDTO.builder()
                    .ticker(ticker)
                    .regularMarketPrice(regularMarketPrice)
                    .fiftyTwoWeekHigh(fiftyTwoWeekHigh)
                    .fiftyTwoWeekLow(fiftyTwoWeekLow)
                    .dataAvailable(regularMarketPrice != null)
                    .build();

        } catch (Exception e) {
            log.debug("Erro ao parsear chart response: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Parseia a resposta JSON da API v6 (quote)
     */
    private YahooFinanceDTO parseQuoteResponse(String jsonResponse, String ticker) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode result = root.path("quoteResponse").path("result").get(0);

            if (result == null || result.isMissingNode()) {
                return createEmptyDTO(ticker, "Dados não encontrados");
            }

            // Extrair dados da quote
            Double trailingPE = extractDoubleSimple(result, "trailingPE");
            Double forwardPE = extractDoubleSimple(result, "forwardPE");
            Double priceToBook = extractDoubleSimple(result, "priceToBook");
            Double dividendYield = extractDoubleSimple(result, "trailingAnnualDividendYield");
            Double trailingEps = extractDoubleSimple(result, "epsTrailingTwelveMonths");
            Double bookValue = extractDoubleSimple(result, "bookValue");
            Double regularMarketPrice = extractDoubleSimple(result, "regularMarketPrice");
            Double fiftyTwoWeekHigh = extractDoubleSimple(result, "fiftyTwoWeekHigh");
            Double fiftyTwoWeekLow = extractDoubleSimple(result, "fiftyTwoWeekLow");
            Long marketCap = extractLongSimple(result, "marketCap");
            String sector = extractStringSimple(result, "sector");
            String industry = extractStringSimple(result, "industry");
            String shortName = extractStringSimple(result, "shortName");

            return YahooFinanceDTO.builder()
                    .ticker(ticker)
                    .trailingPE(trailingPE)
                    .forwardPE(forwardPE)
                    .priceToBook(priceToBook)
                    .dividendYield(dividendYield)
                    .trailingEps(trailingEps)
                    .bookValue(bookValue)
                    .regularMarketPrice(regularMarketPrice)
                    .fiftyTwoWeekHigh(fiftyTwoWeekHigh)
                    .fiftyTwoWeekLow(fiftyTwoWeekLow)
                    .marketCap(marketCap)
                    .sector(sector)
                    .industry(industry)
                    .shortName(shortName)
                    .dataAvailable(trailingPE != null || priceToBook != null || dividendYield != null)
                    .build();

        } catch (Exception e) {
            log.error("Erro ao parsear resposta do Yahoo Finance: {}", e.getMessage());
            return createEmptyDTO(ticker, "Erro ao parsear dados: " + e.getMessage());
        }
    }

    /**
     * Extrai Double simples (campos da v6 nao tem formato raw/fmt)
     */
    private Double extractDoubleSimple(JsonNode node, String field) {
        JsonNode valueNode = node.path(field);
        if (valueNode.isMissingNode() || valueNode.isNull()) {
            return null;
        }
        if (valueNode.isNumber()) {
            return valueNode.asDouble();
        }
        return null;
    }

    /**
     * Extrai Long simples
     */
    private Long extractLongSimple(JsonNode node, String field) {
        JsonNode valueNode = node.path(field);
        if (valueNode.isMissingNode() || valueNode.isNull()) {
            return null;
        }
        if (valueNode.isNumber()) {
            return valueNode.asLong();
        }
        return null;
    }

    /**
     * Extrai String simples
     */
    private String extractStringSimple(JsonNode node, String field) {
        JsonNode valueNode = node.path(field);
        if (valueNode.isMissingNode() || valueNode.isNull()) {
            return null;
        }
        return valueNode.asText();
    }

    /**
     * Cria um DTO vazio para quando a API falha
     */
    private YahooFinanceDTO createEmptyDTO(String ticker, String errorMessage) {
        return YahooFinanceDTO.builder()
                .ticker(ticker)
                .dataAvailable(false)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * Limpa o cache (útil para testes ou forçar atualização)
     */
    public void clearCache() {
        cache.clear();
        log.info("Cache do Yahoo Finance limpo");
    }

    /**
     * Remove um ticker específico do cache
     */
    public void invalidateCache(String ticker) {
        String normalizedTicker = ticker.replace(".SA", "").toUpperCase();
        cache.remove(normalizedTicker);
        log.info("Cache invalidado para {}", normalizedTicker);
    }

    /**
     * Retorna estatísticas do cache
     */
    public Map<String, Object> getCacheStats() {
        long validEntries = cache.values().stream()
                .filter(c -> !c.isExpired())
                .count();

        return Map.of(
                "totalEntries", cache.size(),
                "validEntries", validEntries,
                "expiredEntries", cache.size() - validEntries
        );
    }

    /**
     * Classe interna para armazenar dados em cache com timestamp
     */
    private static class CachedData {
        private final YahooFinanceDTO data;
        private final Instant cachedAt;

        public CachedData(YahooFinanceDTO data, Instant cachedAt) {
            this.data = data;
            this.cachedAt = cachedAt;
        }

        public YahooFinanceDTO getData() {
            return data;
        }

        public boolean isExpired() {
            return Instant.now().isAfter(cachedAt.plus(CACHE_DURATION));
        }
    }
}
