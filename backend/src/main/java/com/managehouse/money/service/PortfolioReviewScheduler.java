package com.managehouse.money.service;

import com.managehouse.money.dto.InvestmentPortfolio;
import com.managehouse.money.dto.RecommendedAsset;
import com.managehouse.money.entity.PortfolioAnalysis;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PortfolioReviewScheduler {

    private final PortfolioReviewService reviewService;
    private final MarketDataService marketDataService;

    private volatile boolean isRunning = false;
    private volatile LocalDateTime lastRunTime = null;
    private volatile String lastRunStatus = "Nunca executado";

    /**
     * Executa automaticamente a cada 10 dias às 9h da manhã
     * Cron: segundo minuto hora dia mês dia-da-semana
     */
    @Scheduled(cron = "0 0 9 */10 * *")
    public void scheduledReview() {
        log.info("Iniciando revisão automática agendada de carteiras...");
        runFullReview();
    }

    /**
     * Executa análise manualmente (chamado pelo endpoint)
     */
    public List<PortfolioAnalysis> runManualReview() {
        if (isRunning) {
            log.warn("Análise já está em execução. Aguarde a conclusão.");
            return List.of();
        }
        return runFullReview();
    }

    /**
     * Executa análise de uma carteira específica
     */
    public List<PortfolioAnalysis> runPortfolioReview(String portfolioName) {
        if (isRunning) {
            log.warn("Análise já está em execução. Aguarde a conclusão.");
            return List.of();
        }

        isRunning = true;
        List<PortfolioAnalysis> results = new ArrayList<>();

        try {
            log.info("Iniciando revisão manual da carteira: {}", portfolioName);
            lastRunStatus = "Em execução...";

            List<InvestmentPortfolio> portfolios = marketDataService.getPortfolios();

            for (InvestmentPortfolio portfolio : portfolios) {
                if (portfolio.getName().equals(portfolioName) && portfolio.getRecommendedAssets() != null) {
                    for (RecommendedAsset asset : portfolio.getRecommendedAssets()) {
                        // Pular ativos de renda fixa (não têm preço de mercado)
                        if ("Renda Fixa".equals(asset.getType())) {
                            continue;
                        }

                        try {
                            PortfolioAnalysis analysis = reviewService.analyzeAsset(asset, portfolio.getName());
                            results.add(analysis);
                            log.info("Análise concluída para {}: {}", asset.getTicker(), analysis.getRecommendation());
                        } catch (Exception e) {
                            log.error("Erro ao analisar {}: {}", asset.getTicker(), e.getMessage());
                        }
                    }
                    break;
                }
            }

            lastRunTime = LocalDateTime.now();
            lastRunStatus = String.format("Concluído: %d ativos analisados", results.size());
            log.info("Revisão da carteira {} concluída. {} ativos analisados.", portfolioName, results.size());

        } catch (Exception e) {
            log.error("Erro durante revisão da carteira: {}", e.getMessage());
            lastRunStatus = "Erro: " + e.getMessage();
        } finally {
            isRunning = false;
        }

        return results;
    }

    /**
     * Executa análise de um ativo específico
     */
    public PortfolioAnalysis runAssetReview(String ticker, String portfolioName) {
        log.info("Iniciando revisão manual do ativo: {} na carteira {}", ticker, portfolioName);

        List<InvestmentPortfolio> portfolios = marketDataService.getPortfolios();

        for (InvestmentPortfolio portfolio : portfolios) {
            if (portfolio.getName().equals(portfolioName) && portfolio.getRecommendedAssets() != null) {
                for (RecommendedAsset asset : portfolio.getRecommendedAssets()) {
                    if (asset.getTicker().equalsIgnoreCase(ticker)) {
                        return reviewService.analyzeAsset(asset, portfolioName);
                    }
                }
            }
        }

        log.warn("Ativo {} não encontrado na carteira {}", ticker, portfolioName);
        return null;
    }

    private List<PortfolioAnalysis> runFullReview() {
        isRunning = true;
        List<PortfolioAnalysis> allResults = new ArrayList<>();

        try {
            lastRunStatus = "Em execução...";
            log.info("Buscando carteiras para revisão...");

            List<InvestmentPortfolio> portfolios = marketDataService.getPortfolios();
            int totalAssets = 0;

            for (InvestmentPortfolio portfolio : portfolios) {
                if (portfolio.getRecommendedAssets() == null) {
                    continue;
                }

                log.info("Revisando carteira: {}", portfolio.getName());

                for (RecommendedAsset asset : portfolio.getRecommendedAssets()) {
                    // Pular ativos de renda fixa (não têm preço de mercado)
                    if ("Renda Fixa".equals(asset.getType())) {
                        continue;
                    }

                    try {
                        PortfolioAnalysis analysis = reviewService.analyzeAsset(asset, portfolio.getName());
                        allResults.add(analysis);
                        totalAssets++;
                        log.info("Análise concluída para {}: {}", asset.getTicker(), analysis.getRecommendation());

                        // Pequena pausa para não sobrecarregar APIs
                        Thread.sleep(500);
                    } catch (Exception e) {
                        log.error("Erro ao analisar {}: {}", asset.getTicker(), e.getMessage());
                    }
                }
            }

            lastRunTime = LocalDateTime.now();
            lastRunStatus = String.format("Concluído: %d ativos analisados", totalAssets);
            log.info("Revisão completa concluída. {} ativos analisados.", totalAssets);

        } catch (Exception e) {
            log.error("Erro durante revisão de carteiras: {}", e.getMessage());
            lastRunStatus = "Erro: " + e.getMessage();
        } finally {
            isRunning = false;
        }

        return allResults;
    }

    // Getters para status
    public boolean isRunning() {
        return isRunning;
    }

    public LocalDateTime getLastRunTime() {
        return lastRunTime;
    }

    public String getLastRunStatus() {
        return lastRunStatus;
    }
}
