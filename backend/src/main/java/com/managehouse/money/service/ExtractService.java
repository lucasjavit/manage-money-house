package com.managehouse.money.service;

import com.managehouse.money.dto.ExtractProcessResponse;
import com.managehouse.money.dto.ExtractUploadRequest;
import com.managehouse.money.dto.IdentifiedTransaction;
import com.managehouse.money.dto.OpenAIRequest;
import com.managehouse.money.dto.OpenAIResponse;
import com.managehouse.money.entity.ExpenseType;
import com.managehouse.money.repository.ExpenseTypeRepository;
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.Base64;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExtractService {
    
    private final ConfigurationService configurationService;
    private final ExpenseTypeRepository expenseTypeRepository;
    
    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;
    
    @Value("${openai.model:gpt-4o-mini}")
    private String model;
    
    private WebClient webClient;
    
    public ExtractProcessResponse processExtract(ExtractUploadRequest request) {
        try {
            // 1. Extrair texto do arquivo
            String extractedText = extractTextFromFile(request);
            if (extractedText == null || extractedText.trim().isEmpty()) {
                return new ExtractProcessResponse(
                    Collections.emptyList(),
                    "",
                    "Não foi possível extrair texto do arquivo. Verifique se o arquivo é válido."
                );
            }
            
            // 2. Obter tipos de despesa para contexto
            List<ExpenseType> expenseTypes = expenseTypeRepository.findAll();
            String expenseTypesContext = expenseTypes.stream()
                .map(et -> et.getId() + ": " + et.getName())
                .collect(Collectors.joining(", "));
            
            // 3. Processar com IA
            List<IdentifiedTransaction> transactions = processWithAI(
                extractedText,
                expenseTypesContext
            );
            
            return new ExtractProcessResponse(transactions, extractedText, null);
            
        } catch (Exception e) {
            log.error("Erro ao processar extrato", e);
            return new ExtractProcessResponse(
                Collections.emptyList(),
                "",
                "Erro ao processar extrato: " + e.getMessage()
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
            // Para imagens, retornamos uma mensagem indicando que precisa de OCR
            // Por enquanto, retornamos null - pode ser implementado com Tesseract depois
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
    
    private List<IdentifiedTransaction> processWithAI(
            String text,
            String expenseTypesContext) {
        
        String apiKey = configurationService.getOpenAIKey();
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("OpenAI API key não configurada");
            return Collections.emptyList();
        }
        
        try {
            String prompt = buildPrompt(text, expenseTypesContext);
            
            List<OpenAIRequest.Message> messages = new ArrayList<>();
            messages.add(new OpenAIRequest.Message("system", 
                "Você é um assistente especializado em análise de extratos bancários e cartões de crédito. " +
                "Sua tarefa é identificar transações e categorizá-las corretamente."));
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
            
            return Collections.emptyList();
            
        } catch (Exception e) {
            log.error("Erro ao processar com IA", e);
            return Collections.emptyList();
        }
    }
    
    private String buildPrompt(String text, String expenseTypesContext) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analise o seguinte extrato bancário/cartão de crédito e identifique todas as transações.\n\n");
        prompt.append("Tipos de despesa disponíveis (ID: Nome): ").append(expenseTypesContext).append("\n\n");
        prompt.append("GUIA DE MAPEAMENTO DE TIPOS (identifique pelo nome da transação):\n");
        prompt.append("- Aluguel: palavras como 'aluguel', 'rent', 'locação', 'imobiliária'\n");
        prompt.append("- Condomínio: 'condomínio', 'condominio', 'síndico', 'taxa condominial'\n");
        prompt.append("- Luz: 'luz', 'energia', 'eletricidade', 'light', 'cemig', 'copel', 'enel'\n");
        prompt.append("- Água: 'água', 'agua', 'sanepar', 'sabesp', 'caesb', 'copasa', 'water'\n");
        prompt.append("- Gás: 'gás', 'gas', 'gás natural', 'comgás', 'petrobras'\n");
        prompt.append("- IPTU: 'iptu', 'imposto predial', 'taxa urbana'\n");
        prompt.append("- Internet: 'internet', 'banda larga', 'net', 'claro', 'vivo', 'oi', 'tim'\n");
        prompt.append("- Mercado: 'supermercado', 'mercado', 'atacadão', 'carrefour', 'extra', 'pao de açucar', 'walmart', 'big', 'assai'\n");
        prompt.append("- Marmitas: 'marmita', 'delivery', 'ifood', 'rappi', 'uber eats', 'comida delivery'\n");
        prompt.append("- Saladas: 'salada', 'salad', 'healthy', 'verde'\n");
        prompt.append("- Diarista: 'diarista', 'faxina', 'limpeza', 'empregada', 'doméstica'\n");
        prompt.append("- Viagem: 'viagem', 'hotel', 'hospedagem', 'airbnb', 'booking', 'passagem', 'aéreo', 'avião'\n");
        prompt.append("- Carro: 'combustível', 'gasolina', 'posto', 'ipva', 'seguro auto', 'oficina', 'mecânico', 'estacionamento'\n");
        prompt.append("- Outros: qualquer transação que não se encaixe nos tipos acima\n\n");
        prompt.append("Para cada transação identificada, retorne no seguinte formato JSON:\n");
        prompt.append("[\n");
        prompt.append("  {\n");
        prompt.append("    \"description\": \"Descrição da transação\",\n");
        prompt.append("    \"amount\": valor_numérico,\n");
        prompt.append("    \"date\": \"YYYY-MM-DD\",\n");
        prompt.append("    \"expenseTypeId\": id_do_tipo,\n");
        prompt.append("    \"expenseTypeName\": \"Nome do tipo EXATO (deve corresponder exatamente a um dos tipos disponíveis)\",\n");
        prompt.append("    \"confidence\": \"high|medium|low\"\n");
        prompt.append("  }\n");
        prompt.append("]\n\n");
        prompt.append("Regras IMPORTANTES:\n");
        prompt.append("- EXTRAIA a data COMPLETA (dia, mês e ano) de cada transação do extrato\n");
        prompt.append("- Se a data estiver incompleta no extrato, use a data de referência do extrato (geralmente no cabeçalho)\n");
        prompt.append("- Se não houver data específica, procure por períodos de referência no documento\n");
        prompt.append("- IDENTIFIQUE o tipo de despesa analisando a DESCRIÇÃO/NOME da transação usando o guia acima\n");
        prompt.append("- O expenseTypeName DEVE ser EXATAMENTE igual a um dos nomes disponíveis (case-sensitive)\n");
        prompt.append("- Use o expenseTypeId correspondente ao tipo identificado\n");
        prompt.append("- IMPORTANTE: Se não conseguir identificar claramente o tipo baseado na descrição, SEMPRE use 'Outros'\n");
        prompt.append("- Quando em dúvida, prefira 'Outros' ao invés de tentar adivinhar\n");
        prompt.append("- Valores devem ser positivos (já são despesas)\n");
        prompt.append("- Seja preciso na identificação das datas - isso é crítico para salvar corretamente no banco de dados\n");
        prompt.append("- Retorne APENAS o JSON válido, sem texto adicional antes ou depois\n\n");
        prompt.append("Extrato:\n");
        prompt.append(text);
        
        return prompt.toString();
    }
    
    private List<IdentifiedTransaction> parseAIResponse(String content) {
        List<IdentifiedTransaction> transactions = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        
        try {
            // Tentar extrair JSON da resposta
            String jsonContent = extractJSON(content);
            if (jsonContent == null) {
                log.warn("Não foi possível extrair JSON da resposta da IA");
                return transactions;
            }
            
            // Parse do JSON usando Jackson
            List<Map<String, Object>> rawTransactions = objectMapper.readValue(
                jsonContent,
                new TypeReference<List<Map<String, Object>>>() {}
            );
            
            for (Map<String, Object> raw : rawTransactions) {
                try {
                    IdentifiedTransaction transaction = new IdentifiedTransaction();
                    transaction.setDescription((String) raw.get("description"));
                    
                    // Converter amount (pode ser Number ou String)
                    Object amountObj = raw.get("amount");
                    if (amountObj instanceof Number) {
                        transaction.setAmount(new java.math.BigDecimal(amountObj.toString()));
                    } else if (amountObj instanceof String) {
                        transaction.setAmount(new java.math.BigDecimal((String) amountObj));
                    }
                    
                    // Converter date
                    String dateStr = (String) raw.get("date");
                    transaction.setDate(LocalDate.parse(dateStr));
                    
                    // Converter expenseTypeId
                    Object typeIdObj = raw.get("expenseTypeId");
                    if (typeIdObj instanceof Number) {
                        transaction.setExpenseTypeId(((Number) typeIdObj).longValue());
                    } else if (typeIdObj instanceof String) {
                        transaction.setExpenseTypeId(Long.parseLong((String) typeIdObj));
                    }
                    
                    String expenseTypeName = (String) raw.get("expenseTypeName");
                    transaction.setExpenseTypeName(expenseTypeName);
                    transaction.setConfidence((String) raw.get("confidence"));
                    
                    // Validar e mapear tipo de despesa - se não conseguir identificar, usar "Outros"
                    List<ExpenseType> expenseTypes = expenseTypeRepository.findAll();
                    
                    // Se expenseTypeId não foi fornecido, tentar mapear pelo nome
                    if (transaction.getExpenseTypeId() == null && expenseTypeName != null) {
                        Optional<ExpenseType> matchedType = expenseTypes.stream()
                            .filter(et -> et.getName().equalsIgnoreCase(expenseTypeName.trim()))
                            .findFirst();
                        
                        if (matchedType.isPresent()) {
                            transaction.setExpenseTypeId(matchedType.get().getId());
                            transaction.setExpenseTypeName(matchedType.get().getName()); // Garantir nome exato
                        } else {
                            // Tentar mapear por palavras-chave na descrição
                            Long mappedId = mapExpenseTypeByName(transaction.getDescription(), expenseTypes);
                            if (mappedId != null) {
                                transaction.setExpenseTypeId(mappedId);
                                ExpenseType matched = expenseTypes.stream()
                                    .filter(et -> et.getId().equals(mappedId))
                                    .findFirst()
                                    .orElse(null);
                                if (matched != null) {
                                    transaction.setExpenseTypeName(matched.getName());
                                }
                            } else {
                                // Se não conseguiu mapear, usar "Outros"
                                Optional<ExpenseType> outrosType = expenseTypes.stream()
                                    .filter(et -> et.getName().equalsIgnoreCase("Outros"))
                                    .findFirst();
                                if (outrosType.isPresent()) {
                                    transaction.setExpenseTypeId(outrosType.get().getId());
                                    transaction.setExpenseTypeName(outrosType.get().getName());
                                    log.info("Tipo não identificado, usando 'Outros' para: " + transaction.getDescription());
                                }
                            }
                        }
                    }
                    
                    // Se ainda não tem tipo, usar "Outros" como padrão
                    if (transaction.getExpenseTypeId() == null) {
                        Optional<ExpenseType> outrosType = expenseTypes.stream()
                            .filter(et -> et.getName().equalsIgnoreCase("Outros"))
                            .findFirst();
                        if (outrosType.isPresent()) {
                            transaction.setExpenseTypeId(outrosType.get().getId());
                            transaction.setExpenseTypeName(outrosType.get().getName());
                            log.info("Tipo não fornecido pela IA, usando 'Outros' para: " + transaction.getDescription());
                        }
                    }
                    
                    // Validar antes de adicionar
                    if (isValidTransaction(transaction)) {
                        transactions.add(transaction);
                    } else {
                        log.warn("Transação inválida ignorada: " + transaction.getDescription());
                    }
                    
                } catch (Exception e) {
                    log.warn("Erro ao parsear transação: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("Erro ao parsear resposta da IA", e);
        }
        
        return transactions;
    }
    
    private Long mapExpenseTypeByName(String description, List<ExpenseType> expenseTypes) {
        if (description == null) return null;
        
        String descLower = description.toLowerCase();
        
        // Mapeamento por palavras-chave
        for (ExpenseType type : expenseTypes) {
            String typeName = type.getName().toLowerCase();
            
            switch (typeName) {
                case "aluguel":
                    if (descLower.contains("aluguel") || descLower.contains("rent") || descLower.contains("locação") || descLower.contains("imobiliária")) {
                        return type.getId();
                    }
                    break;
                case "condomínio":
                    if (descLower.contains("condomínio") || descLower.contains("condominio") || descLower.contains("síndico")) {
                        return type.getId();
                    }
                    break;
                case "luz":
                    if (descLower.contains("luz") || descLower.contains("energia") || descLower.contains("eletricidade") || 
                        descLower.contains("cemig") || descLower.contains("copel") || descLower.contains("enel") || descLower.contains("light")) {
                        return type.getId();
                    }
                    break;
                case "água":
                    if (descLower.contains("água") || descLower.contains("agua") || descLower.contains("sanepar") || 
                        descLower.contains("sabesp") || descLower.contains("caesb") || descLower.contains("copasa") || descLower.contains("water")) {
                        return type.getId();
                    }
                    break;
                case "gás":
                    if (descLower.contains("gás") || descLower.contains("gas") || descLower.contains("comgás") || descLower.contains("petrobras")) {
                        return type.getId();
                    }
                    break;
                case "iptu":
                    if (descLower.contains("iptu") || descLower.contains("imposto predial")) {
                        return type.getId();
                    }
                    break;
                case "internet":
                    if (descLower.contains("internet") || descLower.contains("banda larga") || descLower.contains("net") || 
                        descLower.contains("claro") || descLower.contains("vivo") || descLower.contains("oi") || descLower.contains("tim")) {
                        return type.getId();
                    }
                    break;
                case "mercado":
                    if (descLower.contains("supermercado") || descLower.contains("mercado") || descLower.contains("atacadão") || 
                        descLower.contains("carrefour") || descLower.contains("extra") || descLower.contains("pao de açucar") || 
                        descLower.contains("walmart") || descLower.contains("big") || descLower.contains("assai")) {
                        return type.getId();
                    }
                    break;
                case "marmitas":
                    if (descLower.contains("marmita") || descLower.contains("delivery") || descLower.contains("ifood") || 
                        descLower.contains("rappi") || descLower.contains("uber eats")) {
                        return type.getId();
                    }
                    break;
                case "saladas":
                    if (descLower.contains("salada") || descLower.contains("salad") || descLower.contains("healthy")) {
                        return type.getId();
                    }
                    break;
                case "diarista":
                    if (descLower.contains("diarista") || descLower.contains("faxina") || descLower.contains("limpeza") || 
                        descLower.contains("empregada") || descLower.contains("doméstica")) {
                        return type.getId();
                    }
                    break;
                case "viagem":
                    if (descLower.contains("viagem") || descLower.contains("hotel") || descLower.contains("hospedagem") || 
                        descLower.contains("airbnb") || descLower.contains("booking") || descLower.contains("passagem") || 
                        descLower.contains("aéreo") || descLower.contains("avião")) {
                        return type.getId();
                    }
                    break;
                case "carro":
                    if (descLower.contains("combustível") || descLower.contains("gasolina") || descLower.contains("posto") || 
                        descLower.contains("ipva") || descLower.contains("seguro auto") || descLower.contains("oficina") || 
                        descLower.contains("mecânico") || descLower.contains("estacionamento")) {
                        return type.getId();
                    }
                    break;
            }
        }
        
        // Se não encontrou nenhum match, retorna null (será mapeado para "Outros" depois)
        return null;
    }
    
    private boolean isValidTransaction(IdentifiedTransaction transaction) {
        if (transaction.getDescription() == null || transaction.getDescription().trim().isEmpty()) {
            return false;
        }
        if (transaction.getAmount() == null || transaction.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return false;
        }
        if (transaction.getDate() == null) {
            return false;
        }
        if (transaction.getExpenseTypeId() == null) {
            return false;
        }
        // Validar se a data não é muito antiga ou futura (ex: entre 2020 e 2030)
        int year = transaction.getDate().getYear();
        if (year < 2020 || year > 2030) {
            return false;
        }
        return true;
    }
    
    private String extractJSON(String content) {
        // Procurar por array JSON
        int start = content.indexOf('[');
        int end = content.lastIndexOf(']');
        
        if (start != -1 && end != -1 && end > start) {
            return content.substring(start, end + 1);
        }
        
        // Se não encontrar array, procurar por objeto único
        start = content.indexOf('{');
        end = content.lastIndexOf('}');
        if (start != -1 && end != -1 && end > start) {
            return "[" + content.substring(start, end + 1) + "]";
        }
        
        return null;
    }
}

