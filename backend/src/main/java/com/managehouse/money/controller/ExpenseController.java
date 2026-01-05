package com.managehouse.money.controller;

import com.managehouse.money.dto.ExpenseRequest;
import com.managehouse.money.dto.ExpenseResponse;
import com.managehouse.money.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {
    private final ExpenseService expenseService;

    @GetMapping
    public ResponseEntity<List<ExpenseResponse>> getExpenses(
            @RequestParam(required = false, defaultValue = "2024") Integer year) {
        return ResponseEntity.ok(expenseService.getExpensesByYear(year));
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> createOrUpdateExpense(@RequestBody ExpenseRequest request) {
        return ResponseEntity.ok(expenseService.createOrUpdateExpense(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        expenseService.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/lucas-debt")
    public ResponseEntity<java.math.BigDecimal> getLucasDebt(
            @RequestParam Integer year,
            @RequestParam Integer month) {
        return ResponseEntity.ok(expenseService.calculateLucasDebt(year, month));
    }
}

