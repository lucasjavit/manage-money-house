package com.managehouse.money.controller;

import com.managehouse.money.dto.IngestResponse;
import com.managehouse.money.dto.IngestTransactionRequest;
import com.managehouse.money.service.IngestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Recebe transações do app Android. Protegido pelo IngestTokenFilter (header X-Ingest-Token).
 */
@RestController
@RequestMapping("/api/ingest")
@RequiredArgsConstructor
public class IngestController {

    private final IngestService ingestService;

    @PostMapping
    public ResponseEntity<IngestResponse> ingest(@RequestBody IngestTransactionRequest request) {
        return ResponseEntity.ok(ingestService.ingest(request));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidation(IllegalArgumentException e) {
        return Map.of("error", e.getMessage());
    }
}
