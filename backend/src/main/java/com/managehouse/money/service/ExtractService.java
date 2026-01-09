package com.managehouse.money.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
import com.managehouse.money.dto.ExtractProcessResponse;
import com.managehouse.money.dto.ExtractUploadRequest;
import com.managehouse.money.dto.IdentifiedTransaction;
import com.managehouse.money.dto.OpenAIRequest;
import com.managehouse.money.dto.OpenAIResponse;
import com.managehouse.money.entity.ExtractExpenseType;
import com.managehouse.money.repository.ExtractExpenseTypeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExtractService {
    
    private final ConfigurationService configurationService;
    private final ExtractExpenseTypeRepository extractExpenseTypeRepository;
    
    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;
    
    @Value("${openai.model:gpt-4o-mini}")
    private String model;
    
    private WebClient webClient;
    
    /**
     * Melhora a identificação do tipo de transação usando IA para pesquisar/analisar o nome da transação
     */
    public IdentifiedTransaction improveTransactionTypeWithAI(IdentifiedTransaction transaction) {
        if (transaction == null || transaction.getDescription() == null) {
            return transaction;
        }
        
        String apiKey = configurationService.getOpenAIKey();
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("OpenAI API key não configurada para melhorar identificação");
            return transaction;
        }
        
        try {
            List<ExtractExpenseType> expenseTypes = extractExpenseTypeRepository.findAll();
            String expenseTypesContext = expenseTypes.stream()
                .map(et -> et.getId() + ": " + et.getName())
                .collect(Collectors.joining(", "));
            
            String prompt = buildTypeIdentificationPrompt(transaction.getDescription(), expenseTypesContext, 
                    transaction.getExpenseTypeName(), transaction.getConfidence());
            
            List<OpenAIRequest.Message> messages = new ArrayList<>();
            messages.add(new OpenAIRequest.Message("system", 
                "Você é um assistente especializado em identificar tipos de despesas baseado em nomes de transações bancárias. " +
                "Use seu conhecimento sobre empresas, marcas e padrões comuns para identificar corretamente o tipo de despesa. " +
                "Se necessário, faça uma análise semântica profunda do nome da transação."));
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
                IdentifiedTransaction improved = parseTypeIdentificationResponse(content, transaction, expenseTypes);
                if (improved != null) {
                    log.info("Tipo melhorado para '{}': {} -> {}", 
                        transaction.getDescription(), 
                        transaction.getExpenseTypeName(), 
                        improved.getExpenseTypeName());
                    return improved;
                }
            }
            
        } catch (Exception e) {
            log.warn("Erro ao melhorar tipo de transação com IA: {}", e.getMessage());
        }
        
        return transaction;
    }
    
    private String buildTypeIdentificationPrompt(String transactionName, String expenseTypesContext, 
            String currentType, String currentConfidence) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analise o seguinte nome de transação bancária e identifique o tipo de despesa mais apropriado.\n\n");
        prompt.append("Nome da transação: ").append(transactionName).append("\n\n");
        prompt.append("Tipo atual identificado: ").append(currentType != null ? currentType : "Não identificado").append("\n");
        prompt.append("Confiança atual: ").append(currentConfidence != null ? currentConfidence : "N/A").append("\n\n");
        prompt.append("Tipos de despesa disponíveis (ID: Nome): ").append(expenseTypesContext).append("\n\n");
        
        prompt.append("GUIA DE IDENTIFICAÇÃO:\n");
        prompt.append("- Alimentação: supermercado, mercado, atacadão, carrefour, ifood, rappi, uber eats, delivery, restaurante, padaria\n");
        prompt.append("- Moradia: aluguel, condomínio, luz, energia, água, gás, iptu, cemig, copel, enel, sanepar, sabesp, comgás\n");
        prompt.append("- Saúde: farmácia, drogaria, hospital, clínica, médico, dentista, plano de saúde, unimed, amil\n");
        prompt.append("- Automotivo: combustível, gasolina, posto, ipva, seguro auto, oficina, mecânico, estacionamento, shell, ipiranga\n");
        prompt.append("- Transporte: uber, 99, taxi, passagem aérea, latam, gol, azul, ônibus, metrô, bilhete único\n");
        prompt.append("- Educação: escola, colégio, universidade, faculdade, curso, mensalidade, material escolar, livraria\n");
        prompt.append("- Lazer: cinema, teatro, show, viagem, hotel, airbnb, netflix, spotify, streaming, academia\n");
        prompt.append("- Vestuário: roupa, moda, loja, shopping, calçado, zara, renner, riachuelo\n");
        prompt.append("- Tecnologia: internet, banda larga, claro, vivo, celular, smartphone, computador, notebook, apple, samsung\n");
        prompt.append("- Serviços: diarista, faxina, limpeza, manutenção, reparo, encanador, eletricista\n");
        prompt.append("- Compras: amazon, mercado livre, magazine luiza, americanas, casas bahia, compra online\n");
        prompt.append("- Financeiro: banco, tarifa, anuidade, juros, empréstimo, financiamento, cartão, fatura, pix, investimento\n");
        prompt.append("- Pets: pet shop, veterinário, ração, animal, petz, cobasi\n");
        prompt.append("- Outros: qualquer transação que não se encaixe nos tipos acima\n\n");
        
        prompt.append("INSTRUÇÕES:\n");
        prompt.append("1. Analise SEMANTICAMENTE o nome da transação\n");
        prompt.append("2. Use seu conhecimento sobre empresas, marcas e padrões comuns\n");
        prompt.append("3. Se o tipo atual for 'Outros' ou a confiança for 'low', faça uma análise mais profunda\n");
        prompt.append("4. Retorne APENAS um JSON válido no formato:\n");
        prompt.append("{\n");
        prompt.append("  \"expenseTypeId\": id_do_tipo,\n");
        prompt.append("  \"expenseTypeName\": \"Nome do tipo EXATO (deve corresponder exatamente a um dos tipos disponíveis)\",\n");
        prompt.append("  \"confidence\": \"high|medium|low\",\n");
        prompt.append("  \"reasoning\": \"Breve explicação do porquê esse tipo foi escolhido\"\n");
        prompt.append("}\n\n");
        prompt.append("Retorne APENAS o JSON, sem texto adicional.\n");
        
        return prompt.toString();
    }
    
    private IdentifiedTransaction parseTypeIdentificationResponse(String content, 
            IdentifiedTransaction original, List<ExtractExpenseType> expenseTypes) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonContent = extractJSON(content);
            if (jsonContent == null) {
                return null;
            }
            
            // Se for array, pegar o primeiro elemento
            if (jsonContent.trim().startsWith("[")) {
                List<Map<String, Object>> list = objectMapper.readValue(
                    jsonContent, 
                    new TypeReference<List<Map<String, Object>>>() {}
                );
                if (!list.isEmpty()) {
                    jsonContent = objectMapper.writeValueAsString(list.get(0));
                }
            }
            
            Map<String, Object> result = objectMapper.readValue(jsonContent, 
                new TypeReference<Map<String, Object>>() {});
            
            String expenseTypeName = (String) result.get("expenseTypeName");
            String reasoning = (String) result.get("reasoning");
            
            // Validar se o tipo existe
            if (expenseTypeName != null) {
                Optional<ExtractExpenseType> matchedType = expenseTypes.stream()
                    .filter(et -> et.getName().equalsIgnoreCase(expenseTypeName.trim()))
                    .findFirst();
                
                if (matchedType.isPresent()) {
                    IdentifiedTransaction improved = new IdentifiedTransaction();
                    improved.setDescription(original.getDescription());
                    improved.setAmount(original.getAmount());
                    improved.setDate(original.getDate());
                    improved.setExpenseTypeId(matchedType.get().getId());
                    improved.setExpenseTypeName(matchedType.get().getName());
                    improved.setConfidence((String) result.get("confidence"));
                    
                    log.info("Tipo melhorado para '{}': {} (Raciocínio: {})", 
                        original.getDescription(), expenseTypeName, reasoning);
                    
                    return improved;
                }
            }
            
        } catch (Exception e) {
            log.warn("Erro ao parsear resposta de identificação de tipo: {}", e.getMessage());
        }
        
        return null;
    }
    
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
            
            // 2. Obter tipos de despesa para contexto (usando tipos de extrato)
            List<ExtractExpenseType> expenseTypes = extractExpenseTypeRepository.findAll();
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
                "Sua tarefa é identificar transações e categorizá-las corretamente analisando SEMANTICAMENTE o nome/descrição de cada transação. " +
                "Use seu conhecimento sobre empresas, marcas e padrões comuns para identificar o tipo de despesa com precisão. " +
                "Seja inteligente e contextual na identificação, não apenas busque palavras isoladas."));
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
        prompt.append("GUIA DETALHADO DE IDENTIFICAÇÃO DE TIPOS (analise SEMANTICAMENTE o nome/descrição da transação):\n\n");
        
        prompt.append("1. ALIMENTAÇÃO - Identifique por:\n");
        prompt.append("   - Palavras-chave: 'supermercado', 'mercado', 'atacadão', 'carrefour', 'extra', 'pao de açucar', 'walmart', 'big', 'assai', 'ifood', 'rappi', 'uber eats', 'delivery', 'marmita', 'restaurante', 'lanchonete', 'padaria', 'açougue', 'peixaria', 'hortifruti'\n");
        prompt.append("   - Exemplos: 'CARREFOUR SUPERMERCADO', 'IFOOD DELIVERY', 'RESTAURANTE XYZ', 'PADARIA ABC'\n\n");
        
        prompt.append("2. MORADIA - Identifique por:\n");
        prompt.append("   - Palavras-chave: 'aluguel', 'rent', 'locação', 'imobiliária', 'condomínio', 'condominio', 'síndico', 'iptu', 'imposto predial', 'luz', 'energia', 'eletricidade', 'água', 'agua', 'gás', 'gas', 'saneamento', 'cemig', 'copel', 'enel', 'sanepar', 'sabesp', 'comgás'\n");
        prompt.append("   - Exemplos: 'ALUGUEL APTO', 'CONDOMÍNIO EDIFÍCIO', 'CEMIG ENERGIA', 'SANEPAR ÁGUA', 'IPTU 2025'\n\n");
        
        prompt.append("3. SAÚDE - Identifique por:\n");
        prompt.append("   - Palavras-chave: 'farmácia', 'farmacia', 'drogaria', 'medicamento', 'remédio', 'hospital', 'clínica', 'clinica', 'médico', 'medico', 'dentista', 'laboratório', 'laboratorio', 'plano de saúde', 'unimed', 'amil', 'bradesco saúde'\n");
        prompt.append("   - Exemplos: 'FARMÁCIA XYZ', 'HOSPITAL ABC', 'CLÍNICA MÉDICA', 'UNIMED'\n\n");
        
        prompt.append("4. AUTOMOTIVO - Identifique por:\n");
        prompt.append("   - Palavras-chave: 'combustível', 'combustivel', 'gasolina', 'etanol', 'diesel', 'posto', 'shell', 'ipiranga', 'petrobras', 'ipva', 'seguro auto', 'oficina', 'mecânico', 'mecanico', 'estacionamento', 'parking', 'lavagem', 'auto peças'\n");
        prompt.append("   - Exemplos: 'POSTO SHELL', 'IPVA 2025', 'SEGURO AUTO', 'OFICINA MECÂNICA', 'ESTACIONAMENTO'\n\n");
        
        prompt.append("5. TRANSPORTE - Identifique por:\n");
        prompt.append("   - Palavras-chave: 'uber', '99', 'taxi', 'táxi', 'passagem', 'aéreo', 'aereo', 'avião', 'aviao', 'voo', 'flight', 'latam', 'gol', 'azul', 'tam', 'ônibus', 'onibus', 'metrô', 'metro', 'bilhete único'\n");
        prompt.append("   - Exemplos: 'UBER', '99 POP', 'LATAM PASSAGEM', 'GOL AÉREO', 'BILHETE ÚNICO'\n\n");
        
        prompt.append("6. EDUCAÇÃO - Identifique por:\n");
        prompt.append("   - Palavras-chave: 'escola', 'colégio', 'colegio', 'universidade', 'faculdade', 'curso', 'mensalidade', 'material escolar', 'livro', 'livraria', 'cursinho', 'preparatório'\n");
        prompt.append("   - Exemplos: 'ESCOLA XYZ', 'FACULDADE ABC', 'CURSO DE INGLÊS', 'LIVRARIA'\n\n");
        
        prompt.append("7. LAZER - Identifique por:\n");
        prompt.append("   - Palavras-chave: 'cinema', 'teatro', 'show', 'festival', 'parque', 'praia', 'viagem', 'travel', 'hotel', 'hospedagem', 'airbnb', 'booking', 'netflix', 'spotify', 'streaming', 'jogo', 'game', 'academia', 'ginástica'\n");
        prompt.append("   - Exemplos: 'CINEMA', 'AIRBNB HOSPEDAGEM', 'NETFLIX', 'ACADEMIA XYZ'\n\n");
        
        prompt.append("8. VESTUÁRIO - Identifique por:\n");
        prompt.append("   - Palavras-chave: 'roupa', 'vestuário', 'vestuario', 'moda', 'loja', 'shopping', 'calçado', 'calcado', 'sapato', 'sapataria', 'camiseta', 'calça', 'zara', 'renner', 'riachuelo', 'c&a'\n");
        prompt.append("   - Exemplos: 'LOJA DE ROUPAS', 'ZARA', 'SAPATARIA XYZ', 'SHOPPING CENTER'\n\n");
        
        prompt.append("9. TECNOLOGIA - Identifique por:\n");
        prompt.append("   - Palavras-chave: 'internet', 'banda larga', 'net', 'wi-fi', 'wifi', 'fibra', 'claro', 'vivo', 'oi', 'tim', 'gvt', 'virtua', 'celular', 'smartphone', 'computador', 'notebook', 'tablet', 'apple', 'samsung', 'magazine luiza', 'americanas', 'casas bahia'\n");
        prompt.append("   - Exemplos: 'CLARO INTERNET', 'VIVO FIBRA', 'APPLE STORE', 'MAGAZINE LUIZA'\n\n");
        
        prompt.append("10. SERVIÇOS - Identifique por:\n");
        prompt.append("    - Palavras-chave: 'diarista', 'faxina', 'limpeza', 'empregada', 'doméstica', 'domestica', 'housekeeping', 'cleaning', 'manutenção', 'manutencao', 'reparo', 'conserto', 'encanador', 'eletricista', 'pintor', 'pedreiro'\n");
        prompt.append("    - Exemplos: 'DIARISTA MARIA', 'FAXINA SEMANAL', 'MANUTENÇÃO PREDIAL', 'ELETRICISTA'\n\n");
        
        prompt.append("11. COMPRAS - Identifique por:\n");
        prompt.append("    - Palavras-chave: 'amazon', 'mercado livre', 'magazine luiza', 'americanas', 'casas bahia', 'extra', 'carrefour', 'walmart', 'compra online', 'e-commerce', 'marketplace', 'loja online'\n");
        prompt.append("    - Exemplos: 'AMAZON', 'MERCADO LIVRE', 'MAGAZINE LUIZA', 'AMERICANAS'\n\n");
        
        prompt.append("12. FINANCEIRO - Identifique por:\n");
        prompt.append("    - Palavras-chave: 'banco', 'tarifa', 'anuidade', 'juros', 'taxa', 'empréstimo', 'emprestimo', 'financiamento', 'cartão', 'cartao', 'fatura', 'boleto', 'pix', 'ted', 'doc', 'investimento', 'corretora'\n");
        prompt.append("    - Exemplos: 'TARIFA BANCÁRIA', 'ANUIDADE CARTÃO', 'EMPRÉSTIMO', 'CORRETORA XYZ'\n\n");
        
        prompt.append("13. PETS - Identifique por:\n");
        prompt.append("    - Palavras-chave: 'pet', 'pet shop', 'veterinário', 'veterinario', 'veterinária', 'veterinaria', 'ração', 'racao', 'animal', 'cachorro', 'gato', 'petz', 'cobasi'\n");
        prompt.append("    - Exemplos: 'PET SHOP XYZ', 'VETERINÁRIA ABC', 'PETZ', 'COBASI'\n\n");
        
        prompt.append("14. CUIDADOS PESSOAIS - Identifique por:\n");
        prompt.append("    - Palavras-chave: 'perfumaria', 'cosméticos', 'cosmeticos', 'maquiagem', 'creme', 'shampoo', 'condicionador', 'sabonete', 'desodorante', 'barbeiro', 'barbearia', 'salão', 'salao', 'beleza', 'estética', 'estetica', 'spa', 'massagem', 'manicure', 'pedicure', 'depilação', 'depilacao', 'cabeleireiro', 'corte de cabelo', 'tintura', 'alisamento', 'unhas', 'esmalte'\n");
        prompt.append("    - Exemplos: 'PERFUMARIA XYZ', 'SALÃO DE BELEZA', 'BARBEARIA ABC', 'SPA RELAXAMENTO', 'MANICURE E PEDICURE', 'CABELEIREIRO'\n\n");
        
        prompt.append("15. DELIVERY - Identifique por:\n");
        prompt.append("    - Palavras-chave: 'ifood', 'rappi', 'uber eats', 'delivery', 'entrega', 'pedido online', 'pedido delivery', 'app delivery', 'i food', 'rappi delivery', 'uber eats delivery', '99 food', 'pedidos já', 'pedidosja', 'aiquefome', 'ai que fome'\n");
        prompt.append("    - Exemplos: 'IFOOD', 'RAPPI', 'UBER EATS', 'DELIVERY RESTAURANTE', 'PEDIDO ONLINE'\n\n");
        
        prompt.append("16. OUTROS - Use para:\n");
        prompt.append("    - Qualquer transação que NÃO se encaixe claramente nos tipos acima\n");
        prompt.append("    - Quando houver dúvida sobre o tipo\n");
        prompt.append("    - Transações genéricas sem identificação clara\n\n");
        
        prompt.append("INSTRUÇÕES DE IDENTIFICAÇÃO:\n");
        prompt.append("- Analise SEMANTICAMENTE o nome/descrição da transação, não apenas palavras isoladas\n");
        prompt.append("- Considere o CONTEXTO e o SIGNIFICADO da transação\n");
        prompt.append("- Preste atenção em variações de escrita (com/sem acentos, maiúsculas/minúsculas)\n");
        prompt.append("- Identifique empresas e marcas conhecidas relacionadas a cada tipo\n");
        prompt.append("- Se uma transação menciona múltiplos tipos, escolha o mais relevante ou o primeiro mencionado\n");
        prompt.append("- Seja INTELIGENTE: 'POSTO IPIRANGA' = Carro, 'NET VIRTUA' = Internet, 'IFOOD' = Marmitas\n\n");
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
        prompt.append("REGRAS CRÍTICAS DE IDENTIFICAÇÃO:\n");
        prompt.append("1. ANÁLISE SEMÂNTICA: Analise o SIGNIFICADO e CONTEXTO da descrição, não apenas palavras isoladas\n");
        prompt.append("2. IDENTIFICAÇÃO INTELIGENTE: Use o conhecimento sobre empresas, marcas e padrões comuns\n");
        prompt.append("3. PRECISÃO: O expenseTypeName DEVE ser EXATAMENTE igual a um dos nomes disponíveis (case-sensitive, com acentos)\n");
        prompt.append("4. CONFIANÇA: Use 'high' quando tiver certeza, 'medium' quando provável, 'low' quando incerto\n");
        prompt.append("5. QUANDO EM DÚVIDA: Se não conseguir identificar claramente, SEMPRE use 'Outros'\n");
        prompt.append("6. DATAS: EXTRAIA a data COMPLETA (dia, mês e ano) de cada transação\n");
        prompt.append("7. VALORES: Valores devem ser positivos (já são despesas)\n");
        prompt.append("8. FORMATO: Retorne APENAS o JSON válido, sem texto adicional antes ou depois\n\n");
        
        prompt.append("EXEMPLOS DE IDENTIFICAÇÃO CORRETA:\n");
        prompt.append("- 'POSTO IPIRANGA' → Automotivo\n");
        prompt.append("- 'NET VIRTUA INTERNET' → Tecnologia\n");
        prompt.append("- 'IFOOD DELIVERY' → Alimentação\n");
        prompt.append("- 'CEMIG ENERGIA' → Moradia\n");
        prompt.append("- 'SANEPAR ÁGUA' → Moradia\n");
        prompt.append("- 'CARREFOUR SUPERMERCADO' → Alimentação\n");
        prompt.append("- 'AIRBNB HOSPEDAGEM' → Lazer\n");
        prompt.append("- 'CONDOMÍNIO EDIFÍCIO' → Moradia\n");
        prompt.append("- 'UBER' → Transporte\n");
        prompt.append("- 'FARMÁCIA XYZ' → Saúde\n");
        prompt.append("- 'PET SHOP ABC' → Pets\n\n");
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
                    List<ExtractExpenseType> expenseTypes = extractExpenseTypeRepository.findAll();
                    
                    // Se expenseTypeId não foi fornecido, tentar mapear pelo nome
                    if (transaction.getExpenseTypeId() == null && expenseTypeName != null) {
                        Optional<ExtractExpenseType> matchedType = expenseTypes.stream()
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
                                ExtractExpenseType matched = expenseTypes.stream()
                                    .filter(et -> et.getId().equals(mappedId))
                                    .findFirst()
                                    .orElse(null);
                                if (matched != null) {
                                    transaction.setExpenseTypeName(matched.getName());
                                }
                            } else {
                                // Se não conseguiu mapear, usar "Outros"
                                Optional<ExtractExpenseType> outrosType = expenseTypes.stream()
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
                        Optional<ExtractExpenseType> outrosType = expenseTypes.stream()
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
    
    private Long mapExpenseTypeByName(String description, List<ExtractExpenseType> expenseTypes) {
        if (description == null) return null;
        
        String descLower = description.toLowerCase()
                .replaceAll("[áàâãä]", "a")
                .replaceAll("[éèêë]", "e")
                .replaceAll("[íìîï]", "i")
                .replaceAll("[óòôõö]", "o")
                .replaceAll("[úùûü]", "u")
                .replaceAll("[ç]", "c");
        
        // Mapeamento por palavras-chave e empresas conhecidas
        for (ExtractExpenseType type : expenseTypes) {
            String typeName = type.getName().toLowerCase();
            
            switch (typeName) {
                case "alimentação":
                case "alimentacao":
                    if (containsAny(descLower, "supermercado", "mercado", "atacadao", "atacadão", "carrefour", "extra", "pao de acucar", "pao de açucar", 
                            "walmart", "big", "assai", "makro", "sams", "costco", "ifood", "rappi", "uber eats", "delivery", "marmita", 
                            "restaurante", "lanchonete", "padaria", "acougue", "açougue", "peixaria", "hortifruti", "comida")) {
                        return type.getId();
                    }
                    break;
                case "moradia":
                    if (containsAny(descLower, "aluguel", "rent", "locacao", "locação", "imobiliaria", "imobiliária", "rental", "lease",
                            "condominio", "condomínio", "sindico", "síndico", "taxa condominial", "condo",
                            "luz", "energia", "eletricidade", "electricity", "light", "eletrica",
                            "cemig", "copel", "enel", "ceb", "celesc", "celpe", "coelba", "elektro", "ampla", "amazonas energia",
                            "agua", "água", "water", "abastecimento", "saneamento", "sanepar", "sabesp", "caesb", "copasa", "caern", "cagece", "cesan", "cesp", "embasa", "caema",
                            "gas", "gás", "gas natural", "gás natural", "gn", "glp", "comgas", "comgás", "petrobras", "copergas", "copergás", "scgas", "scgás", "sulgas", "sulgás", "ceg", "cigas",
                            "iptu", "imposto predial", "imposto territorial", "taxa urbana", "taxa predial")) {
                        return type.getId();
                    }
                    break;
                case "saúde":
                case "saude":
                    if (containsAny(descLower, "farmacia", "farmácia", "drogaria", "medicamento", "remedio", "remédio", "hospital", "clinica", "clínica", 
                            "medico", "médico", "dentista", "laboratorio", "laboratório", "plano de saude", "plano de saúde", 
                            "unimed", "amil", "bradesco saude", "bradesco saúde")) {
                        return type.getId();
                    }
                    break;
                case "automotivo":
                    if (containsAny(descLower, "combustivel", "combustível", "gasolina", "etanol", "diesel", "posto", "shell", "ipiranga", "petrobras", 
                            "ipva", "seguro auto", "oficina", "mecanico", "mecânico", "estacionamento", "parking", "lavagem", "auto pecas", "auto peças", "texaco", "esso", "bp")) {
                        return type.getId();
                    }
                    break;
                case "transporte":
                    if (containsAny(descLower, "uber", "99", "taxi", "táxi", "passagem", "aereo", "aéreo", "aviao", "avião", "voo", "flight", 
                            "latam", "gol", "azul", "tam", "onibus", "ônibus", "metro", "metrô", "bilhete unico", "bilhete único")) {
                        return type.getId();
                    }
                    break;
                case "educação":
                case "educacao":
                    if (containsAny(descLower, "escola", "colegio", "colégio", "universidade", "faculdade", "curso", "mensalidade", "material escolar", 
                            "livro", "livraria", "cursinho", "preparatorio", "preparatório")) {
                        return type.getId();
                    }
                    break;
                case "lazer":
                    if (containsAny(descLower, "cinema", "teatro", "show", "festival", "parque", "praia", "viagem", "travel", "hotel", "hospedagem", 
                            "airbnb", "booking", "netflix", "spotify", "streaming", "jogo", "game", "academia", "ginastica", "ginástica")) {
                        return type.getId();
                    }
                    break;
                case "vestuário":
                case "vestuario":
                    if (containsAny(descLower, "roupa", "vestuario", "vestuário", "moda", "loja", "shopping", "calcado", "calçado", "sapato", "sapataria", 
                            "camiseta", "calca", "calça", "zara", "renner", "riachuelo", "c&a", "c e a")) {
                        return type.getId();
                    }
                    break;
                case "tecnologia":
                    if (containsAny(descLower, "internet", "banda larga", "net", "wi-fi", "wifi", "fibra", "claro", "vivo", "oi", "tim", "gvt", "virtua", 
                            "celular", "smartphone", "computador", "notebook", "tablet", "apple", "samsung", "magazine luiza", "americanas", "casas bahia")) {
                        return type.getId();
                    }
                    break;
                case "serviços":
                case "servicos":
                    if (containsAny(descLower, "diarista", "faxina", "limpeza", "empregada", "domestica", "doméstica", "housekeeping", "cleaning", 
                            "manutencao", "manutenção", "reparo", "conserto", "encanador", "eletricista", "pintor", "pedreiro")) {
                        return type.getId();
                    }
                    break;
                case "compras":
                    if (containsAny(descLower, "amazon", "mercado livre", "magazine luiza", "americanas", "casas bahia", "extra", "carrefour", "walmart", 
                            "compra online", "e-commerce", "ecommerce", "marketplace", "loja online")) {
                        return type.getId();
                    }
                    break;
                case "financeiro":
                    if (containsAny(descLower, "banco", "tarifa", "anuidade", "juros", "taxa", "emprestimo", "empréstimo", "financiamento", 
                            "cartao", "cartão", "fatura", "boleto", "pix", "ted", "doc", "investimento", "corretora")) {
                        return type.getId();
                    }
                    break;
                case "pets":
                    if (containsAny(descLower, "pet", "pet shop", "veterinario", "veterinário", "veterinaria", "veterinária", "racao", "ração", 
                            "animal", "cachorro", "gato", "petz", "cobasi")) {
                        return type.getId();
                    }
                    break;
                case "cuidados pessoais":
                    if (containsAny(descLower, "perfumaria", "cosmeticos", "cosméticos", "maquiagem", "creme", "shampoo", "condicionador", 
                            "sabonete", "desodorante", "barbeiro", "barbearia", "salao", "salão", "beleza", "estetica", "estética", 
                            "spa", "massagem", "manicure", "pedicure", "depilacao", "depilação", "cabeleireiro", "corte de cabelo", 
                            "tintura", "alisamento", "unhas", "esmalte")) {
                        return type.getId();
                    }
                    break;
                case "delivery":
                    if (containsAny(descLower, "ifood", "rappi", "uber eats", "delivery", "entrega", "pedido online", "pedido delivery", 
                            "app delivery", "i food", "rappi delivery", "uber eats delivery", "99 food", "pedidos ja", "pedidosja", 
                            "aiquefome", "ai que fome")) {
                        return type.getId();
                    }
                    break;
            }
        }
        
        // Se não encontrou nenhum match, retorna null (será mapeado para "Outros" depois)
        return null;
    }
    
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
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

