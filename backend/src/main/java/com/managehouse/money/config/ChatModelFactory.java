package com.managehouse.money.config;

import com.managehouse.money.service.ConfigurationService;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatModelFactory {

    public static final String PROVIDER_ANTHROPIC = "anthropic";

    @Lazy
    private final ConfigurationService configurationService;

    @Value("${openai.model:gpt-4o-mini}")
    private String openaiModel;

    @Value("${anthropic.model:claude-opus-4-8}")
    private String anthropicModel;

    /**
     * Cria uma instância de ChatLanguageModel com a API key fornecida.
     * Chamado dinamicamente pelos serviços que precisam usar IA.
     *
     * @param apiKey A chave da API do OpenAI (obtida do banco via ConfigurationService)
     * @return ChatLanguageModel configurado, ou null se apiKey for inválida
     */
    public ChatLanguageModel createChatModel(String apiKey) {
        return createChatModel(apiKey, 2000, 30);
    }

    /**
     * Cria uma instância de ChatLanguageModel para tarefas que requerem respostas longas.
     * Use este metodo para extracao de dados de PDFs, analises complexas, etc.
     *
     * O provider vem da configuracao "ai.provider" no banco: com "anthropic" usa a chave
     * "anthropic.api.key" e ignora a apiKey recebida (que é a do OpenAI).
     *
     * @param apiKey A chave da API do OpenAI
     * @param maxTokens Limite maximo de tokens na resposta
     * @param timeoutSeconds Timeout em segundos
     * @return ChatLanguageModel configurado, ou null se a chave do provider for inválida
     */
    public ChatLanguageModel createChatModel(String apiKey, int maxTokens, int timeoutSeconds) {
        String provider = configurationService.getAIProvider();

        if (PROVIDER_ANTHROPIC.equalsIgnoreCase(provider)) {
            return createAnthropicModel(maxTokens, timeoutSeconds);
        }

        if (isBlank(apiKey)) {
            log.warn("API key is null or empty, cannot create ChatLanguageModel");
            return null;
        }

        try {
            log.info("Creating OpenAI ChatLanguageModel with model: {}, maxTokens: {}, timeout: {}s",
                    openaiModel, maxTokens, timeoutSeconds);
            return OpenAiChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(openaiModel)
                    .temperature(0.7)
                    .maxTokens(maxTokens)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .build();
        } catch (Exception e) {
            log.error("Error creating OpenAI ChatLanguageModel", e);
            return null;
        }
    }

    private ChatLanguageModel createAnthropicModel(int maxTokens, int timeoutSeconds) {
        String anthropicKey = configurationService.getAnthropicKey();
        if (isBlank(anthropicKey)) {
            log.warn("Anthropic API key is null or empty, cannot create ChatLanguageModel");
            return null;
        }

        try {
            log.info("Creating Anthropic ChatLanguageModel with model: {}, maxTokens: {}, timeout: {}s",
                    anthropicModel, maxTokens, timeoutSeconds);
            return AnthropicChatModel.builder()
                    .apiKey(anthropicKey)
                    .modelName(anthropicModel)
                    .maxTokens(maxTokens)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .build();
        } catch (Exception e) {
            log.error("Error creating Anthropic ChatLanguageModel", e);
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * Cria uma instância de ChatLanguageModel otimizada para extracao de dados estruturados.
     * Usa maxTokens alto (16000) e timeout maior (180s) para processar PDFs/Excel grandes.
     *
     * @param apiKey A chave da API do OpenAI
     * @return ChatLanguageModel configurado para extracao, ou null se apiKey for inválida
     */
    public ChatLanguageModel createExtractionModel(String apiKey) {
        return createChatModel(apiKey, 16000, 180);
    }
}
