package com.managehouse.money.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatModelFactory {

    @Value("${openai.model:gpt-4o-mini}")
    private String openaiModel;

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
     * @param apiKey A chave da API do OpenAI
     * @param maxTokens Limite maximo de tokens na resposta
     * @param timeoutSeconds Timeout em segundos
     * @return ChatLanguageModel configurado, ou null se apiKey for inválida
     */
    public ChatLanguageModel createChatModel(String apiKey, int maxTokens, int timeoutSeconds) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.isBlank()) {
            log.warn("API key is null or empty, cannot create ChatLanguageModel");
            return null;
        }

        try {
            log.info("Creating ChatLanguageModel with model: {}, maxTokens: {}, timeout: {}s",
                    openaiModel, maxTokens, timeoutSeconds);
            return OpenAiChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(openaiModel)
                    .temperature(0.7)
                    .maxTokens(maxTokens)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .build();
        } catch (Exception e) {
            log.error("Error creating ChatLanguageModel", e);
            return null;
        }
    }

    /**
     * Cria uma instância de ChatLanguageModel otimizada para extracao de dados estruturados.
     * Usa maxTokens alto (16000) e timeout maior (120s) para processar PDFs grandes.
     *
     * @param apiKey A chave da API do OpenAI
     * @return ChatLanguageModel configurado para extracao, ou null se apiKey for inválida
     */
    public ChatLanguageModel createExtractionModel(String apiKey) {
        return createChatModel(apiKey, 16000, 120);
    }
}
