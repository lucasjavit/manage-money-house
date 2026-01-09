package com.managehouse.money.controller;

import com.managehouse.money.dto.ExtractExpenseTypeResponse;
import com.managehouse.money.entity.ExtractExpenseType;
import com.managehouse.money.service.ExtractExpenseTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/extract-expense-types")
@RequiredArgsConstructor
public class ExtractExpenseTypeController {
    private final ExtractExpenseTypeService extractExpenseTypeService;

    @GetMapping
    public ResponseEntity<List<ExtractExpenseTypeResponse>> getAll() {
        System.out.println("ExtractExpenseTypeController.getAll() - Endpoint chamado!");
        List<ExtractExpenseType> allTypes = extractExpenseTypeService.getAll();
        System.out.println("ExtractExpenseTypeController.getAll() - Total de tipos encontrados: " + allTypes.size());
        List<ExtractExpenseTypeResponse> types = allTypes.stream()
                .map(et -> new ExtractExpenseTypeResponse(et.getId(), et.getName()))
                .collect(Collectors.toList());
        System.out.println("ExtractExpenseTypeController.getAll() - Retornando " + types.size() + " tipos");
        return ResponseEntity.ok(types);
    }
}

