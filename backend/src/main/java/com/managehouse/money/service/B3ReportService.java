package com.managehouse.money.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.managehouse.money.config.ChatModelFactory;
import com.managehouse.money.dto.B3ReportUploadResponse;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class B3ReportService {

    private final ChatModelFactory chatModelFactory;
    private final ConfigurationService configurationService;
    private ObjectMapper objectMapper = new ObjectMapper();

    public B3ReportUploadResponse processReport(String base64Content, Long userId) {
        log.info("Processando relatorio B3 para usuario {}", userId);

        try {
            // 1. Decodificar PDF
            byte[] pdfBytes = Base64.getDecoder().decode(base64Content);
            log.info("PDF decodificado: {} bytes", pdfBytes.length);

            // 2. Extrair texto do PDF
            String extractedText = extractTextFromPDF(pdfBytes);
            log.info("Texto extraido do PDF: {} caracteres", extractedText.length());
            log.debug("Texto extraido: {}", extractedText.substring(0, Math.min(2000, extractedText.length())));

            // 3. Usar IA para parsear os dados
            String apiKey = configurationService.getOpenAIKey();
            if (apiKey == null || apiKey.isEmpty()) {
                log.error("Chave OpenAI nao configurada");
                return B3ReportUploadResponse.builder()
                        .errorMessage("Chave OpenAI nao configurada. Configure em Configuracoes.")
                        .build();
            }

            // Usar modelo com mais tokens para extracao de dados estruturados
            ChatLanguageModel chatModel = chatModelFactory.createExtractionModel(apiKey);
            String prompt = buildExtractionPrompt(extractedText);
            log.info("Enviando prompt para IA (modelo de extracao com 16000 tokens, timeout 120s)...");

            String aiResponse = chatModel.generate(prompt);
            log.info("Resposta da IA recebida: {} caracteres", aiResponse.length());
            log.debug("Resposta da IA: {}", aiResponse);

            // 4. Parsear resposta da IA
            return parseAIResponse(aiResponse);

        } catch (Exception e) {
            log.error("Erro ao processar relatorio B3: {}", e.getMessage(), e);
            return B3ReportUploadResponse.builder()
                    .errorMessage("Erro ao processar PDF: " + e.getMessage())
                    .build();
        }
    }

    private String extractTextFromPDF(byte[] pdfBytes) throws IOException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(pdfBytes);
             PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String buildExtractionPrompt(String text) {
        return """
            Voce e um especialista em extrair dados de relatorios consolidados da B3.

            IMPORTANTE: Extraia TODOS os dados do relatorio abaixo e retorne APENAS JSON valido (sem markdown, sem ```).

            O relatorio contem secoes como:
            - Posicao - Acoes (tickers como BBAS3, VALE3, PETR4, etc)
            - Posicao - FII - Fundo de Investimento Imobiliario (tickers terminados em 11)
            - Posicao - CDB - Certificado de Deposito Bancario
            - Posicao - LCA - Letra de Credito do Agronegocio
            - Posicao - DEB - Debentures
            - Posicao - Fundos de Investimentos (FIAgro, etc)
            - Proventos recebidos (Dividendos, JCP, Rendimentos)

            Extraia os dados e retorne neste formato JSON:
            {
              "reportMonth": 12,
              "reportYear": 2025,
              "stocks": [
                {
                  "ticker": "BBAS3",
                  "name": "Banco do Brasil",
                  "type": "ON",
                  "quantity": 887.0,
                  "closePrice": 21.92,
                  "totalValue": 19443.04,
                  "institution": "Banco BTG Pactual"
                }
              ],
              "fiis": [
                {
                  "ticker": "RECR11",
                  "name": "FII REC Recebiveis Imobiliarios",
                  "quantity": 97.0,
                  "closePrice": 81.99,
                  "totalValue": 7953.03,
                  "institution": "Banco BTG Pactual"
                }
              ],
              "fixedIncome": [
                {
                  "product": "CDB - Banco BMG",
                  "productType": "CDB",
                  "institution": "Banco BTG Pactual",
                  "maturityDate": "2026-01-15",
                  "quantity": 14.0,
                  "unitPrice": 1458.62,
                  "totalValue": 20420.74
                }
              ],
              "funds": [
                {
                  "ticker": "SNAG11",
                  "name": "Suno Agro FIAGRO",
                  "fundType": "FIAGRO",
                  "quantity": 425.0,
                  "closePrice": 11.14,
                  "totalValue": 4734.50,
                  "institution": "Banco BTG Pactual"
                }
              ],
              "dividends": [
                {
                  "ticker": "BBAS3",
                  "productName": "Banco do Brasil",
                  "paymentDate": "2025-12-12",
                  "eventType": "Juros Sobre Capital Proprio",
                  "quantity": 887.0,
                  "unitPrice": 0.05,
                  "netValue": 34.56
                }
              ],
              "totals": {
                "stocks": 55210.93,
                "fiis": 10246.95,
                "fixedIncome": 246941.29,
                "funds": 4734.50,
                "dividends": 1117.68,
                "grandTotal": 317133.67
              }
            }

            REGRAS:
            1. Extraia a data do relatorio (mes/ano) do cabecalho "Data: MM/YYYY"
            2. Para acoes, separe o ticker do nome (ex: "BBAS3 - BCO BRASIL S.A." -> ticker: "BBAS3", name: "Banco do Brasil")
            3. Para tipo de acao, use ON, PN, PNA, PNB conforme indicado
            4. Para FIIs, o ticker sempre termina em 11 (ex: RECR11, SNCI11)
            5. Para renda fixa, identifique o tipo (CDB, LCA, LCI, DEBENTURE) pelo nome do produto
            6. Converta datas para formato ISO (YYYY-MM-DD)
            7. Use numeros decimais (nao strings) para valores monetarios
            8. Some os totais de cada categoria
            9. Calcule grandTotal somando todos os ativos (acoes + fiis + fixedIncome + funds)

            TEXTO DO RELATORIO:
            %s
            """.formatted(text);
    }

    private B3ReportUploadResponse parseAIResponse(String aiResponse) {
        try {
            // Limpar resposta (remover markdown se houver)
            String cleanedResponse = aiResponse
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();

            JsonNode root = objectMapper.readTree(cleanedResponse);

            // Parsear mes/ano
            Integer reportMonth = root.has("reportMonth") ? root.get("reportMonth").asInt() : null;
            Integer reportYear = root.has("reportYear") ? root.get("reportYear").asInt() : null;

            // Parsear acoes
            List<B3ReportUploadResponse.StockPosition> stocks = new ArrayList<>();
            if (root.has("stocks") && root.get("stocks").isArray()) {
                for (JsonNode node : root.get("stocks")) {
                    stocks.add(B3ReportUploadResponse.StockPosition.builder()
                            .ticker(getTextOrNull(node, "ticker"))
                            .name(getTextOrNull(node, "name"))
                            .type(getTextOrNull(node, "type"))
                            .quantity(getBigDecimalOrNull(node, "quantity"))
                            .closePrice(getBigDecimalOrNull(node, "closePrice"))
                            .totalValue(getBigDecimalOrNull(node, "totalValue"))
                            .institution(getTextOrNull(node, "institution"))
                            .build());
                }
            }

            // Parsear FIIs
            List<B3ReportUploadResponse.FiiPosition> fiis = new ArrayList<>();
            if (root.has("fiis") && root.get("fiis").isArray()) {
                for (JsonNode node : root.get("fiis")) {
                    fiis.add(B3ReportUploadResponse.FiiPosition.builder()
                            .ticker(getTextOrNull(node, "ticker"))
                            .name(getTextOrNull(node, "name"))
                            .quantity(getBigDecimalOrNull(node, "quantity"))
                            .closePrice(getBigDecimalOrNull(node, "closePrice"))
                            .totalValue(getBigDecimalOrNull(node, "totalValue"))
                            .institution(getTextOrNull(node, "institution"))
                            .build());
                }
            }

            // Parsear Renda Fixa
            List<B3ReportUploadResponse.FixedIncomePosition> fixedIncome = new ArrayList<>();
            if (root.has("fixedIncome") && root.get("fixedIncome").isArray()) {
                for (JsonNode node : root.get("fixedIncome")) {
                    fixedIncome.add(B3ReportUploadResponse.FixedIncomePosition.builder()
                            .product(getTextOrNull(node, "product"))
                            .productType(getTextOrNull(node, "productType"))
                            .institution(getTextOrNull(node, "institution"))
                            .maturityDate(getTextOrNull(node, "maturityDate"))
                            .quantity(getBigDecimalOrNull(node, "quantity"))
                            .unitPrice(getBigDecimalOrNull(node, "unitPrice"))
                            .totalValue(getBigDecimalOrNull(node, "totalValue"))
                            .build());
                }
            }

            // Parsear Fundos
            List<B3ReportUploadResponse.FundPosition> funds = new ArrayList<>();
            if (root.has("funds") && root.get("funds").isArray()) {
                for (JsonNode node : root.get("funds")) {
                    funds.add(B3ReportUploadResponse.FundPosition.builder()
                            .ticker(getTextOrNull(node, "ticker"))
                            .name(getTextOrNull(node, "name"))
                            .fundType(getTextOrNull(node, "fundType"))
                            .quantity(getBigDecimalOrNull(node, "quantity"))
                            .closePrice(getBigDecimalOrNull(node, "closePrice"))
                            .totalValue(getBigDecimalOrNull(node, "totalValue"))
                            .institution(getTextOrNull(node, "institution"))
                            .build());
                }
            }

            // Parsear Dividendos
            List<B3ReportUploadResponse.DividendReceived> dividends = new ArrayList<>();
            if (root.has("dividends") && root.get("dividends").isArray()) {
                for (JsonNode node : root.get("dividends")) {
                    dividends.add(B3ReportUploadResponse.DividendReceived.builder()
                            .ticker(getTextOrNull(node, "ticker"))
                            .productName(getTextOrNull(node, "productName"))
                            .paymentDate(getTextOrNull(node, "paymentDate"))
                            .eventType(getTextOrNull(node, "eventType"))
                            .quantity(getBigDecimalOrNull(node, "quantity"))
                            .unitPrice(getBigDecimalOrNull(node, "unitPrice"))
                            .netValue(getBigDecimalOrNull(node, "netValue"))
                            .build());
                }
            }

            // Parsear Totais
            B3ReportUploadResponse.PortfolioTotals totals = null;
            if (root.has("totals")) {
                JsonNode totalsNode = root.get("totals");
                totals = B3ReportUploadResponse.PortfolioTotals.builder()
                        .stocks(getBigDecimalOrNull(totalsNode, "stocks"))
                        .fiis(getBigDecimalOrNull(totalsNode, "fiis"))
                        .fixedIncome(getBigDecimalOrNull(totalsNode, "fixedIncome"))
                        .funds(getBigDecimalOrNull(totalsNode, "funds"))
                        .dividends(getBigDecimalOrNull(totalsNode, "dividends"))
                        .grandTotal(getBigDecimalOrNull(totalsNode, "grandTotal"))
                        .build();
            }

            return B3ReportUploadResponse.builder()
                    .reportMonth(reportMonth)
                    .reportYear(reportYear)
                    .stocks(stocks)
                    .fiis(fiis)
                    .fixedIncome(fixedIncome)
                    .funds(funds)
                    .dividends(dividends)
                    .totals(totals)
                    .build();

        } catch (Exception e) {
            log.error("Erro ao parsear resposta da IA: {}", e.getMessage(), e);
            return B3ReportUploadResponse.builder()
                    .errorMessage("Erro ao parsear dados extraidos: " + e.getMessage())
                    .build();
        }
    }

    private String getTextOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    private BigDecimal getBigDecimalOrNull(JsonNode node, String field) {
        if (node.has(field) && !node.get(field).isNull()) {
            return new BigDecimal(node.get(field).asText());
        }
        return null;
    }
}
