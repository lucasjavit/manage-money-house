package com.managehouse.money.controller;

import com.managehouse.money.dto.*;
import com.managehouse.money.service.CompanyExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/company-expenses")
@RequiredArgsConstructor
public class CompanyExpenseController {

    private final CompanyExpenseService service;

    // Categorias (gerenciadas pelo usuário, alimentam o select)
    @PostMapping("/categories")
    public ResponseEntity<CompanyCategoryResponse> createCategory(@RequestBody CompanyCategoryRequest request) {
        return ResponseEntity.ok(service.createCategory(request));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CompanyCategoryResponse>> getCategories(@RequestParam Long userId) {
        return ResponseEntity.ok(service.getCategories(userId));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        service.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    // Lançamentos
    @PostMapping
    public ResponseEntity<CompanyExpenseResponse> create(@RequestBody CompanyExpenseRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CompanyExpenseResponse> update(@PathVariable Long id,
                                                         @RequestBody CompanyExpenseRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @GetMapping
    public ResponseEntity<List<CompanyExpenseResponse>> getByMonth(
            @RequestParam Long userId,
            @RequestParam Integer year,
            @RequestParam Integer month) {
        return ResponseEntity.ok(service.getByMonth(userId, year, month));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handle(IllegalArgumentException e) {
        return Map.of("error", e.getMessage());
    }
}
