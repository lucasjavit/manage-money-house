package com.managehouse.money.controller;

import com.managehouse.money.dto.B3ReportUploadRequest;
import com.managehouse.money.dto.B3ReportUploadResponse;
import com.managehouse.money.dto.RealPortfolioSummaryDTO;
import com.managehouse.money.entity.UserRealPortfolio;
import com.managehouse.money.service.B3ReportService;
import com.managehouse.money.service.RealPortfolioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/real-portfolio")
@RequiredArgsConstructor
@Slf4j
public class RealPortfolioController {

    private final B3ReportService b3ReportService;
    private final RealPortfolioService realPortfolioService;

    @PostMapping("/upload")
    public ResponseEntity<B3ReportUploadResponse> uploadReport(
            @RequestBody B3ReportUploadRequest request) {
        log.info("POST /api/real-portfolio/upload - UserId: {}, FileName: {}",
                request.getUserId(), request.getFileName());

        // 1. Processar arquivo (PDF ou Excel) e extrair dados com IA
        B3ReportUploadResponse response = b3ReportService.processReport(
                request.getFileContent(), request.getUserId(), request.getFileName());

        // Se houve erro na extracao, retornar
        if (response.getErrorMessage() != null) {
            log.error("Erro na extracao: {}", response.getErrorMessage());
            return ResponseEntity.badRequest().body(response);
        }

        // 2. Salvar no banco de dados
        UserRealPortfolio savedPortfolio = realPortfolioService.savePortfolio(
                request.getUserId(), response);

        // 3. Gerar analise de IA automaticamente
        String analysis = realPortfolioService.generateAIAnalysis(request.getUserId());
        response.setAiAnalysis(analysis);

        log.info("Carteira processada e salva com sucesso. ID: {}", savedPortfolio.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<RealPortfolioSummaryDTO> getLatestPortfolio(
            @PathVariable Long userId) {
        log.info("GET /api/real-portfolio/{}", userId);

        return realPortfolioService.getLatestPortfolio(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{userId}/history")
    public ResponseEntity<List<RealPortfolioSummaryDTO>> getPortfolioHistory(
            @PathVariable Long userId) {
        log.info("GET /api/real-portfolio/{}/history", userId);

        List<RealPortfolioSummaryDTO> portfolios = realPortfolioService.getAllPortfolios(userId);
        return ResponseEntity.ok(portfolios);
    }

    @GetMapping("/{userId}/{year}/{month}")
    public ResponseEntity<RealPortfolioSummaryDTO> getPortfolioByMonthYear(
            @PathVariable Long userId,
            @PathVariable Integer year,
            @PathVariable Integer month) {
        log.info("GET /api/real-portfolio/{}/{}/{}", userId, year, month);

        return realPortfolioService.getPortfolioByMonthYear(userId, month, year)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{userId}/analyze")
    public ResponseEntity<Map<String, String>> analyzePortfolio(
            @PathVariable Long userId) {
        log.info("POST /api/real-portfolio/{}/analyze", userId);

        String analysis = realPortfolioService.generateAIAnalysis(userId);
        return ResponseEntity.ok(Map.of("analysis", analysis));
    }

    @PostMapping("/{userId}/analyze-assets")
    public ResponseEntity<Map<String, Object>> analyzeIndividualAssets(
            @PathVariable Long userId) {
        log.info("POST /api/real-portfolio/{}/analyze-assets", userId);

        realPortfolioService.generateIndividualAssetAnalyses(userId);

        return ResponseEntity.ok(Map.of(
                "message", "Analises individuais geradas com sucesso",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}
