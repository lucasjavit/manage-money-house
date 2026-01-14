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
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
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
        return processReport(base64Content, userId, null);
    }

    public B3ReportUploadResponse processReport(String base64Content, Long userId, String fileName) {
        log.info("Processando relatorio B3 para usuario {}", userId);

        try {
            // 1. Decodificar arquivo
            byte[] fileBytes = Base64.getDecoder().decode(base64Content);
            log.info("Arquivo decodificado: {} bytes", fileBytes.length);

            // 2. Detectar tipo de arquivo e extrair texto
            String extractedText;
            String fileType = detectFileType(fileBytes, fileName);

            if ("EXCEL".equals(fileType)) {
                log.info("Detectado arquivo Excel");
                extractedText = extractTextFromExcel(fileBytes, fileName);
            } else {
                log.info("Detectado arquivo PDF");
                extractedText = extractTextFromPDF(fileBytes);
            }

            log.info("Texto extraido: {} caracteres", extractedText.length());
            log.debug("Texto extraido (primeiros 2000 chars): {}", extractedText.substring(0, Math.min(2000, extractedText.length())));

            // Log para verificar se Tesouro Direto está no texto extraído
            if (extractedText.contains("Tesouro")) {
                log.info("TESOURO DIRETO ENCONTRADO no texto extraido!");
                int idx = extractedText.indexOf("Tesouro");
                log.info("Trecho com Tesouro: {}", extractedText.substring(Math.max(0, idx - 100), Math.min(extractedText.length(), idx + 500)));
            } else {
                log.warn("TESOURO DIRETO NAO ENCONTRADO no texto extraido!");
            }

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
            log.info("Resposta da IA (completa): {}", aiResponse);

            // Verificar se Tesouro foi extraído
            if (aiResponse.contains("TESOURO") || aiResponse.contains("Tesouro")) {
                log.info("TESOURO encontrado na resposta da IA!");
            } else {
                log.warn("TESOURO NAO encontrado na resposta da IA!");
            }

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

    private String detectFileType(byte[] fileBytes, String fileName) {
        // Verificar por extensao do arquivo
        if (fileName != null) {
            String lowerName = fileName.toLowerCase();
            if (lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls")) {
                return "EXCEL";
            }
            if (lowerName.endsWith(".pdf")) {
                return "PDF";
            }
        }

        // Verificar por magic bytes
        if (fileBytes.length >= 4) {
            // PDF: %PDF
            if (fileBytes[0] == 0x25 && fileBytes[1] == 0x50 &&
                fileBytes[2] == 0x44 && fileBytes[3] == 0x46) {
                return "PDF";
            }
            // XLSX (ZIP): PK
            if (fileBytes[0] == 0x50 && fileBytes[1] == 0x4B) {
                return "EXCEL";
            }
            // XLS (OLE): D0 CF 11 E0
            if (fileBytes.length >= 8 &&
                (fileBytes[0] & 0xFF) == 0xD0 && (fileBytes[1] & 0xFF) == 0xCF &&
                (fileBytes[2] & 0xFF) == 0x11 && (fileBytes[3] & 0xFF) == 0xE0) {
                return "EXCEL";
            }
        }

        // Padrao: PDF
        return "PDF";
    }

    private String extractTextFromExcel(byte[] excelBytes, String fileName) throws IOException {
        StringBuilder text = new StringBuilder();

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(excelBytes)) {
            Workbook workbook;

            // Determinar tipo de Excel
            boolean isXlsx = fileName != null && fileName.toLowerCase().endsWith(".xlsx");
            if (!isXlsx && fileName != null && fileName.toLowerCase().endsWith(".xls")) {
                workbook = new HSSFWorkbook(inputStream);
            } else {
                // Tentar XLSX primeiro (mais comum)
                workbook = new XSSFWorkbook(inputStream);
            }

            try {
                DataFormatter formatter = new DataFormatter();

                // Log todas as planilhas encontradas
                log.info("Excel possui {} planilhas:", workbook.getNumberOfSheets());
                for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                    log.info("  - Planilha {}: '{}'", i, workbook.getSheetAt(i).getSheetName());
                }

                // Iterar por todas as planilhas
                for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                    Sheet sheet = workbook.getSheetAt(sheetIndex);
                    String sheetName = sheet.getSheetName();
                    log.info("Processando planilha: '{}'", sheetName);
                    text.append("\n=== PLANILHA: ").append(sheetName).append(" ===\n\n");

                    // Iterar por todas as linhas
                    for (Row row : sheet) {
                        StringBuilder rowText = new StringBuilder();
                        boolean hasContent = false;

                        for (Cell cell : row) {
                            String cellValue = formatter.formatCellValue(cell).trim();
                            if (!cellValue.isEmpty()) {
                                hasContent = true;
                                if (rowText.length() > 0) {
                                    rowText.append("\t");
                                }
                                rowText.append(cellValue);
                            }
                        }

                        if (hasContent) {
                            text.append(rowText).append("\n");
                        }
                    }
                }
            } finally {
                workbook.close();
            }
        }

        return text.toString();
    }

    private String buildExtractionPrompt(String text) {
        return """
            Voce e um especialista em extrair dados de relatorios consolidados da B3.

            IMPORTANTE: Extraia TODOS os dados do relatorio abaixo e retorne APENAS JSON valido (sem markdown, sem ```).

            O relatorio contem secoes/planilhas como:
            - Posicao - Acoes (tickers como BBAS3, VALE3, PETR4, etc)
            - Posicao - FII - Fundo de Investimento Imobiliario (tickers terminados em 11)
            - Posicao - CDB - Certificado de Deposito Bancario
            - Posicao - LCA - Letra de Credito do Agronegocio
            - Posicao - DEB - Debentures
            - Posicao - Tesouro Direto (planilha com titulos do governo: Tesouro IPCA+, Tesouro Selic, Tesouro Prefixado, etc)
            - Posicao - Fundos de Investimentos (FIAgro, etc)
            - Proventos recebidos (Dividendos, JCP, Rendimentos)

            ATENCAO ESPECIAL PARA TESOURO DIRETO:
            A planilha "Posicao - Tesouro Direto" contem colunas: Produto e Valor Atualizado.
            Para cada titulo do Tesouro, extraia APENAS:
            - product: nome do produto (ex: "Tesouro Selic 2027", "Tesouro IPCA+ 2026")
            - totalValue: valor da coluna "Valor Atualizado" (ex: 46488.90, 11117.13)
            - productType: sempre "TESOURO"
            Os demais campos podem ser null.

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
                },
                {
                  "product": "Tesouro IPCA+ 2026",
                  "productType": "TESOURO",
                  "totalValue": 11117.13
                },
                {
                  "product": "Tesouro Selic 2027",
                  "productType": "TESOURO",
                  "totalValue": 46488.90
                },
                {
                  "product": "Tesouro Selic 2029",
                  "productType": "TESOURO",
                  "totalValue": 41338.38
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
                "grandTotal": 318151.65
              }
            }

            REGRAS:
            1. Extraia a data do relatorio (mes/ano) do cabecalho "Data: MM/YYYY"
            2. Para acoes, separe o ticker do nome (ex: "BBAS3 - BCO BRASIL S.A." -> ticker: "BBAS3", name: "Banco do Brasil")
            3. Para tipo de acao, use ON, PN, PNA, PNB conforme indicado
            4. Para FIIs, o ticker sempre termina em 11 (ex: RECR11, SNCI11)
            5. Para renda fixa, identifique o tipo (CDB, LCA, LCI, DEBENTURE, TESOURO) pelo nome do produto
            6. CRITICO - TESOURO DIRETO: Na planilha "Posicao - Tesouro Direto", extraia TODOS os titulos (Tesouro IPCA+, Tesouro Selic, Tesouro Prefixado). Para cada um, inclua em fixedIncome com: product (nome), productType="TESOURO", totalValue (coluna "Valor Atualizado"). NAO IGNORE NENHUM TITULO!
            7. Converta datas para formato ISO (YYYY-MM-DD)
            8. Use numeros decimais (nao strings) para valores monetarios
            9. Some os totais de cada categoria (incluindo Tesouro Direto em fixedIncome)
            10. Calcule grandTotal somando TODOS os ativos (acoes + fiis + fixedIncome + funds)

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
