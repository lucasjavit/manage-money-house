package com.managehouse.money.controller;

import com.managehouse.money.dto.ExpenseTypeRequest;
import com.managehouse.money.dto.ExpenseTypeResponse;
import com.managehouse.money.service.ExpenseTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expense-types")
@RequiredArgsConstructor
public class ExpenseTypeController {
    private final ExpenseTypeService expenseTypeService;

    @GetMapping
    public ResponseEntity<List<ExpenseTypeResponse>> getAll() {
        return ResponseEntity.ok(expenseTypeService.getAll());
    }

    @PostMapping
    public ResponseEntity<ExpenseTypeResponse> create(@RequestBody ExpenseTypeRequest request) {
        ExpenseTypeResponse response = expenseTypeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            expenseTypeService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(java.util.Map.of("error", e.getMessage()));
        }
    }
}

