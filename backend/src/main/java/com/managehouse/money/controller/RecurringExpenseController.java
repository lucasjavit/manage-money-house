package com.managehouse.money.controller;

import com.managehouse.money.dto.RecurringExpenseRequest;
import com.managehouse.money.dto.RecurringExpenseResponse;
import com.managehouse.money.service.RecurringExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recurring-expenses")
@RequiredArgsConstructor
public class RecurringExpenseController {
    private final RecurringExpenseService recurringExpenseService;

    @PostMapping
    public ResponseEntity<RecurringExpenseResponse> createRecurringExpense(
            @RequestBody RecurringExpenseRequest request) {
        RecurringExpenseResponse response = recurringExpenseService.createRecurringExpense(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecurringExpenseResponse> updateRecurringExpense(
            @PathVariable Long id,
            @RequestBody RecurringExpenseRequest request) {
        RecurringExpenseResponse response = recurringExpenseService.updateRecurringExpense(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecurringExpense(@PathVariable Long id) {
        recurringExpenseService.deleteRecurringExpense(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<RecurringExpenseResponse>> getAllRecurringExpenses() {
        List<RecurringExpenseResponse> expenses = recurringExpenseService.getAllRecurringExpenses();
        return ResponseEntity.ok(expenses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecurringExpenseResponse> getRecurringExpenseById(@PathVariable Long id) {
        RecurringExpenseResponse expense = recurringExpenseService.getRecurringExpenseById(id);
        return ResponseEntity.ok(expense);
    }
}

