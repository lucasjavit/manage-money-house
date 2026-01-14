package com.managehouse.money.service;

import com.managehouse.money.config.ChatModelFactory;
import com.managehouse.money.dto.B3ReportUploadResponse;
import com.managehouse.money.dto.RealPortfolioSummaryDTO;
import com.managehouse.money.dto.YahooFinanceDTO;
import com.managehouse.money.entity.PortfolioDividend;
import com.managehouse.money.entity.PortfolioPosition;
import com.managehouse.money.entity.UserRealPortfolio;
import com.managehouse.money.repository.PortfolioDividendRepository;
import com.managehouse.money.repository.PortfolioPositionRepository;
import com.managehouse.money.repository.UserRealPortfolioRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RealPortfolioService {

    private final UserRealPortfolioRepository portfolioRepository;
    private final PortfolioPositionRepository positionRepository;
    private final PortfolioDividendRepository dividendRepository;
    private final ChatModelFactory chatModelFactory;
    private final ConfigurationService configurationService;
    private final YahooFinanceService yahooFinanceService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public UserRealPortfolio savePortfolio(Long userId, B3ReportUploadResponse data) {
        log.info("Salvando carteira para usuario {} - {}/{}", userId, data.getReportMonth(), data.getReportYear());

        // Verificar se ja existe carteira para este mes/ano e deletar
        portfolioRepository.findByUserIdAndReportMonthAndReportYear(
                userId, data.getReportMonth(), data.getReportYear()
        ).ifPresent(existing -> {
            log.info("Deletando carteira existente id={}", existing.getId());
            positionRepository.deleteByPortfolioId(existing.getId());
            dividendRepository.deleteByPortfolioId(existing.getId());
            portfolioRepository.delete(existing);
        });

        // Criar nova carteira
        UserRealPortfolio portfolio = UserRealPortfolio.builder()
                .userId(userId)
                .reportMonth(data.getReportMonth())
                .reportYear(data.getReportYear())
                .totalStocks(data.getTotals() != null ? data.getTotals().getStocks() : BigDecimal.ZERO)
                .totalFiis(data.getTotals() != null ? data.getTotals().getFiis() : BigDecimal.ZERO)
                .totalFixedIncome(data.getTotals() != null ? data.getTotals().getFixedIncome() : BigDecimal.ZERO)
                .totalFunds(data.getTotals() != null ? data.getTotals().getFunds() : BigDecimal.ZERO)
                .totalDividends(data.getTotals() != null ? data.getTotals().getDividends() : BigDecimal.ZERO)
                .grandTotal(data.getTotals() != null ? data.getTotals().getGrandTotal() : BigDecimal.ZERO)
                .build();

        portfolio = portfolioRepository.save(portfolio);
        log.info("Carteira salva com id={}", portfolio.getId());

        // Salvar posicoes de acoes
        if (data.getStocks() != null) {
            for (B3ReportUploadResponse.StockPosition stock : data.getStocks()) {
                PortfolioPosition position = PortfolioPosition.builder()
                        .portfolio(portfolio)
                        .ticker(stock.getTicker())
                        .name(stock.getName())
                        .assetType("ACAO")
                        .assetSubtype(stock.getType())
                        .institution(stock.getInstitution())
                        .quantity(stock.getQuantity())
                        .closePrice(stock.getClosePrice())
                        .totalValue(stock.getTotalValue())
                        .build();
                positionRepository.save(position);
            }
        }

        // Salvar posicoes de FIIs
        if (data.getFiis() != null) {
            for (B3ReportUploadResponse.FiiPosition fii : data.getFiis()) {
                PortfolioPosition position = PortfolioPosition.builder()
                        .portfolio(portfolio)
                        .ticker(fii.getTicker())
                        .name(fii.getName())
                        .assetType("FII")
                        .institution(fii.getInstitution())
                        .quantity(fii.getQuantity())
                        .closePrice(fii.getClosePrice())
                        .totalValue(fii.getTotalValue())
                        .build();
                positionRepository.save(position);
            }
        }

        // Salvar posicoes de Renda Fixa
        if (data.getFixedIncome() != null) {
            for (B3ReportUploadResponse.FixedIncomePosition fi : data.getFixedIncome()) {
                LocalDate maturity = null;
                if (fi.getMaturityDate() != null) {
                    try {
                        maturity = LocalDate.parse(fi.getMaturityDate());
                    } catch (Exception e) {
                        log.warn("Erro ao parsear data de vencimento: {}", fi.getMaturityDate());
                    }
                }

                PortfolioPosition position = PortfolioPosition.builder()
                        .portfolio(portfolio)
                        .ticker(fi.getProduct())
                        .name(fi.getProduct())
                        .assetType(fi.getProductType() != null ? fi.getProductType() : "RENDA_FIXA")
                        .institution(fi.getInstitution())
                        .quantity(fi.getQuantity())
                        .closePrice(fi.getUnitPrice())
                        .totalValue(fi.getTotalValue())
                        .maturityDate(maturity)
                        .build();
                positionRepository.save(position);
            }
        }

        // Salvar posicoes de Fundos
        if (data.getFunds() != null) {
            for (B3ReportUploadResponse.FundPosition fund : data.getFunds()) {
                PortfolioPosition position = PortfolioPosition.builder()
                        .portfolio(portfolio)
                        .ticker(fund.getTicker())
                        .name(fund.getName())
                        .assetType(fund.getFundType() != null ? fund.getFundType() : "FUNDO")
                        .institution(fund.getInstitution())
                        .quantity(fund.getQuantity())
                        .closePrice(fund.getClosePrice())
                        .totalValue(fund.getTotalValue())
                        .build();
                positionRepository.save(position);
            }
        }

        // Salvar dividendos
        if (data.getDividends() != null) {
            for (B3ReportUploadResponse.DividendReceived div : data.getDividends()) {
                LocalDate paymentDate = null;
                if (div.getPaymentDate() != null) {
                    try {
                        paymentDate = LocalDate.parse(div.getPaymentDate());
                    } catch (Exception e) {
                        log.warn("Erro ao parsear data de pagamento: {}", div.getPaymentDate());
                    }
                }

                PortfolioDividend dividend = PortfolioDividend.builder()
                        .portfolio(portfolio)
                        .ticker(div.getTicker())
                        .productName(div.getProductName())
                        .paymentDate(paymentDate)
                        .eventType(div.getEventType())
                        .quantity(div.getQuantity())
                        .unitPrice(div.getUnitPrice())
                        .netValue(div.getNetValue())
                        .build();
                dividendRepository.save(dividend);
            }
        }

        log.info("Todas as posicoes e dividendos salvos para carteira id={}", portfolio.getId());
        return portfolio;
    }

    public Optional<RealPortfolioSummaryDTO> getLatestPortfolio(Long userId) {
        return portfolioRepository.findTopByUserIdOrderByReportYearDescReportMonthDesc(userId)
                .map(this::convertToDTO);
    }

    public Optional<RealPortfolioSummaryDTO> getPortfolioByMonthYear(Long userId, Integer month, Integer year) {
        return portfolioRepository.findByUserIdAndReportMonthAndReportYear(userId, month, year)
                .map(this::convertToDTO);
    }

    public List<RealPortfolioSummaryDTO> getAllPortfolios(Long userId) {
        return portfolioRepository.findByUserIdOrderByReportYearDescReportMonthDesc(userId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public String generateAIAnalysis(Long userId) {
        log.info("Gerando analise IA para usuario {}", userId);

        Optional<UserRealPortfolio> portfolioOpt = portfolioRepository
                .findTopByUserIdOrderByReportYearDescReportMonthDesc(userId);

        if (portfolioOpt.isEmpty()) {
            return "Nenhuma carteira encontrada para analise.";
        }

        UserRealPortfolio portfolio = portfolioOpt.get();
        List<PortfolioPosition> positions = positionRepository.findByPortfolioId(portfolio.getId());
        List<PortfolioDividend> dividends = dividendRepository.findByPortfolioId(portfolio.getId());

        String apiKey = configurationService.getOpenAIKey();
        if (apiKey == null || apiKey.isEmpty()) {
            return "Chave OpenAI nao configurada.";
        }

        try {
            ChatLanguageModel chatModel = chatModelFactory.createChatModel(apiKey);
            String prompt = buildAnalysisPrompt(portfolio, positions, dividends);
            String analysis = chatModel.generate(prompt);

            // Salvar analise
            portfolio.setAiAnalysis(analysis);
            portfolio.setAnalyzedAt(LocalDateTime.now());
            portfolioRepository.save(portfolio);

            return analysis;
        } catch (Exception e) {
            log.error("Erro ao gerar analise IA: {}", e.getMessage());
            return "Erro ao gerar analise: " + e.getMessage();
        }
    }

    @Transactional
    public void generateIndividualAssetAnalyses(Long userId) {
        log.info("Gerando analises individuais de ativos para usuario {}", userId);

        Optional<UserRealPortfolio> portfolioOpt = portfolioRepository
                .findTopByUserIdOrderByReportYearDescReportMonthDesc(userId);

        if (portfolioOpt.isEmpty()) {
            log.warn("Nenhuma carteira encontrada para usuario {}", userId);
            return;
        }

        UserRealPortfolio portfolio = portfolioOpt.get();
        List<PortfolioPosition> positions = positionRepository.findByPortfolioId(portfolio.getId());

        String apiKey = configurationService.getOpenAIKey();
        if (apiKey == null || apiKey.isEmpty()) {
            log.error("Chave OpenAI nao configurada");
            return;
        }

        ChatLanguageModel chatModel = chatModelFactory.createChatModel(apiKey);

        for (PortfolioPosition position : positions) {
            try {
                log.info("Analisando ativo: {} ({})", position.getTicker(), position.getAssetType());

                // Buscar dados fundamentalistas do Yahoo Finance para acoes e FIIs
                YahooFinanceDTO fundamentals = null;
                if ("ACAO".equals(position.getAssetType()) || "FII".equals(position.getAssetType())) {
                    fundamentals = yahooFinanceService.getStockFundamentals(position.getTicker());
                    if (fundamentals.isDataAvailable()) {
                        log.info("Dados Yahoo Finance obtidos para {}: P/L={}, P/VP={}, DY={}",
                                position.getTicker(),
                                fundamentals.getFormattedPE(),
                                fundamentals.getFormattedPB(),
                                fundamentals.getFormattedDY());

                        // Salvar dados do Yahoo Finance na posicao para exibir no tooltip
                        position.setYahooTrailingPE(fundamentals.getTrailingPE() != null
                                ? BigDecimal.valueOf(fundamentals.getTrailingPE()) : null);
                        position.setYahooPriceToBook(fundamentals.getPriceToBook() != null
                                ? BigDecimal.valueOf(fundamentals.getPriceToBook()) : null);
                        position.setYahooDividendYield(fundamentals.getDividendYield() != null
                                ? BigDecimal.valueOf(fundamentals.getDividendYield()) : null);
                        position.setYahooSector(fundamentals.getSector());
                    } else {
                        log.warn("Dados Yahoo Finance indisponiveis para {}: {}",
                                position.getTicker(), fundamentals.getErrorMessage());
                    }
                }

                String prompt = buildAssetAnalysisPrompt(position, portfolio, fundamentals);
                String aiResponse = chatModel.generate(prompt);

                // Parsear resposta JSON da IA
                parseAndSaveAssetAnalysis(position, aiResponse);

                log.info("Analise salva para ativo: {}", position.getTicker());
            } catch (Exception e) {
                log.error("Erro ao analisar ativo {}: {}", position.getTicker(), e.getMessage());
                // Continua com proximo ativo
            }
        }

        // Apos analisar todos os ativos, calcular o health score da carteira
        calculateAndSaveHealthScore(portfolio, positions);

        log.info("Analises individuais concluidas para {} ativos", positions.size());
    }

    /**
     * Calcula o Health Score (0-100) da carteira baseado em criterios profissionais:
     * - Diversificacao entre classes de ativos (0-25 pontos)
     * - Concentracao de posicoes (0-25 pontos)
     * - Qualidade dos ativos com base nas recomendacoes (0-25 pontos)
     * - Nivel de risco geral (0-25 pontos)
     */
    @Transactional
    public void calculateAndSaveHealthScore(UserRealPortfolio portfolio, List<PortfolioPosition> positions) {
        log.info("Calculando Health Score para carteira id={}", portfolio.getId());

        if (positions == null || positions.isEmpty()) {
            positions = positionRepository.findByPortfolioId(portfolio.getId());
        }

        BigDecimal grandTotal = portfolio.getGrandTotal() != null && portfolio.getGrandTotal().compareTo(BigDecimal.ZERO) > 0
                ? portfolio.getGrandTotal()
                : BigDecimal.ONE;

        // 1. Diversificacao entre classes (0-25)
        BigDecimal diversificationScore = calculateDiversificationScore(portfolio, grandTotal);

        // 2. Concentracao de posicoes (0-25)
        BigDecimal concentrationScore = calculateConcentrationScore(positions, grandTotal);

        // 3. Qualidade dos ativos (0-25)
        BigDecimal qualityScore = calculateQualityScore(positions);

        // 4. Nivel de risco (0-25)
        BigDecimal riskScore = calculateRiskScore(positions);

        // Score total
        BigDecimal totalScore = diversificationScore
                .add(concentrationScore)
                .add(qualityScore)
                .add(riskScore);

        // Determinar status geral
        String overallStatus = determineOverallStatus(totalScore);

        // Identificar ponto forte e fraco
        String mainStrength = identifyMainStrength(diversificationScore, concentrationScore, qualityScore, riskScore);
        String mainWeakness = identifyMainWeakness(diversificationScore, concentrationScore, qualityScore, riskScore);

        // Gerar recomendacoes
        List<String> recommendations = generateHealthRecommendations(
                diversificationScore, concentrationScore, qualityScore, riskScore,
                portfolio, positions, grandTotal);

        // Criar objeto de detalhes
        RealPortfolioSummaryDTO.HealthScoreDetails details = RealPortfolioSummaryDTO.HealthScoreDetails.builder()
                .diversificationScore(diversificationScore)
                .concentrationScore(concentrationScore)
                .qualityScore(qualityScore)
                .riskScore(riskScore)
                .overallStatus(overallStatus)
                .mainStrength(mainStrength)
                .mainWeakness(mainWeakness)
                .recommendations(recommendations)
                .build();

        // Salvar no portfolio
        try {
            String detailsJson = objectMapper.writeValueAsString(details);
            portfolio.setHealthScore(totalScore);
            portfolio.setHealthScoreDetails(detailsJson);
            portfolioRepository.save(portfolio);
            log.info("Health Score salvo: {} ({})", totalScore, overallStatus);
        } catch (Exception e) {
            log.error("Erro ao salvar Health Score: {}", e.getMessage());
        }
    }

    private BigDecimal calculateDiversificationScore(UserRealPortfolio portfolio, BigDecimal grandTotal) {
        // Ideal: 4 classes de ativos com distribuicao equilibrada
        // Penalizar se muito concentrado em uma classe ou se poucas classes

        BigDecimal totalStocks = portfolio.getTotalStocks() != null ? portfolio.getTotalStocks() : BigDecimal.ZERO;
        BigDecimal totalFiis = portfolio.getTotalFiis() != null ? portfolio.getTotalFiis() : BigDecimal.ZERO;
        BigDecimal totalFixedIncome = portfolio.getTotalFixedIncome() != null ? portfolio.getTotalFixedIncome() : BigDecimal.ZERO;
        BigDecimal totalFunds = portfolio.getTotalFunds() != null ? portfolio.getTotalFunds() : BigDecimal.ZERO;

        int classesPresentes = 0;
        if (totalStocks.compareTo(BigDecimal.ZERO) > 0) classesPresentes++;
        if (totalFiis.compareTo(BigDecimal.ZERO) > 0) classesPresentes++;
        if (totalFixedIncome.compareTo(BigDecimal.ZERO) > 0) classesPresentes++;
        if (totalFunds.compareTo(BigDecimal.ZERO) > 0) classesPresentes++;

        // Pontuacao base pela quantidade de classes (max 15 pontos)
        BigDecimal baseScore = new BigDecimal(classesPresentes * 3.75);

        // Bonus por distribuicao equilibrada (max 10 pontos)
        BigDecimal pctStocks = totalStocks.divide(grandTotal, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
        BigDecimal pctFiis = totalFiis.divide(grandTotal, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
        BigDecimal pctFixed = totalFixedIncome.divide(grandTotal, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
        BigDecimal pctFunds = totalFunds.divide(grandTotal, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));

        // Verificar se alguma classe domina muito (>60%)
        BigDecimal distributionBonus = new BigDecimal("10");
        if (pctStocks.compareTo(new BigDecimal("60")) > 0 ||
            pctFiis.compareTo(new BigDecimal("60")) > 0 ||
            pctFixed.compareTo(new BigDecimal("60")) > 0 ||
            pctFunds.compareTo(new BigDecimal("60")) > 0) {
            distributionBonus = new BigDecimal("5");
        }
        if (pctStocks.compareTo(new BigDecimal("80")) > 0 ||
            pctFiis.compareTo(new BigDecimal("80")) > 0 ||
            pctFixed.compareTo(new BigDecimal("80")) > 0 ||
            pctFunds.compareTo(new BigDecimal("80")) > 0) {
            distributionBonus = new BigDecimal("2");
        }

        BigDecimal total = baseScore.add(distributionBonus).min(new BigDecimal("25"));
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateConcentrationScore(List<PortfolioPosition> positions, BigDecimal grandTotal) {
        // Penalizar se um ativo representa muito do portfolio
        // Ideal: nenhum ativo > 10%, max score se todos < 5%

        if (positions.isEmpty()) {
            return BigDecimal.ZERO;
        }

        int positionsAbove10Pct = 0;
        int positionsAbove15Pct = 0;
        int positionsAbove20Pct = 0;

        for (PortfolioPosition pos : positions) {
            BigDecimal totalValue = pos.getTotalValue() != null ? pos.getTotalValue() : BigDecimal.ZERO;
            BigDecimal pct = totalValue.divide(grandTotal, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));

            if (pct.compareTo(new BigDecimal("20")) > 0) {
                positionsAbove20Pct++;
            } else if (pct.compareTo(new BigDecimal("15")) > 0) {
                positionsAbove15Pct++;
            } else if (pct.compareTo(new BigDecimal("10")) > 0) {
                positionsAbove10Pct++;
            }
        }

        // Score base: 25 pontos
        BigDecimal score = new BigDecimal("25");

        // Penalizacoes
        score = score.subtract(new BigDecimal(positionsAbove10Pct * 2)); // -2 por ativo >10%
        score = score.subtract(new BigDecimal(positionsAbove15Pct * 4)); // -4 adicional por ativo >15%
        score = score.subtract(new BigDecimal(positionsAbove20Pct * 6)); // -6 adicional por ativo >20%

        return score.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateQualityScore(List<PortfolioPosition> positions) {
        // Baseado nas recomendacoes de IA dos ativos
        // MANTER/COMPRAR_MAIS = bom, VENDER = ruim

        if (positions.isEmpty()) {
            return BigDecimal.ZERO;
        }

        int totalWithRecommendation = 0;
        int goodRecommendations = 0; // MANTER ou COMPRAR_MAIS
        int badRecommendations = 0;  // VENDER

        for (PortfolioPosition pos : positions) {
            if (pos.getAiRecommendation() != null) {
                totalWithRecommendation++;
                if ("MANTER".equals(pos.getAiRecommendation()) || "COMPRAR_MAIS".equals(pos.getAiRecommendation())) {
                    goodRecommendations++;
                } else if ("VENDER".equals(pos.getAiRecommendation())) {
                    badRecommendations++;
                }
            }
        }

        if (totalWithRecommendation == 0) {
            return new BigDecimal("12.5"); // Score neutro se nao ha recomendacoes
        }

        // Calcular percentual de boas recomendacoes
        double goodPct = (double) goodRecommendations / totalWithRecommendation;

        // Score proporcional (0-25)
        BigDecimal score = new BigDecimal(goodPct * 25);

        // Penalidade extra por ativos com recomendacao VENDER
        score = score.subtract(new BigDecimal(badRecommendations * 2));

        return score.max(BigDecimal.ZERO).min(new BigDecimal("25")).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateRiskScore(List<PortfolioPosition> positions) {
        // Baseado no nivel de risco dos ativos (BAIXO, MEDIO, ALTO)

        if (positions.isEmpty()) {
            return BigDecimal.ZERO;
        }

        int totalWithRisk = 0;
        int lowRisk = 0;
        int mediumRisk = 0;
        int highRisk = 0;

        for (PortfolioPosition pos : positions) {
            if (pos.getAiRiskLevel() != null) {
                totalWithRisk++;
                switch (pos.getAiRiskLevel()) {
                    case "BAIXO" -> lowRisk++;
                    case "MEDIO" -> mediumRisk++;
                    case "ALTO" -> highRisk++;
                }
            }
        }

        if (totalWithRisk == 0) {
            return new BigDecimal("12.5"); // Score neutro
        }

        // Pesos: BAIXO = 25pts, MEDIO = 15pts, ALTO = 5pts
        double weightedSum = (lowRisk * 25.0 + mediumRisk * 15.0 + highRisk * 5.0) / totalWithRisk;

        return new BigDecimal(weightedSum).min(new BigDecimal("25")).setScale(2, RoundingMode.HALF_UP);
    }

    private String determineOverallStatus(BigDecimal totalScore) {
        int score = totalScore.intValue();
        if (score >= 85) return "EXCELENTE";
        if (score >= 70) return "BOM";
        if (score >= 50) return "REGULAR";
        if (score >= 30) return "RUIM";
        return "CRITICO";
    }

    private String identifyMainStrength(BigDecimal div, BigDecimal conc, BigDecimal qual, BigDecimal risk) {
        BigDecimal max = div;
        String strength = "Boa diversificacao entre classes de ativos";

        if (conc.compareTo(max) > 0) {
            max = conc;
            strength = "Posicoes bem distribuidas, sem concentracao excessiva";
        }
        if (qual.compareTo(max) > 0) {
            max = qual;
            strength = "Ativos de qualidade com boas recomendacoes";
        }
        if (risk.compareTo(max) > 0) {
            strength = "Perfil de risco controlado";
        }
        return strength;
    }

    private String identifyMainWeakness(BigDecimal div, BigDecimal conc, BigDecimal qual, BigDecimal risk) {
        BigDecimal min = div;
        String weakness = "Poderia diversificar mais entre classes de ativos";

        if (conc.compareTo(min) < 0) {
            min = conc;
            weakness = "Concentracao elevada em poucos ativos";
        }
        if (qual.compareTo(min) < 0) {
            min = qual;
            weakness = "Alguns ativos com recomendacao de venda";
        }
        if (risk.compareTo(min) < 0) {
            weakness = "Nivel de risco elevado na carteira";
        }
        return weakness;
    }

    private List<String> generateHealthRecommendations(
            BigDecimal divScore, BigDecimal concScore, BigDecimal qualScore, BigDecimal riskScore,
            UserRealPortfolio portfolio, List<PortfolioPosition> positions, BigDecimal grandTotal) {

        List<String> recommendations = new ArrayList<>();

        // Recomendacoes de diversificacao
        if (divScore.compareTo(new BigDecimal("15")) < 0) {
            BigDecimal pctFixed = portfolio.getTotalFixedIncome() != null
                    ? portfolio.getTotalFixedIncome().divide(grandTotal, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                    : BigDecimal.ZERO;
            if (pctFixed.compareTo(new BigDecimal("60")) > 0) {
                recommendations.add("Considere aumentar exposicao em renda variavel (acoes e FIIs) para melhor diversificacao");
            } else {
                recommendations.add("Diversifique entre mais classes de ativos (acoes, FIIs, renda fixa, fundos)");
            }
        }

        // Recomendacoes de concentracao
        if (concScore.compareTo(new BigDecimal("15")) < 0) {
            for (PortfolioPosition pos : positions) {
                BigDecimal pct = pos.getTotalValue().divide(grandTotal, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
                if (pct.compareTo(new BigDecimal("15")) > 0) {
                    recommendations.add("Reduza exposicao em " + pos.getTicker() + " (representa " + pct.setScale(1, RoundingMode.HALF_UP) + "% da carteira)");
                    break; // Apenas uma recomendacao deste tipo
                }
            }
        }

        // Recomendacoes de qualidade
        if (qualScore.compareTo(new BigDecimal("15")) < 0) {
            for (PortfolioPosition pos : positions) {
                if ("VENDER".equals(pos.getAiRecommendation())) {
                    recommendations.add("Avalie realizar " + pos.getTicker() + " conforme recomendacao de venda");
                    break;
                }
            }
        }

        // Recomendacoes de risco
        if (riskScore.compareTo(new BigDecimal("15")) < 0) {
            recommendations.add("Considere adicionar ativos de menor risco para equilibrar a carteira");
        }

        // Recomendacao geral se carteira esta boa
        if (recommendations.isEmpty()) {
            recommendations.add("Carteira bem estruturada. Mantenha o monitoramento periodico dos ativos");
        }

        return recommendations;
    }

    private String buildAssetAnalysisPrompt(PortfolioPosition position, UserRealPortfolio portfolio, YahooFinanceDTO fundamentals) {
        BigDecimal grandTotal = portfolio.getGrandTotal() != null ? portfolio.getGrandTotal() : BigDecimal.ONE;
        BigDecimal totalValue = position.getTotalValue() != null ? position.getTotalValue() : BigDecimal.ZERO;

        BigDecimal percentOfPortfolio = totalValue
                .divide(grandTotal, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        // Construir secao de dados fundamentalistas
        String fundamentalsSection = buildFundamentalsSection(fundamentals);

        return """
            Voce e um analista de investimentos CNPI brasileiro. Analise com base nos DADOS REAIS abaixo.

            ATIVO A ANALISAR:
            - Ticker: %s
            - Nome: %s
            - Tipo: %s
            - Subtipo: %s
            - Preco Atual: R$ %s
            - Valor Total: R$ %s
            - Percentual na Carteira: %.2f%%
            - Vencimento: %s

            CONTEXTO DA CARTEIRA:
            - Total: R$ %s | Acoes: R$ %s | FIIs: R$ %s | Renda Fixa: R$ %s

            %s

            ============================================================
            REGRAS DE ANALISE FUNDAMENTALISTA:
            ============================================================

            1. CONCENTRACAO (regra fixa):
               - Se percentual > 20%% da carteira → recommendation = "VENDER"
               - Se percentual entre 10-20%% → mencionar risco, pode ser "MANTER"
               - Se percentual < 10%% → OK

            2. PARA ACOES COM DADOS REAIS (se P/L e P/VP disponiveis):
               VALUATION:
               - P/L < 8 E P/VP < 1.0 → ativo SUBVALORIZADO → considerar "COMPRAR_MAIS"
               - P/L > 15 OU P/VP > 2.0 → ativo CARO → considerar "VENDER" ou "MANTER"
               - P/L entre 8-15 E P/VP entre 0.8-1.5 → ativo JUSTO → "MANTER"

               PRECO TETO (calcule usando LPA real se disponivel):
               - ceilingPrice = LPA * P/L_justo_do_setor
               - Bancos: P/L justo = 8
               - Utilities/Energia: P/L justo = 12
               - Commodities: P/L justo = 6
               - Varejo/Consumo: P/L justo = 15
               - Outros: P/L justo = 10

               DIVIDEND YIELD:
               - DY > 6%% → ponto POSITIVO, mencionar na analise
               - DY < 3%% para empresa madura → ponto NEGATIVO

            3. PARA ACOES SEM DADOS (fallback):
               - Use multiplicadores fixos: ceilingPrice = precoAtual * 1.15
               - confidenceScore = 0.60 (menor confianca)

            4. PARA FIIs:
               - P/VP > 1.1 → CARO → considerar "VENDER"
               - P/VP < 0.9 → BARATO → considerar "COMPRAR_MAIS"
               - P/VP entre 0.9-1.1 → JUSTO → "MANTER"
               - ceilingPrice = precoAtual / P/VP_atual (para chegar em P/VP = 1.0)
               - DY < 8%% aa → ponto negativo

            5. PARA RENDA FIXA (CDB, LCA, LCI, DEBENTURE):
               - Se nome contem "Master", "BRK", "Portocred", "Open", "Digimais" → "VENDER", riskLevel = "ALTO"
               - Se vencimento < 6 meses → riskLevel = "BAIXO"
               - Se vencimento > 2 anos → riskLevel = "MEDIO"
               - ceilingPrice = null (SEMPRE)
               - bias = "NEUTRO" (SEMPRE)

            6. BIAS:
               - "COMPRA" se P/L < 10 E P/VP < 1.0 E DY > 5%%
               - "VENDA" se recomendacao = "VENDER"
               - "NEUTRO" nos demais casos

            7. CONFIDENCE SCORE:
               - 0.90 se tem dados Yahoo Finance completos (P/L, P/VP, DY)
               - 0.75 se tem dados parciais
               - 0.60 se nao tem dados fundamentalistas
               - 0.30 para renda fixa de banco em liquidacao

            ============================================================

            Responda APENAS em JSON valido (sem markdown, sem ```):
            {
              "recommendation": "MANTER",
              "analysis": "Justificativa em 2 frases usando os indicadores reais",
              "mainReason": "Frase curta (max 50 chars)",
              "riskLevel": "BAIXO",
              "confidenceScore": 0.90,
              "ceilingPrice": 25.50,
              "bias": "COMPRA",
              "valuationAnalysis": "Analise de 2-3 frases explicando os indicadores: P/L indica X, P/VP mostra Y, DY sugere Z. Conclusao do vies."
            }

            Valores permitidos:
            - recommendation: "MANTER", "VENDER", "COMPRAR_MAIS"
            - riskLevel: "BAIXO", "MEDIO", "ALTO"
            - bias: "COMPRA", "VENDA", "NEUTRO"
            - ceilingPrice: numero decimal ou null
            - valuationAnalysis: texto explicando os indicadores de valuation e justificando o vies
            """.formatted(
                position.getTicker(),
                position.getName(),
                position.getAssetType(),
                position.getAssetSubtype() != null ? position.getAssetSubtype() : "N/A",
                position.getClosePrice(),
                position.getTotalValue(),
                percentOfPortfolio,
                position.getMaturityDate() != null ? position.getMaturityDate().toString() : "N/A",
                portfolio.getGrandTotal() != null ? portfolio.getGrandTotal() : BigDecimal.ZERO,
                portfolio.getTotalStocks() != null ? portfolio.getTotalStocks() : BigDecimal.ZERO,
                portfolio.getTotalFiis() != null ? portfolio.getTotalFiis() : BigDecimal.ZERO,
                portfolio.getTotalFixedIncome() != null ? portfolio.getTotalFixedIncome() : BigDecimal.ZERO,
                fundamentalsSection
        );
    }

    /**
     * Constroi a secao de dados fundamentalistas para o prompt
     */
    private String buildFundamentalsSection(YahooFinanceDTO fundamentals) {
        if (fundamentals == null || !fundamentals.isDataAvailable()) {
            return """
            DADOS FUNDAMENTALISTAS (Yahoo Finance):
            - Dados NAO DISPONIVEIS para este ativo
            - Use as regras de fallback com multiplicadores fixos
            """;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("DADOS FUNDAMENTALISTAS REAIS (Yahoo Finance):\n");

        if (fundamentals.getTrailingPE() != null) {
            sb.append(String.format("- P/L (Preco/Lucro): %.2f", fundamentals.getTrailingPE()));
            if (fundamentals.getTrailingPE() < 8) sb.append(" [BARATO]");
            else if (fundamentals.getTrailingPE() > 15) sb.append(" [CARO]");
            else sb.append(" [JUSTO]");
            sb.append("\n");
        } else {
            sb.append("- P/L: N/A\n");
        }

        if (fundamentals.getPriceToBook() != null) {
            sb.append(String.format("- P/VP (Preco/Valor Patrimonial): %.2f", fundamentals.getPriceToBook()));
            if (fundamentals.getPriceToBook() < 1.0) sb.append(" [BARATO - abaixo do VPA]");
            else if (fundamentals.getPriceToBook() > 2.0) sb.append(" [CARO]");
            else sb.append(" [JUSTO]");
            sb.append("\n");
        } else {
            sb.append("- P/VP: N/A\n");
        }

        if (fundamentals.getDividendYield() != null) {
            double dyPercent = fundamentals.getDividendYield() * 100;
            sb.append(String.format("- Dividend Yield: %.2f%%", dyPercent));
            if (dyPercent > 6) sb.append(" [EXCELENTE]");
            else if (dyPercent > 4) sb.append(" [BOM]");
            else if (dyPercent < 2) sb.append(" [BAIXO]");
            sb.append("\n");
        } else {
            sb.append("- Dividend Yield: N/A\n");
        }

        if (fundamentals.getTrailingEps() != null) {
            sb.append(String.format("- LPA (Lucro por Acao): R$ %.2f\n", fundamentals.getTrailingEps()));
        }

        if (fundamentals.getBookValue() != null) {
            sb.append(String.format("- VPA (Valor Patrimonial por Acao): R$ %.2f\n", fundamentals.getBookValue()));
        }

        if (fundamentals.getRegularMarketPrice() != null) {
            sb.append(String.format("- Preco de Mercado (Yahoo): R$ %.2f\n", fundamentals.getRegularMarketPrice()));
        }

        if (fundamentals.getFiftyTwoWeekHigh() != null && fundamentals.getFiftyTwoWeekLow() != null) {
            sb.append(String.format("- Range 52 semanas: R$ %.2f - R$ %.2f\n",
                    fundamentals.getFiftyTwoWeekLow(), fundamentals.getFiftyTwoWeekHigh()));
        }

        if (fundamentals.getSector() != null) {
            sb.append(String.format("- Setor: %s\n", fundamentals.getSector()));
        }

        if (fundamentals.getIndustry() != null) {
            sb.append(String.format("- Industria: %s\n", fundamentals.getIndustry()));
        }

        // Adicionar calculo de preco teto sugerido
        if (fundamentals.getTrailingEps() != null && fundamentals.getTrailingEps() > 0) {
            double fairPE = determineFairPE(fundamentals.getSector());
            double suggestedCeiling = fundamentals.getTrailingEps() * fairPE;
            sb.append(String.format("\nPRECO TETO SUGERIDO: R$ %.2f (LPA %.2f x P/L justo %.0f)\n",
                    suggestedCeiling, fundamentals.getTrailingEps(), fairPE));
        }

        return sb.toString();
    }

    /**
     * Determina o P/L justo baseado no setor
     */
    private double determineFairPE(String sector) {
        if (sector == null) return 10.0;

        String sectorLower = sector.toLowerCase();
        if (sectorLower.contains("financial") || sectorLower.contains("bank")) {
            return 8.0;
        } else if (sectorLower.contains("utilities") || sectorLower.contains("energy")) {
            return 12.0;
        } else if (sectorLower.contains("basic materials") || sectorLower.contains("mining")) {
            return 6.0;
        } else if (sectorLower.contains("consumer") || sectorLower.contains("retail")) {
            return 15.0;
        } else if (sectorLower.contains("technology")) {
            return 20.0;
        }
        return 10.0;
    }

    private void parseAndSaveAssetAnalysis(PortfolioPosition position, String aiResponse) {
        try {
            // Limpar resposta (remover markdown se houver)
            String cleanedResponse = aiResponse
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();

            JsonNode root = objectMapper.readTree(cleanedResponse);

            String recommendation = root.has("recommendation") ? root.get("recommendation").asText() : null;
            String analysis = root.has("analysis") ? root.get("analysis").asText() : null;
            String mainReason = root.has("mainReason") ? root.get("mainReason").asText() : null;
            String riskLevel = root.has("riskLevel") ? root.get("riskLevel").asText() : null;
            BigDecimal confidenceScore = root.has("confidenceScore")
                    ? new BigDecimal(root.get("confidenceScore").asText())
                    : null;
            BigDecimal ceilingPrice = null;
            if (root.has("ceilingPrice") && !root.get("ceilingPrice").isNull()) {
                try {
                    JsonNode ceilingNode = root.get("ceilingPrice");
                    if (ceilingNode.isNumber()) {
                        ceilingPrice = BigDecimal.valueOf(ceilingNode.asDouble());
                    } else {
                        ceilingPrice = new BigDecimal(ceilingNode.asText());
                    }
                    log.info("Preco teto parseado: {}", ceilingPrice);
                } catch (Exception e) {
                    log.warn("Erro ao parsear ceilingPrice: {}", root.get("ceilingPrice"));
                }
            }
            String bias = root.has("bias") ? root.get("bias").asText() : null;
            String valuationAnalysis = root.has("valuationAnalysis") ? root.get("valuationAnalysis").asText() : null;
            log.info("Bias parseado: {}, ValuationAnalysis: {}", bias, valuationAnalysis != null ? "presente" : "ausente");

            position.setAiRecommendation(recommendation);
            position.setAiAnalysis(analysis);
            position.setAiMainReason(mainReason);
            position.setAiRiskLevel(riskLevel);
            position.setAiConfidenceScore(confidenceScore);
            position.setAiCeilingPrice(ceilingPrice);
            position.setAiBias(bias);
            position.setAiValuationAnalysis(valuationAnalysis);
            position.setAiAnalyzedAt(LocalDateTime.now());

            positionRepository.save(position);

        } catch (Exception e) {
            log.error("Erro ao parsear analise do ativo {}: {}", position.getTicker(), e.getMessage());
            throw new RuntimeException("Erro ao parsear analise: " + e.getMessage());
        }
    }

    private String buildAnalysisPrompt(UserRealPortfolio portfolio, List<PortfolioPosition> positions, List<PortfolioDividend> dividends) {
        StringBuilder positionsText = new StringBuilder();
        for (PortfolioPosition pos : positions) {
            positionsText.append(String.format("- %s (%s): %s unidades, R$ %.2f total%n",
                    pos.getTicker(), pos.getAssetType(),
                    pos.getQuantity(), pos.getTotalValue()));
        }

        BigDecimal totalDividends = dividends.stream()
                .map(PortfolioDividend::getNetValue)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return """
            Voce e um consultor de investimentos brasileiro certificado (CNPI).

            Analise a carteira de investimentos abaixo e forneca:
            1. Resumo da alocacao atual (percentual por tipo de ativo)
            2. Pontos positivos da carteira
            3. Pontos de atencao ou riscos identificados
            4. Sugestoes de melhoria para diversificacao
            5. Analise dos proventos recebidos

            CARTEIRA (Ref: %d/%d):
            - Total em Acoes: R$ %.2f
            - Total em FIIs: R$ %.2f
            - Total em Renda Fixa: R$ %.2f
            - Total em Fundos: R$ %.2f
            - TOTAL GERAL: R$ %.2f
            - Proventos no mes: R$ %.2f

            POSICOES:
            %s

            IMPORTANTE:
            - Se houver CDBs do Banco Master em liquidacao extrajudicial, alerte sobre o risco
            - Avalie a concentracao em poucos ativos
            - Sugira diversificacao se necessario
            - Calcule o yield dos proventos sobre o patrimonio

            Responda de forma clara e objetiva em portugues brasileiro.
            """.formatted(
                portfolio.getReportMonth(), portfolio.getReportYear(),
                portfolio.getTotalStocks() != null ? portfolio.getTotalStocks() : BigDecimal.ZERO,
                portfolio.getTotalFiis() != null ? portfolio.getTotalFiis() : BigDecimal.ZERO,
                portfolio.getTotalFixedIncome() != null ? portfolio.getTotalFixedIncome() : BigDecimal.ZERO,
                portfolio.getTotalFunds() != null ? portfolio.getTotalFunds() : BigDecimal.ZERO,
                portfolio.getGrandTotal() != null ? portfolio.getGrandTotal() : BigDecimal.ZERO,
                totalDividends,
                positionsText.toString()
        );
    }

    private RealPortfolioSummaryDTO convertToDTO(UserRealPortfolio portfolio) {
        List<PortfolioPosition> positions = positionRepository.findByPortfolioId(portfolio.getId());
        List<PortfolioDividend> dividends = dividendRepository.findByPortfolioId(portfolio.getId());

        List<RealPortfolioSummaryDTO.PositionDTO> positionDTOs = positions.stream()
                .map(p -> RealPortfolioSummaryDTO.PositionDTO.builder()
                        .id(p.getId())
                        .ticker(p.getTicker())
                        .name(p.getName())
                        .assetType(p.getAssetType())
                        .assetSubtype(p.getAssetSubtype())
                        .institution(p.getInstitution())
                        .quantity(p.getQuantity())
                        .closePrice(p.getClosePrice())
                        .totalValue(p.getTotalValue())
                        .maturityDate(p.getMaturityDate() != null ? p.getMaturityDate().toString() : null)
                        // Campos de analise individual
                        .aiRecommendation(p.getAiRecommendation())
                        .aiAnalysis(p.getAiAnalysis())
                        .aiMainReason(p.getAiMainReason())
                        .aiRiskLevel(p.getAiRiskLevel())
                        .aiConfidenceScore(p.getAiConfidenceScore())
                        .aiCeilingPrice(p.getAiCeilingPrice())
                        .aiBias(p.getAiBias())
                        .aiValuationAnalysis(p.getAiValuationAnalysis())
                        .aiAnalyzedAt(p.getAiAnalyzedAt() != null ? p.getAiAnalyzedAt().toString() : null)
                        // Dados fundamentalistas do Yahoo Finance
                        .yahooTrailingPE(p.getYahooTrailingPE())
                        .yahooPriceToBook(p.getYahooPriceToBook())
                        .yahooDividendYield(p.getYahooDividendYield())
                        .yahooSector(p.getYahooSector())
                        .build())
                .collect(Collectors.toList());

        List<RealPortfolioSummaryDTO.DividendDTO> dividendDTOs = dividends.stream()
                .map(d -> RealPortfolioSummaryDTO.DividendDTO.builder()
                        .id(d.getId())
                        .ticker(d.getTicker())
                        .productName(d.getProductName())
                        .paymentDate(d.getPaymentDate() != null ? d.getPaymentDate().toString() : null)
                        .eventType(d.getEventType())
                        .quantity(d.getQuantity())
                        .unitPrice(d.getUnitPrice())
                        .netValue(d.getNetValue())
                        .build())
                .collect(Collectors.toList());

        // Parse health score details from JSON
        RealPortfolioSummaryDTO.HealthScoreDetails healthScoreDetails = null;
        if (portfolio.getHealthScoreDetails() != null && !portfolio.getHealthScoreDetails().isEmpty()) {
            try {
                healthScoreDetails = objectMapper.readValue(
                        portfolio.getHealthScoreDetails(),
                        RealPortfolioSummaryDTO.HealthScoreDetails.class);
            } catch (Exception e) {
                log.warn("Erro ao parsear healthScoreDetails: {}", e.getMessage());
            }
        }

        return RealPortfolioSummaryDTO.builder()
                .id(portfolio.getId())
                .userId(portfolio.getUserId())
                .reportMonth(portfolio.getReportMonth())
                .reportYear(portfolio.getReportYear())
                .totalStocks(portfolio.getTotalStocks())
                .totalFiis(portfolio.getTotalFiis())
                .totalFixedIncome(portfolio.getTotalFixedIncome())
                .totalFunds(portfolio.getTotalFunds())
                .totalDividends(portfolio.getTotalDividends())
                .grandTotal(portfolio.getGrandTotal())
                .aiAnalysis(portfolio.getAiAnalysis())
                .uploadedAt(portfolio.getUploadedAt())
                .analyzedAt(portfolio.getAnalyzedAt())
                .healthScore(portfolio.getHealthScore())
                .healthScoreDetails(healthScoreDetails)
                .positions(positionDTOs)
                .dividends(dividendDTOs)
                .build();
    }
}
