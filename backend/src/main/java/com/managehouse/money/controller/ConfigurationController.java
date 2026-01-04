package com.managehouse.money.controller;

import com.managehouse.money.dto.ConfigurationRequest;
import com.managehouse.money.dto.ConfigurationResponse;
import com.managehouse.money.service.ConfigurationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/configurations")
@RequiredArgsConstructor
public class ConfigurationController {
    private final ConfigurationService configurationService;

    @GetMapping
    public ResponseEntity<List<ConfigurationResponse>> getAllConfigurations() {
        return ResponseEntity.ok(configurationService.getAllConfigurations());
    }

    @GetMapping("/{key}")
    public ResponseEntity<ConfigurationResponse> getConfiguration(@PathVariable String key) {
        ConfigurationResponse config = configurationService.getConfiguration(key);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(config);
    }

    @PostMapping
    public ResponseEntity<ConfigurationResponse> saveOrUpdateConfiguration(
            @RequestBody ConfigurationRequest request) {
        ConfigurationResponse response = configurationService.saveOrUpdateConfiguration(request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> deleteConfiguration(@PathVariable String key) {
        configurationService.deleteConfiguration(key);
        return ResponseEntity.noContent().build();
    }
}

