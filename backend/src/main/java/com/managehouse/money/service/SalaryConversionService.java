package com.managehouse.money.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.managehouse.money.dto.OpenAIRequest;
import com.managehouse.money.dto.OpenAIResponse;
import com.managehouse.money.dto.SalaryConversionProcessResponse;
import com.managehouse.money.dto.SalaryConversionRequest;
import com.managehouse.money.dto.SalaryConversionResponse;
import com.managehouse.money.entity.SalaryConversion;
import com.managehouse.money.entity.User;
import com.managehouse.money.repository.SalaryConversionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalaryConversionService {
    
    private final SalaryConversionRepository salaryConversionRepository;
    private final UserService userService;
    private final ConfigurationService configurationService;
    
    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;
    
    @Value("${openai.model:gpt-4o-mini}")
    private String model;
    
    private WebClient webClient;
    
    public SalaryConversionProcessResponse processConversionText(String text) {
        String apiKey = configurationService.getOpenAIKey();
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("OpenAI API key não configurada");
            return new SalaryConversionProcessResponse(null, null, null, null, null, "API Key do OpenAI não configurada");
        }

        try {
            String prompt = buildPrompt(text);

            List<OpenAIRequest.Message> messages = new ArrayList<>();
            messages.add(new OpenAIRequest.Message("system",
                "Você é um assistente especializado em análise de informações de conversão de moeda. " +
                "Sua tarefa é extrair informações de conversão de USD para BRL: data da conversão, cotação, valor em USD, VET, e valor final em BRL."));
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

            return new SalaryConversionProcessResponse(null, null, null, null, null, "Não foi possível processar o texto");

        } catch (Exception e) {
            log.error("Erro ao processar com IA", e);
            return new SalaryConversionProcessResponse(null, null, null, null, null, "Erro ao processar: " + e.getMessage());
        }
    }
    
    private String buildPrompt(String text) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analise o seguinte texto sobre conversão de moeda e extraia as seguintes informações em formato JSON:\n\n");
        prompt.append("O texto pode conter:\n");
        prompt.append("- Token de cotação com data e cotação\n");
        prompt.append("- Informações de saque (Valor do saque em USD, IOF, Taxa da operação)\n");
        prompt.append("- VET (Valor Efetivo da Taxa)\n");
        prompt.append("- Valor antes da taxa em BRL\n");
        prompt.append("- Valor da taxa em BRL\n");
        prompt.append("- Valor convertido final em BRL\n\n");
        prompt.append("Extraia as seguintes informações em formato JSON:\n");
        prompt.append("{\n");
        prompt.append("  \"conversionDate\": \"YYYY-MM-DD\", // Data da conversão (formato ISO). Procure por 'Data:' no token de cotação\n");
        prompt.append("  \"exchangeRate\": 5.464513, // Cotação (número decimal). Procure por 'Cotação:' no token de cotação\n");
        prompt.append("  \"amountUSD\": 4534.60, // Valor do saque em USD (número decimal, sem pontos de milhar). Procure por 'Valor do saque:'\n");
        prompt.append("  \"vet\": 5.437190, // VET - Valor Efetivo da Taxa (número decimal). Procure por 'VET:'\n");
        prompt.append("  \"finalAmountBRL\": 24655.48 // Valor convertido final em BRL (número decimal, sem pontos de milhar). Procure por 'Valor convertido final:'\n");
        prompt.append("}\n\n");
        prompt.append("Importante:\n");
        prompt.append("- Extraia a data do formato DD/MM/YYYY HH:MM:SS BRT ou DD/MM/YYYY e converta para YYYY-MM-DD\n");
        prompt.append("- Todos os valores numéricos devem ser números decimais (use ponto como separador decimal)\n");
        prompt.append("- Remova pontos de milhar dos valores (ex: 4.534,60 vira 4534.60)\n");
        prompt.append("- Remova vírgulas de milhar e use ponto como separador decimal\n");
        prompt.append("- Se encontrar 'IOF: Zero', ignore o IOF\n");
        prompt.append("- Se encontrar 'Taxa da operação: X%', use essa informação para validar os cálculos, mas não inclua no JSON\n");
        prompt.append("- Retorne APENAS o JSON válido, sem texto adicional antes ou depois\n\n");
        prompt.append("Texto:\n");
        prompt.append(text);
        
        return prompt.toString();
    }
    
    private SalaryConversionProcessResponse parseAIResponse(String content) {
        ObjectMapper objectMapper = new ObjectMapper();
        
        try {
            // Tentar extrair JSON da resposta
            String jsonContent = extractJSON(content);
            if (jsonContent == null) {
                log.warn("Não foi possível extrair JSON da resposta da IA");
                return new SalaryConversionProcessResponse(null, null, null, null, null, "Não foi possível extrair JSON da resposta");
            }
            
            // Parse do JSON
            Map<String, Object> data = objectMapper.readValue(jsonContent, new TypeReference<Map<String, Object>>() {});
            
            LocalDate conversionDate = null;
            if (data.get("conversionDate") != null) {
                conversionDate = LocalDate.parse(data.get("conversionDate").toString());
            }
            
            BigDecimal exchangeRate = parseBigDecimal(data.get("exchangeRate"));
            BigDecimal amountUSD = parseBigDecimal(data.get("amountUSD"));
            BigDecimal vet = parseBigDecimal(data.get("vet"));
            BigDecimal finalAmountBRL = parseBigDecimal(data.get("finalAmountBRL"));
            
            return new SalaryConversionProcessResponse(
                conversionDate,
                exchangeRate,
                amountUSD,
                vet,
                finalAmountBRL,
                null
            );
            
        } catch (Exception e) {
            log.error("Erro ao fazer parse da resposta da IA", e);
            return new SalaryConversionProcessResponse(null, null, null, null, null, "Erro ao processar resposta: " + e.getMessage());
        }
    }
    
    private BigDecimal parseBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        try {
            String str = value.toString().replace(",", ".");
            return new BigDecimal(str);
        } catch (Exception e) {
            log.error("Erro ao converter para BigDecimal: " + value, e);
            return null;
        }
    }
    
    private String extractJSON(String content) {
        // Tentar encontrar JSON na resposta
        Pattern jsonPattern = Pattern.compile("\\{.*\\}", Pattern.DOTALL);
        Matcher matcher = jsonPattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(0);
        }
        return null;
    }
    
    @Transactional
    public SalaryConversionResponse createOrUpdateConversion(SalaryConversionRequest request) {
        User user = userService.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verifica se já existe uma conversão para este usuário, mês e ano
        SalaryConversion conversion = salaryConversionRepository
                .findByUserIdAndMonthAndYear(request.getUserId(), request.getMonth(), request.getYear())
                .orElse(new SalaryConversion());

        conversion.setUser(user);
        conversion.setMonth(request.getMonth());
        conversion.setYear(request.getYear());
        conversion.setConversionDate(request.getConversionDate());
        conversion.setExchangeRate(request.getExchangeRate());
        conversion.setAmountUSD(request.getAmountUSD());
        conversion.setVet(request.getVet());
        conversion.setFinalAmountBRL(request.getFinalAmountBRL());

        SalaryConversion saved = salaryConversionRepository.save(conversion);
        return toResponse(saved);
    }
    
    public SalaryConversionResponse getConversionByMonthAndYear(Long userId, Integer month, Integer year) {
        return salaryConversionRepository
                .findByUserIdAndMonthAndYear(userId, month, year)
                .map(this::toResponse)
                .orElse(null);
    }
    
    private SalaryConversionResponse toResponse(SalaryConversion conversion) {
        return new SalaryConversionResponse(
                conversion.getId(),
                conversion.getUser().getId(),
                conversion.getMonth(),
                conversion.getYear(),
                conversion.getConversionDate(),
                conversion.getExchangeRate(),
                conversion.getAmountUSD(),
                conversion.getVet(),
                conversion.getFinalAmountBRL()
        );
    }
}

