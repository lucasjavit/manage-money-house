package com.managehouse.money.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.managehouse.money.config.ChatModelFactory;
import com.managehouse.money.entity.ExpenseType;
import com.managehouse.money.repository.ExpenseTypeRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Extrai valor, descrição e (para gasto da casa) o tipo sugerido a partir do TEXTO CRU de uma
 * notificação de banco, usando a IA do provider ativo (Claude quando ai.provider=anthropic).
 * Quando a IA está indisponível ou falha, devolve um resultado "unavailable" — nunca propaga erro.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationExtractionService {

    private final ConfigurationService configurationService;
    private final ChatModelFactory chatModelFactory;
    private final ExpenseTypeRepository expenseTypeRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public record ExtractionResult(
            BigDecimal amount,
            String description,
            Long suggestedExpenseTypeId,
            boolean aiUsed) {

        static ExtractionResult unavailable() {
            return new ExtractionResult(null, null, null, false);
        }
    }

    public ExtractionResult extract(String rawText, boolean isHouse) {
        if (rawText == null || rawText.isBlank()) {
            return ExtractionResult.unavailable();
        }

        String apiKey = configurationService.getActiveProviderKey();
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Extração de notificação: nenhuma chave de IA configurada.");
            return ExtractionResult.unavailable();
        }

        try {
            List<ExpenseType> houseTypes = isHouse ? expenseTypeRepository.findAll() : List.of();
            ChatLanguageModel model = chatModelFactory.createChatModel(apiKey, 600, 30);
            if (model == null) {
                return ExtractionResult.unavailable();
            }

            String prompt = buildPrompt(rawText, isHouse, houseTypes);
            String response = model.generate(prompt);
            return parse(response, isHouse, houseTypes);
        } catch (Exception e) {
            log.error("Falha na extração por IA: {}", e.getMessage());
            return ExtractionResult.unavailable();
        }
    }

    private String buildPrompt(String rawText, boolean isHouse, List<ExpenseType> houseTypes) {
        StringBuilder sb = new StringBuilder();
        sb.append("Você extrai dados de uma notificação de banco brasileira. ");
        sb.append("Responda APENAS com um JSON, sem texto extra e sem cercas de código.\n\n");
        sb.append("Texto da notificação:\n\"\"\"\n").append(rawText).append("\n\"\"\"\n\n");
        sb.append("Regras:\n");
        sb.append("- amount: o valor gasto como número decimal (ex.: \"R$ 1.234,56\" vira 1234.56). ");
        sb.append("Se não houver valor claro, use null.\n");
        sb.append("- description: o estabelecimento ou descrição da compra (curto). Se não houver, null.\n");
        if (isHouse) {
            String types = houseTypes.stream()
                    .map(t -> t.getId() + ":" + t.getName())
                    .collect(Collectors.joining(", "));
            sb.append("- expenseTypeId: escolha o id do tipo mais adequado entre estes (id:nome): ")
              .append(types).append(". Se nenhum servir, null.\n");
            sb.append("\nFormato: {\"amount\": number|null, \"description\": string|null, \"expenseTypeId\": number|null}\n");
        } else {
            sb.append("\nFormato: {\"amount\": number|null, \"description\": string|null}\n");
        }
        return sb.toString();
    }

    private ExtractionResult parse(String response, boolean isHouse, List<ExpenseType> houseTypes) {
        try {
            String json = cleanJson(response);
            JsonNode node = objectMapper.readTree(json);

            BigDecimal amount = parseAmount(node.get("amount"));
            String description = textOrNull(node.get("description"));

            Long typeId = null;
            if (isHouse && node.hasNonNull("expenseTypeId")) {
                long candidate = node.get("expenseTypeId").asLong();
                // Só aceita se for um dos tipos reais da casa.
                boolean valid = houseTypes.stream().anyMatch(t -> t.getId() == candidate);
                if (valid) {
                    typeId = candidate;
                }
            }
            return new ExtractionResult(amount, description, typeId, true);
        } catch (Exception e) {
            log.error("Falha ao parsear JSON da IA: {}", e.getMessage());
            return ExtractionResult.unavailable();
        }
    }

    /** Remove cercas ```json e recorta do primeiro { ao último } para tolerar texto extra. */
    private String cleanJson(String raw) {
        String s = raw.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return s.substring(start, end + 1);
        }
        return s;
    }

    private BigDecimal parseAmount(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            BigDecimal v = node.decimalValue();
            return v.signum() > 0 ? v : null;
        }
        // Fallback: veio como string tipo "R$ 1.234,56"
        return parseBrAmount(node.asText());
    }

    private BigDecimal parseBrAmount(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String cleaned = text.replaceAll("[R$\\s]", "");
        if (cleaned.contains(",")) {
            // Formato PT-BR: ponto é milhar, vírgula é decimal.
            cleaned = cleaned.replace(".", "").replace(",", ".");
        }
        try {
            BigDecimal v = new BigDecimal(cleaned);
            return v.signum() > 0 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String t = node.asText();
        return (t == null || t.isBlank()) ? null : t;
    }
}
