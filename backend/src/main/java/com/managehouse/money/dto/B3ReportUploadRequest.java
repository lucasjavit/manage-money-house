package com.managehouse.money.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class B3ReportUploadRequest {
    private Long userId;
    private String fileName;
    private String fileContent; // Base64 encoded PDF
}
