package com.managehouse.money.controller;

import com.managehouse.money.dto.AIMonthlyAnalysisResponse;
import com.managehouse.money.entity.Expense;
import com.managehouse.money.repository.ExpenseRepository;
import com.managehouse.money.service.AIInsightsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIAnalysisController {

    private final AIInsightsService aiInsightsService;
    private final ExpenseRepository expenseRepository;

    /**
     * Endpoint para gerar análise completa sob demanda
     */
    @GetMapping("/analyze")
    public ResponseEntity<AIMonthlyAnalysisResponse> analyzeMonth(
            @RequestParam Long userId,
            @RequestParam Integer month,
            @RequestParam Integer year) {

        AIMonthlyAnalysisResponse analysis = aiInsightsService.generateMonthlyAnalysis(userId, month, year);
        return ResponseEntity.ok(analysis);
    }

    /**
     * Endpoint para detectar padrões
     */
    @GetMapping("/patterns")
    public ResponseEntity<List<AIMonthlyAnalysisResponse.Pattern>> detectPatterns(
            @RequestParam Long userId,
            @RequestParam Integer month,
            @RequestParam Integer year) {

        List<Expense> expenses = expenseRepository.findByUserIdAndMonthAndYear(userId, month, year);
        List<AIMonthlyAnalysisResponse.Pattern> patterns = aiInsightsService.detectSpendingPatterns(expenses);
        return ResponseEntity.ok(patterns);
    }
}
