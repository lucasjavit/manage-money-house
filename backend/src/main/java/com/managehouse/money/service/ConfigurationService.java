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

    public String getOpenAIKey() {
        return configurationRepository.findByKey("openai.api.key")
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

