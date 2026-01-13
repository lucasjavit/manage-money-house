package com.managehouse.money.controller;

import com.managehouse.money.entity.PortfolioAnalysis;
import com.managehouse.money.service.PortfolioReviewScheduler;
import com.managehouse.money.service.PortfolioReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/portfolio-review")
@RequiredArgsConstructor
@Slf4j
public class PortfolioReviewController {

    private final PortfolioReviewScheduler reviewScheduler;
    private final PortfolioReviewService reviewService;

    /**
     * Executar análise de TODAS as carteiras manualmente
     */
    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> runFullReview() {
        log.info("POST /api/portfolio-review/run - Iniciando análise manual completa");

        if (reviewScheduler.isRunning()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Uma análise já está em execução. Aguarde a conclusão."
            ));
        }

        List<PortfolioAnalysis> results = reviewScheduler.runManualReview();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", String.format("Análise concluída: %d ativos analisados", results.size()));
        response.put("results", results);
        response.put("substitutions", results.stream()
                .filter(a -> "SUBSTITUIR".equals(a.getRecommendation()))
                .count());

        return ResponseEntity.ok(response);
    }

    /**
     * Executar análise de uma carteira específica
     */
    @PostMapping("/run/{portfolioName}")
    public ResponseEntity<Map<String, Object>> runPortfolioReview(@PathVariable String portfolioName) {
        log.info("POST /api/portfolio-review/run/{} - Iniciando análise da carteira", portfolioName);

        if (reviewScheduler.isRunning()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Uma análise já está em execução. Aguarde a conclusão."
            ));
        }

        List<PortfolioAnalysis> results = reviewScheduler.runPortfolioReview(portfolioName);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("portfolioName", portfolioName);
        response.put("message", String.format("Análise concluída: %d ativos analisados", results.size()));
        response.put("results", results);

        return ResponseEntity.ok(response);
    }

    /**
     * Executar análise de um ativo específico
     */
    @PostMapping("/run/{portfolioName}/{ticker}")
    public ResponseEntity<Map<String, Object>> runAssetReview(
            @PathVariable String portfolioName,
            @PathVariable String ticker) {
        log.info("POST /api/portfolio-review/run/{}/{} - Iniciando análise do ativo", portfolioName, ticker);

        PortfolioAnalysis result = reviewScheduler.runAssetReview(ticker, portfolioName);

        if (result == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("result", result);

        return ResponseEntity.ok(response);
    }

    /**
     * Obter status da execução atual
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("isRunning", reviewScheduler.isRunning());
        status.put("lastRunTime", reviewScheduler.getLastRunTime());
        status.put("lastRunStatus", reviewScheduler.getLastRunStatus());

        return ResponseEntity.ok(status);
    }

    /**
     * Obter todas as análises ativas
     */
    @GetMapping("/analyses")
    public ResponseEntity<List<PortfolioAnalysis>> getAllActiveAnalyses() {
        log.info("GET /api/portfolio-review/analyses");
        return ResponseEntity.ok(reviewService.getPendingReviews().isEmpty()
                ? reviewService.getSubstitutionRecommendations()
                : reviewService.getPendingReviews());
    }

    /**
     * Obter análises de uma carteira específica
     */
    @GetMapping("/analyses/{portfolioName}")
    public ResponseEntity<List<PortfolioAnalysis>> getPortfolioAnalyses(@PathVariable String portfolioName) {
        log.info("GET /api/portfolio-review/analyses/{}", portfolioName);
        return ResponseEntity.ok(reviewService.getPortfolioAnalyses(portfolioName));
    }

    /**
     * Obter última análise de um ticker
     */
    @GetMapping("/analyses/ticker/{ticker}")
    public ResponseEntity<PortfolioAnalysis> getTickerAnalysis(@PathVariable String ticker) {
        log.info("GET /api/portfolio-review/analyses/ticker/{}", ticker);
        return reviewService.getLatestAnalysis(ticker)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Obter histórico de análises de um ticker
     */
    @GetMapping("/history/{ticker}")
    public ResponseEntity<List<PortfolioAnalysis>> getTickerHistory(@PathVariable String ticker) {
        log.info("GET /api/portfolio-review/history/{}", ticker);
        return ResponseEntity.ok(reviewService.getAnalysisHistory(ticker));
    }

    /**
     * Obter ativos recomendados para substituição
     */
    @GetMapping("/substitutions")
    public ResponseEntity<List<PortfolioAnalysis>> getSubstitutionRecommendations() {
        log.info("GET /api/portfolio-review/substitutions");
        return ResponseEntity.ok(reviewService.getSubstitutionRecommendations());
    }
}
