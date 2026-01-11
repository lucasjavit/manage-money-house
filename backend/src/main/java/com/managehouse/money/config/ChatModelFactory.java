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
        if (apiKey == null || apiKey.isEmpty() || apiKey.isBlank()) {
            log.warn("API key is null or empty, cannot create ChatLanguageModel");
            return null;
        }

        try {
            log.info("Creating ChatLanguageModel with model: {}", openaiModel);
            return OpenAiChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(openaiModel)
                    .temperature(0.7)
                    .maxTokens(1500)
                    .timeout(Duration.ofSeconds(30))
                    .build();
        } catch (Exception e) {
            log.error("Error creating ChatLanguageModel", e);
            return null;
        }
    }
}
