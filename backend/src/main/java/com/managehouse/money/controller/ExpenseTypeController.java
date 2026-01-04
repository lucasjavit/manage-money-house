package com.managehouse.money.controller;

import com.managehouse.money.dto.ExpenseTypeResponse;
import com.managehouse.money.service.ExpenseTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}

