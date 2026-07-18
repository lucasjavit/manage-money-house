package com.managehouse.money.service;

import com.managehouse.money.dto.ConfigurationRequest;
import com.managehouse.money.dto.ConfigurationResponse;
import com.managehouse.money.entity.Configuration;
import com.managehouse.money.repository.ConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConfigurationService {
    private final ConfigurationRepository configurationRepository;

    public ConfigurationResponse getConfiguration(String key) {
        return configurationRepository.findByKey(key)
                .map(this::toResponse)
                .orElse(null);
    }

    public List<ConfigurationResponse> getAllConfigurations() {
        return configurationRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ConfigurationResponse saveOrUpdateConfiguration(ConfigurationRequest request) {
        Configuration config = configurationRepository.findByKey(request.getKey())
                .orElse(new Configuration());

        config.setKey(request.getKey());
        config.setValue(request.getValue());
        if (request.getDescription() != null) {
            config.setDescription(request.getDescription());
        }

        Configuration saved = configurationRepository.save(config);
        return toResponse(saved);
    }

    @Transactional
    public void deleteConfiguration(String key) {
        configurationRepository.findByKey(key)
                .ifPresent(configurationRepository::delete);
    }

    /**
     * Chave do OpenAI. Continua sendo a chave do OpenAI mesmo com o provider em
     * "anthropic": os serviços que falam o formato da OpenAI via WebClient (extrato,
     * boleto, salário, insights de despesa) seguem no OpenAI.
     */
    public String getOpenAIKey() {
        return getValue("openai.api.key");
    }

    /**
     * Chave do provider de IA ativo, para os serviços que passam pelo ChatModelFactory.
     * Eles checam a chave antes de pedir o modelo, então com "anthropic" precisam receber
     * a chave da Anthropic — senão o guard barra a feature antes do factory escolher.
     */
    public String getActiveProviderKey() {
        return "anthropic".equalsIgnoreCase(getAIProvider())
                ? getAnthropicKey()
                : getValue("openai.api.key");
    }

    public String getAnthropicKey() {
        return getValue("anthropic.api.key");
    }

    /** Token compartilhado que protege a rota /api/ingest (app Android). */
    public String getIngestToken() {
        return getValue("ingest.token");
    }

    /** "openai" (padrão) ou "anthropic": qual provider de IA a aplicação usa. */
    public String getAIProvider() {
        String provider = getValue("ai.provider");
        return (provider == null || provider.isBlank()) ? "openai" : provider.trim();
    }

    private String getValue(String key) {
        return configurationRepository.findByKey(key)
                .map(Configuration::getValue)
                .orElse(null);
    }

    private ConfigurationResponse toResponse(Configuration config) {
        return new ConfigurationResponse(
                config.getId(),
                config.getKey(),
                config.getValue(),
                config.getDescription()
        );
    }
}

