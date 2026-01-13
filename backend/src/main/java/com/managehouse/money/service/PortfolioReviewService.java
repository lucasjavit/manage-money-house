package com.managehouse.money.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.managehouse.money.config.ChatModelFactory;
import com.managehouse.money.dto.EconomicContextResponse;
import com.managehouse.money.dto.RecommendedAsset;
import com.managehouse.money.entity.PortfolioAnalysis;
import com.managehouse.money.repository.PortfolioAnalysisRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PortfolioReviewService {

    private final PortfolioAnalysisRepository analysisRepository;
    private final ChatModelFactory chatModelFactory;
    private final ConfigurationService configurationService;
    private final EconomicDataService economicDataService;
    private final AssetPriceService assetPriceService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Analisa um ativo e determina se deve ser mantido ou substituído
     */
    @Transactional
    public PortfolioAnalysis analyzeAsset(RecommendedAsset asset, String portfolioName) {
        log.info("Iniciando análise de revisão para {} na carteira {}", asset.getTicker(), portfolioName);

        // Desativar análises anteriores deste ticker
        analysisRepository.deactivatePreviousAnalyses(asset.getTicker());

        // Buscar preço atual
        Double currentPrice = null;
        if ("Ação".equals(asset.getType()) || "FII".equals(asset.getType()) || "ETF".equals(asset.getType())) {
            currentPrice = assetPriceService.getBrazilianAssetPrice(asset.getTicker());
        } else if ("Cripto".equals(asset.getType())) {
            currentPrice = assetPriceService.getCryptoPrice(asset.getTicker());
        }

        // Buscar contexto econômico
        EconomicContextResponse economicContext = null;
        try {
            economicContext = economicDataService.fetchEconomicContext();
        } catch (Exception e) {
            log.warn("Erro ao buscar contexto econômico: {}", e.getMessage());
        }

        // Tentar análise com IA
        String apiKey = configurationService.getOpenAIKey();
        PortfolioAnalysis analysis;

        if (apiKey != null && !apiKey.isEmpty()) {
            try {
                analysis = generateAIReview(asset, portfolioName, currentPrice, economicContext, apiKey);
            } catch (Exception e) {
                log.error("Erro ao gerar análise IA: {}", e.getMessage());
                analysis = generateBasicReview(asset, portfolioName, currentPrice);
            }
        } else {
            analysis = generateBasicReview(asset, portfolioName, currentPrice);
        }

        // Salvar análise
        return analysisRepository.save(analysis);
    }

    private PortfolioAnalysis generateAIReview(
            RecommendedAsset asset,
            String portfolioName,
            Double currentPrice,
            EconomicContextResponse economicContext,
            String apiKey) {

        ChatLanguageModel chatModel = chatModelFactory.createChatModel(apiKey);
        String prompt = buildReviewPrompt(asset, portfolioName, currentPrice, economicContext);

        log.debug("Prompt para revisão: {}", prompt);
        String aiResponse = chatModel.generate(prompt);
        log.debug("Resposta da IA: {}", aiResponse);

        return parseAIReviewResponse(aiResponse, asset, portfolioName, currentPrice);
    }

    private String buildReviewPrompt(
            RecommendedAsset asset,
            String portfolioName,
            Double currentPrice,
            EconomicContextResponse economicContext) {

        Double selic = economicContext != null && economicContext.getSelic() != null
                ? economicContext.getSelic().getValue() : 13.25;
        Double ipca = economicContext != null && economicContext.getIpca() != null
                ? economicContext.getIpca().getValue() : 4.5;

        String priceStatus = "N/A";
        if (currentPrice != null && asset.getCeilingPrice() != null) {
            double percentDiff = ((currentPrice - asset.getCeilingPrice()) / asset.getCeilingPrice()) * 100;
            if (percentDiff > 20) {
                priceStatus = String.format("MUITO ACIMA do teto (%.1f%% acima)", percentDiff);
            } else if (percentDiff > 0) {
                priceStatus = String.format("Acima do teto (%.1f%% acima)", percentDiff);
            } else if (percentDiff > -10) {
                priceStatus = String.format("Próximo do teto (%.1f%% abaixo)", Math.abs(percentDiff));
            } else {
                priceStatus = String.format("BOM PREÇO (%.1f%% abaixo do teto)", Math.abs(percentDiff));
            }
        }

        return String.format("""
            Você é um analista de investimentos brasileiro certificado (CNPI).
            Faça uma REVISÃO PERIÓDICA deste ativo para determinar se deve continuar na carteira.

            DADOS DO ATIVO:
            - Ticker: %s
            - Nome: %s
            - Tipo: %s
            - Carteira: %s
            - Preço Atual: R$ %.2f
            - Preço-Teto Original: R$ %.2f
            - Status do Preço: %s
            - DY Esperado: %s
            - Rationale Original: %s

            CONTEXTO ECONÔMICO BRASILEIRO:
            - Taxa SELIC: %.2f%% a.a.
            - IPCA (12 meses): %.2f%%

            CRITÉRIOS DE AVALIAÇÃO:
            1. O ativo ainda faz sentido para esta carteira?
            2. Os fundamentos da empresa/fundo mudaram significativamente?
            3. Existem alternativas melhores no mercado?
            4. O preço atual justifica manter ou é melhor substituir?

            Responda APENAS com um JSON válido (sem markdown):
            {
              "recommendation": "MANTER" ou "SUBSTITUIR" ou "OBSERVAR",
              "confidenceScore": 0-100,
              "analysisText": "Análise detalhada de 2-3 parágrafos explicando sua recomendação",
              "substitutionSuggestion": "Se SUBSTITUIR, sugira um ativo alternativo com ticker e justificativa. Se MANTER ou OBSERVAR, deixe null",
              "keyFactors": ["fator 1", "fator 2", "fator 3"]
            }
            """,
                asset.getTicker(),
                asset.getName(),
                asset.getType(),
                portfolioName,
                currentPrice != null ? currentPrice : 0.0,
                asset.getCeilingPrice() != null ? asset.getCeilingPrice() : 0.0,
                priceStatus,
                asset.getExpectedDY() != null ? String.format("%.1f%%", asset.getExpectedDY()) : "N/A",
                asset.getRationale() != null ? asset.getRationale() : "N/A",
                selic,
                ipca
        );
    }

    private PortfolioAnalysis parseAIReviewResponse(
            String aiResponse,
            RecommendedAsset asset,
            String portfolioName,
            Double currentPrice) {

        try {
            String cleanedResponse = aiResponse
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();

            JsonNode root = objectMapper.readTree(cleanedResponse);

            String recommendation = root.has("recommendation")
                    ? root.get("recommendation").asText().toUpperCase()
                    : "OBSERVAR";

            // Validar recomendação
            if (!recommendation.equals("MANTER") && !recommendation.equals("SUBSTITUIR") && !recommendation.equals("OBSERVAR")) {
                recommendation = "OBSERVAR";
            }

            int confidenceScore = root.has("confidenceScore")
                    ? root.get("confidenceScore").asInt()
                    : 50;

            String analysisText = root.has("analysisText")
                    ? root.get("analysisText").asText()
                    : "Análise não disponível";

            String substitutionSuggestion = null;
            if (root.has("substitutionSuggestion") && !root.get("substitutionSuggestion").isNull()) {
                substitutionSuggestion = root.get("substitutionSuggestion").asText();
            }

            return PortfolioAnalysis.builder()
                    .portfolioName(portfolioName)
                    .ticker(asset.getTicker())
                    .assetName(asset.getName())
                    .assetType(asset.getType())
                    .currentPrice(currentPrice)
                    .ceilingPrice(asset.getCeilingPrice())
                    .recommendation(recommendation)
                    .analysisText(analysisText)
                    .substitutionSuggestion(substitutionSuggestion)
                    .confidenceScore(confidenceScore)
                    .analysisDate(LocalDateTime.now())
                    .nextReviewDate(LocalDateTime.now().plusDays(10))
                    .isActive(true)
                    .build();

        } catch (Exception e) {
            log.error("Erro ao parsear resposta da IA: {}", e.getMessage());
            return generateBasicReview(asset, portfolioName, currentPrice);
        }
    }

    private PortfolioAnalysis generateBasicReview(
            RecommendedAsset asset,
            String portfolioName,
            Double currentPrice) {

        String recommendation = "MANTER";
        String analysisText = "Análise básica sem IA. ";

        // Lógica simples baseada no preço
        if (currentPrice != null && asset.getCeilingPrice() != null) {
            double percentAbove = ((currentPrice - asset.getCeilingPrice()) / asset.getCeilingPrice()) * 100;

            if (percentAbove > 30) {
                recommendation = "SUBSTITUIR";
                analysisText += String.format(
                        "O ativo %s está %.1f%% acima do preço-teto, indicando que pode não ser mais uma boa oportunidade. " +
                        "Considere buscar alternativas no mesmo setor com melhor relação risco/retorno.",
                        asset.getTicker(), percentAbove);
            } else if (percentAbove > 15) {
                recommendation = "OBSERVAR";
                analysisText += String.format(
                        "O ativo %s está %.1f%% acima do preço-teto. " +
                        "Ainda pode ser mantido, mas monitore de perto para eventual correção.",
                        asset.getTicker(), percentAbove);
            } else {
                recommendation = "MANTER";
                analysisText += String.format(
                        "O ativo %s está em um nível de preço aceitável. " +
                        "Continua sendo uma boa opção para a carteira %s.",
                        asset.getTicker(), portfolioName);
            }
        } else {
            analysisText += "Não foi possível obter o preço atual. Recomenda-se manter e monitorar.";
        }

        return PortfolioAnalysis.builder()
                .portfolioName(portfolioName)
                .ticker(asset.getTicker())
                .assetName(asset.getName())
                .assetType(asset.getType())
                .currentPrice(currentPrice)
                .ceilingPrice(asset.getCeilingPrice())
                .recommendation(recommendation)
                .analysisText(analysisText)
                .substitutionSuggestion(null)
                .confidenceScore(60)
                .analysisDate(LocalDateTime.now())
                .nextReviewDate(LocalDateTime.now().plusDays(10))
                .isActive(true)
                .build();
    }

    /**
     * Buscar última análise de um ticker
     */
    public Optional<PortfolioAnalysis> getLatestAnalysis(String ticker) {
        return analysisRepository.findByTickerAndIsActiveTrue(ticker);
    }

    /**
     * Buscar todas as análises ativas de uma carteira
     */
    public List<PortfolioAnalysis> getPortfolioAnalyses(String portfolioName) {
        return analysisRepository.findByPortfolioNameAndIsActiveTrueOrderByAnalysisDateDesc(portfolioName);
    }

    /**
     * Buscar análises que precisam de revisão
     */
    public List<PortfolioAnalysis> getPendingReviews() {
        return analysisRepository.findByIsActiveTrueAndNextReviewDateLessThanEqual(LocalDateTime.now());
    }

    /**
     * Buscar análises com recomendação de substituição
     */
    public List<PortfolioAnalysis> getSubstitutionRecommendations() {
        return analysisRepository.findByRecommendationAndIsActiveTrueOrderByAnalysisDateDesc("SUBSTITUIR");
    }

    /**
     * Buscar histórico de análises de um ticker
     */
    public List<PortfolioAnalysis> getAnalysisHistory(String ticker) {
        return analysisRepository.findByTickerOrderByAnalysisDateDesc(ticker);
    }
}
