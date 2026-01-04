package com.managehouse.money.controller;

import com.managehouse.money.dto.AnnualSalaryCalculationResponse;
import com.managehouse.money.dto.SalaryCalculationRequest;
import com.managehouse.money.dto.SalaryCalculationResponse;
import com.managehouse.money.dto.SalaryRequest;
import com.managehouse.money.dto.SalaryResponse;
import com.managehouse.money.service.SalaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/salaries")
@RequiredArgsConstructor
@Slf4j
public class SalaryController {
    private final SalaryService salaryService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<SalaryResponse> getSalaryByUser(@PathVariable Long userId) {
        log.info("GET /api/salaries/user/{}", userId);
        SalaryResponse salary = salaryService.getSalaryByUser(userId);
        return salary != null ? ResponseEntity.ok(salary) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<SalaryResponse> createOrUpdateSalary(@RequestBody SalaryRequest request) {
        log.info("POST /api/salaries - UserId: {}", request.getUserId());
        SalaryResponse response = salaryService.createOrUpdateSalary(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/calculate")
    public ResponseEntity<SalaryCalculationResponse> calculateVariableSalary(@RequestBody SalaryCalculationRequest request) {
        SalaryCalculationResponse response = salaryService.calculateVariableSalary(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/calculate/annual")
    public ResponseEntity<AnnualSalaryCalculationResponse> calculateAnnualSalary(
            @RequestParam Long userId,
            @RequestParam Integer year) {
        AnnualSalaryCalculationResponse response = salaryService.calculateAnnualSalary(userId, year);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSalary(@PathVariable Long id) {
        salaryService.deleteSalary(id);
        return ResponseEntity.noContent().build();
    }
}

