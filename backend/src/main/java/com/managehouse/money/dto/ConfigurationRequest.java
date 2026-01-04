package com.managehouse.money.dto;

import lombok.Data;

@Data
public class ConfigurationRequest {
    private String key;
    private String value;
    private String description;
}

