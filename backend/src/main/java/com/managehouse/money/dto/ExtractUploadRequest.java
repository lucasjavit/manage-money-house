package com.managehouse.money.dto;

import lombok.Data;

@Data
public class ExtractUploadRequest {
    private String fileName;
    private String fileContent; // Base64 encoded
    private String fileType; // "pdf" or "png"
}

