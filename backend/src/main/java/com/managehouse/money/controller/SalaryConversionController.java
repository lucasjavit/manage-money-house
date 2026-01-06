package com.managehouse.money.controller;

import com.managehouse.money.dto.*;
import com.managehouse.money.service.SalaryConversionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/salary-conversions")
@RequiredArgsConstructor
public class SalaryConversionController {
    private final SalaryConversionService salaryConversionService;

    @PostMapping("/process")
    public ResponseEntity<SalaryConversionProcessResponse> processConversionText(
            @RequestBody SalaryConversionProcessRequest request) {
        SalaryConversionProcessResponse response = salaryConversionService.processConversionText(request.getText());
        if (response.getError() != null) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<SalaryConversionResponse> createOrUpdateConversion(
            @RequestBody SalaryConversionRequest request) {
        SalaryConversionResponse response = salaryConversionService.createOrUpdateConversion(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<SalaryConversionResponse> getConversion(
            @RequestParam Long userId,
            @RequestParam Integer month,
            @RequestParam Integer year) {
        SalaryConversionResponse response = salaryConversionService.getConversionByMonthAndYear(userId, month, year);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }
}

