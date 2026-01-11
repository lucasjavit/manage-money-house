package com.managehouse.money.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.managehouse.money.dto.EconomicContextResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
@RequiredArgsConstructor
public class EconomicDataService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String BCB_SGS_BASE_URL = "https://api.bcb.gov.br/dados/serie/bcdata.sgs.{codigo}/dados/ultimos/{quantidade}?formato=json";
    private static final String AWESOME_API_URL = "https://economia.awesomeapi.com.br/json/last/USD-BRL";

    // Códigos de séries do BCB
    private static final String SELIC_CODE = "432";
    private static final String IPCA_CODE = "433";
    private static final String IGPM_CODE = "189";

    /**
     * Busca contexto econômico completo (IPCA, IGP-M, SELIC, USD/BRL)
     * SEM cache - sempre busca dados atualizados
     */
    public EconomicContextResponse fetchEconomicContext() {
        log.info("Buscando contexto econômico das APIs externas...");

        try {
            EconomicContextResponse response = new EconomicContextResponse();

            // Buscar dados em paralelo (cada método já tem try-catch interno)
            response.setIpca(fetchIPCA());
            response.setIgpm(fetchIGPM());
            response.setSelic(fetchSELIC());
            response.setUsdBrl(fetchUSD());

            log.info("Contexto econômico obtido com sucesso");
            return response;

        } catch (Exception e) {
            log.error("Erro ao buscar contexto econômico completo", e);
            // Retornar resposta vazia ao invés de lançar exceção
            return new EconomicContextResponse();
        }
    }

    /**
     * Busca IPCA mensal do Banco Central (série 433)
     */
    private EconomicContextResponse.IndicatorData fetchIPCA() {
        try {
            log.debug("Buscando IPCA do BCB...");
            String url = BCB_SGS_BASE_URL.replace("{codigo}", IPCA_CODE).replace("{quantidade}", "2");
            String response = restTemplate.getForObject(url, String.class);

            JsonNode data = objectMapper.readTree(response);
            if (data != null && data.isArray() && data.size() >= 1) {
                JsonNode current = data.get(data.size() - 1); // Último valor
                JsonNode previous = data.size() >= 2 ? data.get(data.size() - 2) : null;

                double currentValue = current.get("valor").asDouble();
                double previousValue = previous != null ? previous.get("valor").asDouble() : 0;
                String period = current.get("data").asText(); // formato: "01/01/2024"

                EconomicContextResponse.IndicatorData ipca = new EconomicContextResponse.IndicatorData();
                ipca.setValue(currentValue);
                ipca.setPeriod(formatPeriod(period));
                ipca.setVsLastMonth(currentValue - previousValue);

                log.debug("IPCA obtido: {}% (período: {})", currentValue, period);
                return ipca;
            }

            return null;

        } catch (Exception e) {
            log.error("Erro ao buscar IPCA do BCB", e);
            return null;
        }
    }

    /**
     * Busca IGP-M mensal do Banco Central (série 189)
     */
    private EconomicContextResponse.IndicatorData fetchIGPM() {
        try {
            log.debug("Buscando IGP-M do BCB...");
            String url = BCB_SGS_BASE_URL.replace("{codigo}", IGPM_CODE).replace("{quantidade}", "2");
            String response = restTemplate.getForObject(url, String.class);

            JsonNode data = objectMapper.readTree(response);
            if (data != null && data.isArray() && data.size() >= 1) {
                JsonNode current = data.get(data.size() - 1);
                JsonNode previous = data.size() >= 2 ? data.get(data.size() - 2) : null;

                double currentValue = current.get("valor").asDouble();
                double previousValue = previous != null ? previous.get("valor").asDouble() : 0;
                String period = current.get("data").asText();

                EconomicContextResponse.IndicatorData igpm = new EconomicContextResponse.IndicatorData();
                igpm.setValue(currentValue);
                igpm.setPeriod(formatPeriod(period));
                igpm.setVsLastMonth(currentValue - previousValue);

                log.debug("IGP-M obtido: {}% (período: {})", currentValue, period);
                return igpm;
            }

            return null;

        } catch (Exception e) {
            log.error("Erro ao buscar IGP-M do BCB", e);
            return null;
        }
    }

    /**
     * Busca Taxa SELIC meta do Banco Central (série 432)
     */
    private EconomicContextResponse.SelicData fetchSELIC() {
        try {
            log.debug("Buscando SELIC do BCB...");
            String url = BCB_SGS_BASE_URL.replace("{codigo}", SELIC_CODE).replace("{quantidade}", "1");
            String response = restTemplate.getForObject(url, String.class);

            JsonNode data = objectMapper.readTree(response);
            if (data != null && data.isArray() && data.size() >= 1) {
                JsonNode current = data.get(0);

                double value = current.get("valor").asDouble();
                String lastUpdate = current.get("data").asText();

                EconomicContextResponse.SelicData selic = new EconomicContextResponse.SelicData();
                selic.setValue(value);
                selic.setLastUpdate(formatPeriod(lastUpdate));

                log.debug("SELIC obtida: {}% (última atualização: {})", value, lastUpdate);
                return selic;
            }

            return null;

        } catch (Exception e) {
            log.error("Erro ao buscar SELIC do BCB", e);
            return null;
        }
    }

    /**
     * Busca cotação USD/BRL do AwesomeAPI
     */
    private EconomicContextResponse.ExchangeData fetchUSD() {
        try {
            log.debug("Buscando USD/BRL do AwesomeAPI...");
            String response = restTemplate.getForObject(AWESOME_API_URL, String.class);

            JsonNode data = objectMapper.readTree(response);
            if (data != null && data.has("USDBRL")) {
                JsonNode usd = data.get("USDBRL");

                double bid = usd.get("bid").asDouble();
                double pctChange = usd.get("pctChange").asDouble();

                EconomicContextResponse.ExchangeData exchangeData = new EconomicContextResponse.ExchangeData();
                exchangeData.setValue(bid);
                exchangeData.setVariation(pctChange);

                log.debug("USD/BRL obtido: R$ {} (variação: {}%)", bid, pctChange);
                return exchangeData;
            }

            return null;

        } catch (Exception e) {
            log.error("Erro ao buscar USD/BRL do AwesomeAPI", e);
            return null;
        }
    }

    /**
     * Formata período de dd/MM/yyyy para yyyy-MM
     */
    private String formatPeriod(String dateStr) {
        try {
            // Formato BCB: "01/01/2024" -> "2024-01"
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
            LocalDate date = LocalDate.parse(dateStr, inputFormatter);
            return date.format(outputFormatter);
        } catch (Exception e) {
            log.warn("Erro ao formatar período: {}", dateStr);
            return dateStr;
        }
    }
}
