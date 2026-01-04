package com.managehouse.money.service;

import com.managehouse.money.dto.BoletoProcessResponse;
import com.managehouse.money.dto.ExtractUploadRequest;
import com.managehouse.money.dto.OpenAIRequest;
import com.managehouse.money.dto.OpenAIResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoletoService {
    
    private final ConfigurationService configurationService;
    
    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;
    
    @Value("${openai.model:gpt-4o-mini}")
    private String model;
    
    private WebClient webClient;
    
    public BoletoProcessResponse processBoleto(ExtractUploadRequest request) {
        try {
            // 1. Extrair texto do arquivo
            String extractedText = extractTextFromFile(request);
            if (extractedText == null || extractedText.trim().isEmpty()) {
                return new BoletoProcessResponse(
                    null,
                    null,
                    null,
                    "Não foi possível extrair texto do arquivo. Verifique se o arquivo é válido."
                );
            }

            // 2. Processar com IA
            BoletoProcessResponse response = processWithAI(extractedText);
            
            return response;

        } catch (Exception e) {
            log.error("Erro ao processar boleto", e);
            return new BoletoProcessResponse(
                null,
                null,
                null,
                "Erro ao processar boleto: " + e.getMessage()
            );
        }
    }

    private String extractTextFromFile(ExtractUploadRequest request) throws IOException {
        byte[] fileBytes = Base64.getDecoder().decode(request.getFileContent());

        if ("pdf".equalsIgnoreCase(request.getFileType())) {
            return extractTextFromPDF(fileBytes);
        } else if ("png".equalsIgnoreCase(request.getFileType()) ||
                   "jpg".equalsIgnoreCase(request.getFileType()) ||
                   "jpeg".equalsIgnoreCase(request.getFileType())) {
            return "IMAGEM_DETECTADA: Para processar imagens, é necessário OCR. Por favor, converta para PDF ou texto primeiro.";
        }

        return null;
    }

    private String extractTextFromPDF(byte[] pdfBytes) throws IOException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(pdfBytes);
             PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private BoletoProcessResponse processWithAI(String text) {
        String apiKey = configurationService.getOpenAIKey();
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("OpenAI API key não configurada");
            return new BoletoProcessResponse(null, null, null, "API Key do OpenAI não configurada");
        }

        try {
            String prompt = buildPrompt(text);

            List<OpenAIRequest.Message> messages = new ArrayList<>();
            messages.add(new OpenAIRequest.Message("system",
                "Você é um assistente especializado em análise de boletos bancários. " +
                "Sua tarefa é extrair informações de boletos: descrição/nome, valor e data de vencimento."));
            messages.add(new OpenAIRequest.Message("user", prompt));

            OpenAIRequest openAIRequest = new OpenAIRequest(model, messages, 0.3);

            if (webClient == null) {
                webClient = WebClient.builder()
                    .baseUrl(apiUrl)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .build();
            }

            OpenAIResponse response = webClient.post()
                .bodyValue(openAIRequest)
                .retrieve()
                .bodyToMono(OpenAIResponse.class)
                .block();

            if (response != null &&
                response.getChoices() != null &&
                !response.getChoices().isEmpty()) {
                String content = response.getChoices().get(0).getMessage().getContent();
                return parseAIResponse(content);
            }

            return new BoletoProcessResponse(null, null, null, "Não foi possível processar o boleto");

        } catch (Exception e) {
            log.error("Erro ao processar com IA", e);
            return new BoletoProcessResponse(null, null, null, "Erro ao processar com IA: " + e.getMessage());
        }
    }

    private String buildPrompt(String text) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analise o seguinte boleto bancário e extraia as seguintes informações:\n\n");
        prompt.append("1. DESCRIÇÃO/NOME do boleto (ex: 'IPTU', 'Conta de Luz', 'Aluguel', etc.)\n");
        prompt.append("2. VALOR do boleto (valor numérico em BRL)\n");
        prompt.append("3. DATA DE VENCIMENTO (data completa: dia, mês e ano)\n\n");
        prompt.append("Retorne APENAS um JSON no seguinte formato:\n");
        prompt.append("{\n");
        prompt.append("  \"description\": \"Nome/Descrição do boleto\",\n");
        prompt.append("  \"amount\": valor_numérico,\n");
        prompt.append("  \"dueDate\": \"YYYY-MM-DD\"\n");
        prompt.append("}\n\n");
        prompt.append("Regras IMPORTANTES:\n");
        prompt.append("- A descrição deve ser clara e identificável (ex: 'IPTU', 'Conta de Luz', 'Aluguel')\n");
        prompt.append("- O valor deve ser um número positivo (sem símbolos de moeda)\n");
        prompt.append("- A data de vencimento DEVE ser extraída do boleto (dia, mês e ano completos)\n");
        prompt.append("- Se a data estiver incompleta no boleto, use a data de referência do documento\n");
        prompt.append("- Retorne APENAS o JSON válido, sem texto adicional antes ou depois\n\n");
        prompt.append("Boleto:\n");
        prompt.append(text);

        return prompt.toString();
    }

    private BoletoProcessResponse parseAIResponse(String content) {
        ObjectMapper objectMapper = new ObjectMapper();
        
        try {
            String jsonContent = extractJSON(content);
            if (jsonContent == null) {
                log.warn("Não foi possível extrair JSON da resposta da IA");
                return new BoletoProcessResponse(null, null, null, "Não foi possível extrair dados do boleto");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> boletoData = (Map<String, Object>) objectMapper.readValue(jsonContent, Map.class);

            String description = (String) boletoData.get("description");
            
            Object amountObj = boletoData.get("amount");
            BigDecimal amount = null;
            if (amountObj instanceof Number) {
                amount = new BigDecimal(amountObj.toString());
            } else if (amountObj instanceof String) {
                // Remover símbolos de moeda e espaços
                String amountStr = ((String) amountObj).replaceAll("[R$\\s\\.]", "").replace(",", ".");
                amount = new BigDecimal(amountStr);
            }
            
            String dateStr = (String) boletoData.get("dueDate");
            LocalDate dueDate = null;
            if (dateStr != null && !dateStr.isEmpty()) {
                try {
                    dueDate = LocalDate.parse(dateStr);
                } catch (Exception e) {
                    log.warn("Erro ao parsear data: " + dateStr);
                }
            }

            // Validações
            if (description == null || description.trim().isEmpty()) {
                return new BoletoProcessResponse(null, null, null, "Descrição do boleto não encontrada");
            }
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                return new BoletoProcessResponse(null, null, null, "Valor do boleto inválido");
            }
            if (dueDate == null) {
                return new BoletoProcessResponse(null, null, null, "Data de vencimento não encontrada");
            }

            return new BoletoProcessResponse(description, amount, dueDate, null);

        } catch (Exception e) {
            log.error("Erro ao parsear resposta da IA", e);
            return new BoletoProcessResponse(null, null, null, "Erro ao processar dados do boleto: " + e.getMessage());
        }
    }

    private String extractJSON(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');

        if (start != -1 && end != -1 && end > start) {
            return content.substring(start, end + 1);
        }

        return null;
    }
}

