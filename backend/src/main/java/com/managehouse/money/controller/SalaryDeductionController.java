package com.managehouse.money.controller;

import com.managehouse.money.dto.BoletoProcessResponse;
import com.managehouse.money.dto.ExtractUploadRequest;
import com.managehouse.money.dto.SalaryDeductionRequest;
import com.managehouse.money.dto.SalaryDeductionResponse;
import com.managehouse.money.service.BoletoService;
import com.managehouse.money.service.SalaryDeductionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/salary-deductions")
@RequiredArgsConstructor
@Slf4j
public class SalaryDeductionController {
    private final BoletoService boletoService;
    private final SalaryDeductionService salaryDeductionService;

    @PostMapping("/process")
    public ResponseEntity<BoletoProcessResponse> processBoleto(@RequestBody ExtractUploadRequest request) {
        BoletoProcessResponse response = boletoService.processBoleto(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<SalaryDeductionResponse> createDeduction(@RequestBody SalaryDeductionRequest request) {
        SalaryDeductionResponse response = salaryDeductionService.createDeduction(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<SalaryDeductionResponse>> getDeductions(
            @RequestParam Long userId,
            @RequestParam Integer month,
            @RequestParam Integer year) {
        log.info("GET /api/salary-deductions - UserId: {}, Month: {}, Year: {}", userId, month, year);
        List<SalaryDeductionResponse> deductions = salaryDeductionService.getDeductionsByMonthAndYear(userId, month, year);
        log.info("Found {} deductions", deductions.size());
        return ResponseEntity.ok(deductions);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDeduction(@PathVariable Long id) {
        salaryDeductionService.deleteDeduction(id);
        return ResponseEntity.noContent().build();
    }
}

