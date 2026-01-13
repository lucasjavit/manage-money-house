package com.managehouse.money.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.managehouse.money.config.ChatModelFactory;
import com.managehouse.money.dto.InvestmentPortfolio;
import com.managehouse.money.dto.PortfolioAsset;
import com.managehouse.money.dto.RecommendedAsset;
import com.managehouse.money.entity.UserPortfolio;
import com.managehouse.money.repository.UserPortfolioRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PersonalizedPortfolioService {

    private final MarketDataService marketDataService;
    private final AssetPriceService assetPriceService;
    private final ChatModelFactory chatModelFactory;
    private final ConfigurationService configurationService;
    private final UserPortfolioRepository userPortfolioRepository;

    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Gera uma carteira personalizada baseada no perfil de risco
     */
    @Transactional
    public InvestmentPortfolio generatePortfolio(String riskProfile, Long userId) {
        log.info("Gerando carteira personalizada para usuario {} com perfil {}", userId, riskProfile);

        // 1. Buscar todos os ativos de todas as carteiras
        List<RecommendedAsset> allAssets = getAllAssetsFromPortfolios();
        log.info("Total de ativos encontrados: {}", allAssets.size());

        // 2. Enriquecer com precos atuais
        assetPriceService.enrichWithPrices(allAssets);

        // 3. Filtrar ativos com bom preco (preco atual <= preco teto) ou sem preco definido
        List<RecommendedAsset> eligibleAssets = filterEligibleAssets(allAssets);
        log.info("Ativos elegiveis (bom preco): {}", eligibleAssets.size());

        // 4. Tentar usar IA para selecionar os melhores ativos
        String apiKey = configurationService.getOpenAIKey();
        InvestmentPortfolio portfolio;

        if (apiKey != null && !apiKey.isEmpty()) {
            try {
                portfolio = generateWithAI(eligibleAssets, riskProfile, apiKey);
            } catch (Exception e) {
                log.error("Erro ao gerar carteira com IA: {}", e.getMessage());
                portfolio = generateBasicPortfolio(eligibleAssets, riskProfile);
            }
        } else {
            log.warn("Chave OpenAI nao configurada. Gerando carteira basica.");
            portfolio = generateBasicPortfolio(eligibleAssets, riskProfile);
        }

        // 5. Salvar no banco de dados
        saveUserPortfolio(userId, riskProfile, portfolio);

        return portfolio;
    }

    /**
     * Busca a carteira salva do usuario
     */
    public Optional<InvestmentPortfolio> getUserPortfolio(Long userId) {
        return userPortfolioRepository.findByUserId(userId)
                .map(this::convertToInvestmentPortfolio);
    }

    private List<RecommendedAsset> getAllAssetsFromPortfolios() {
        List<InvestmentPortfolio> portfolios = marketDataService.getPortfolios();
        List<RecommendedAsset> allAssets = new ArrayList<>();

        for (InvestmentPortfolio portfolio : portfolios) {
            if (portfolio.getRecommendedAssets() != null) {
                for (RecommendedAsset asset : portfolio.getRecommendedAssets()) {
                    // Evitar duplicatas
                    boolean exists = allAssets.stream()
                            .anyMatch(a -> a.getTicker().equals(asset.getTicker()));
                    if (!exists) {
                        allAssets.add(asset);
                    }
                }
            }
        }

        return allAssets;
    }

    private List<RecommendedAsset> filterEligibleAssets(List<RecommendedAsset> assets) {
        return assets.stream()
                .filter(asset -> {
                    // Incluir ativos sem preco definido (renda fixa, etc.)
                    if (asset.getCurrentPrice() == null || asset.getCeilingPrice() == null) {
                        return true;
                    }
                    // Incluir ativos com preco atual <= preco teto (bom para comprar)
                    return asset.getCurrentPrice() <= asset.getCeilingPrice();
                })
                .collect(Collectors.toList());
    }

    private InvestmentPortfolio generateWithAI(List<RecommendedAsset> assets, String riskProfile, String apiKey) {
        ChatLanguageModel chatModel = chatModelFactory.createChatModel(apiKey);

        String prompt = buildSelectionPrompt(assets, riskProfile);
        log.debug("Prompt para IA: {}", prompt);

        String aiResponse = chatModel.generate(prompt);
        log.debug("Resposta da IA: {}", aiResponse);

        return parseAIResponse(aiResponse, assets, riskProfile);
    }

    private String buildSelectionPrompt(List<RecommendedAsset> assets, String profile) {
        StringBuilder assetsList = new StringBuilder();
        for (RecommendedAsset asset : assets) {
            String priceInfo = asset.getCurrentPrice() != null
                    ? String.format("R$ %.2f (teto: R$ %.2f)", asset.getCurrentPrice(), asset.getCeilingPrice())
                    : "N/A";
            String dyInfo = asset.getExpectedDY() != null
                    ? String.format("%.1f%%", asset.getExpectedDY())
                    : "N/A";

            assetsList.append(String.format(
                    "- %s (%s) | Tipo: %s | Preco: %s | DY: %s | %s%n",
                    asset.getTicker(),
                    asset.getName(),
                    asset.getType(),
                    priceInfo,
                    dyInfo,
                    asset.getRationale() != null ? asset.getRationale() : ""
            ));
        }

        return String.format("""
            Voce e um consultor de investimentos brasileiro certificado (CNPI).

            Monte uma carteira de investimentos com perfil %s usando os MELHORES ativos da lista abaixo.

            REGRAS POR PERFIL:
            - CONSERVADOR: 60%% FIIs + 30%% Acoes de Dividendos + 10%% Renda Fixa (8-10 ativos total)
            - MODERADO: 40%% Acoes de Valor + 30%% FIIs + 20%% Acoes de Dividendos + 10%% Cripto (10-12 ativos total)
            - ARROJADO: 40%% Acoes + 25%% Small Caps + 20%% Cripto + 15%% ETFs Internacionais (10-12 ativos total)

            CRITERIOS DE SELECAO (em ordem de prioridade):
            1. Priorizar ativos com preco atual ABAIXO do teto
            2. Maior Dividend Yield esperado (para FIIs e acoes de dividendos)
            3. Diversificacao setorial (nao concentrar em um setor)
            4. Fundamentos solidos (conforme rationale)

            ATIVOS DISPONIVEIS:
            %s

            IMPORTANTE: Responda APENAS com JSON valido, sem markdown:
            {
              "selectedAssets": [
                {"ticker": "TICKER1", "allocation": 10, "reason": "Motivo da selecao"},
                {"ticker": "TICKER2", "allocation": 10, "reason": "Motivo da selecao"}
              ],
              "portfolioRationale": "Explicacao geral de 2-3 frases sobre a estrategia da carteira",
              "expectedDY": 8.5,
              "riskAssessment": "Avaliacao de risco em 1-2 frases"
            }
            """, profile, assetsList.toString());
    }

    private InvestmentPortfolio parseAIResponse(String aiResponse, List<RecommendedAsset> allAssets, String riskProfile) {
        try {
            String cleanedResponse = aiResponse
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();

            JsonNode root = objectMapper.readTree(cleanedResponse);

            // Parsear ativos selecionados
            List<RecommendedAsset> selectedAssets = new ArrayList<>();
            JsonNode assetsNode = root.get("selectedAssets");

            if (assetsNode != null && assetsNode.isArray()) {
                int rank = 1;
                for (JsonNode assetNode : assetsNode) {
                    String ticker = assetNode.get("ticker").asText();
                    double allocation = assetNode.get("allocation").asDouble();
                    String reason = assetNode.has("reason") ? assetNode.get("reason").asText() : "";

                    // Buscar ativo completo da lista original
                    RecommendedAsset original = allAssets.stream()
                            .filter(a -> a.getTicker().equalsIgnoreCase(ticker))
                            .findFirst()
                            .orElse(null);

                    if (original != null) {
                        RecommendedAsset selected = RecommendedAsset.builder()
                                .rank(rank++)
                                .ticker(original.getTicker())
                                .name(original.getName())
                                .type(original.getType())
                                .expectedDY(original.getExpectedDY())
                                .entryPrice(original.getEntryPrice())
                                .currentPrice(original.getCurrentPrice())
                                .ceilingPrice(original.getCeilingPrice())
                                .targetAllocation(allocation)
                                .bias(original.getBias())
                                .rationale(reason.isEmpty() ? original.getRationale() : reason)
                                .build();
                        selectedAssets.add(selected);
                    }
                }
            }

            String portfolioRationale = root.has("portfolioRationale")
                    ? root.get("portfolioRationale").asText()
                    : "Carteira montada automaticamente.";

            Double expectedDY = root.has("expectedDY")
                    ? root.get("expectedDY").asDouble()
                    : null;

            String riskAssessment = root.has("riskAssessment")
                    ? root.get("riskAssessment").asText()
                    : "";

            // Montar composicao sugerida baseada no perfil
            List<PortfolioAsset> suggestedComposition = getSuggestedComposition(riskProfile);

            return InvestmentPortfolio.builder()
                    .name("Minha Carteira")
                    .description(portfolioRationale)
                    .strategy(riskAssessment)
                    .riskLevel(formatRiskLevel(riskProfile))
                    .icon("👤")
                    .suggestedComposition(suggestedComposition)
                    .recommendedAssets(selectedAssets)
                    .characteristics(getCharacteristics(riskProfile))
                    .build();

        } catch (Exception e) {
            log.error("Erro ao parsear resposta da IA: {}", e.getMessage());
            return generateBasicPortfolio(allAssets, riskProfile);
        }
    }

    private InvestmentPortfolio generateBasicPortfolio(List<RecommendedAsset> assets, String riskProfile) {
        List<RecommendedAsset> selectedAssets = new ArrayList<>();

        // Agrupar por tipo
        Map<String, List<RecommendedAsset>> byType = assets.stream()
                .collect(Collectors.groupingBy(RecommendedAsset::getType));

        // Selecionar baseado no perfil
        switch (riskProfile.toUpperCase()) {
            case "CONSERVADOR":
                addAssetsByType(selectedAssets, byType, "FII", 6, 10);
                addAssetsByType(selectedAssets, byType, "Acao", 3, 10);
                addAssetsByType(selectedAssets, byType, "Renda Fixa", 1, 10);
                break;
            case "MODERADO":
                addAssetsByType(selectedAssets, byType, "Acao", 4, 10);
                addAssetsByType(selectedAssets, byType, "FII", 3, 10);
                addAssetsByType(selectedAssets, byType, "Acao", 2, 10);
                addAssetsByType(selectedAssets, byType, "Cripto", 1, 10);
                break;
            case "ARROJADO":
                addAssetsByType(selectedAssets, byType, "Acao", 4, 10);
                addAssetsByType(selectedAssets, byType, "Acao", 3, 8);
                addAssetsByType(selectedAssets, byType, "Cripto", 2, 10);
                addAssetsByType(selectedAssets, byType, "ETF", 2, 7);
                break;
            default:
                addAssetsByType(selectedAssets, byType, "Acao", 5, 10);
                addAssetsByType(selectedAssets, byType, "FII", 3, 10);
                addAssetsByType(selectedAssets, byType, "Cripto", 2, 10);
        }

        // Renumerar ranks
        for (int i = 0; i < selectedAssets.size(); i++) {
            selectedAssets.get(i).setRank(i + 1);
        }

        return InvestmentPortfolio.builder()
                .name("Minha Carteira")
                .description("Carteira montada automaticamente baseada no seu perfil de risco.")
                .strategy("Selecao baseada em precos atrativos e diversificacao.")
                .riskLevel(formatRiskLevel(riskProfile))
                .icon("👤")
                .suggestedComposition(getSuggestedComposition(riskProfile))
                .recommendedAssets(selectedAssets)
                .characteristics(getCharacteristics(riskProfile))
                .build();
    }

    private void addAssetsByType(List<RecommendedAsset> selected, Map<String, List<RecommendedAsset>> byType,
                                  String type, int count, double allocation) {
        List<RecommendedAsset> assetsOfType = byType.getOrDefault(type, Collections.emptyList());

        // Ordenar por melhor relacao preco/teto (menor = melhor)
        List<RecommendedAsset> sorted = assetsOfType.stream()
                .sorted((a, b) -> {
                    if (a.getCurrentPrice() == null || a.getCeilingPrice() == null) return 1;
                    if (b.getCurrentPrice() == null || b.getCeilingPrice() == null) return -1;
                    double ratioA = a.getCurrentPrice() / a.getCeilingPrice();
                    double ratioB = b.getCurrentPrice() / b.getCeilingPrice();
                    return Double.compare(ratioA, ratioB);
                })
                .limit(count)
                .collect(Collectors.toList());

        for (RecommendedAsset asset : sorted) {
            if (!selected.stream().anyMatch(s -> s.getTicker().equals(asset.getTicker()))) {
                RecommendedAsset copy = RecommendedAsset.builder()
                        .rank(selected.size() + 1)
                        .ticker(asset.getTicker())
                        .name(asset.getName())
                        .type(asset.getType())
                        .expectedDY(asset.getExpectedDY())
                        .entryPrice(asset.getEntryPrice())
                        .currentPrice(asset.getCurrentPrice())
                        .ceilingPrice(asset.getCeilingPrice())
                        .targetAllocation(allocation)
                        .bias(asset.getBias())
                        .rationale(asset.getRationale())
                        .build();
                selected.add(copy);
            }
        }
    }

    private List<PortfolioAsset> getSuggestedComposition(String riskProfile) {
        switch (riskProfile.toUpperCase()) {
            case "CONSERVADOR":
                return Arrays.asList(
                        PortfolioAsset.builder().type("FIIs").percentage(60).description("Fundos Imobiliarios").build(),
                        PortfolioAsset.builder().type("Acoes").percentage(30).description("Acoes de Dividendos").build(),
                        PortfolioAsset.builder().type("Renda Fixa").percentage(10).description("Tesouro/CDBs").build()
                );
            case "MODERADO":
                return Arrays.asList(
                        PortfolioAsset.builder().type("Acoes").percentage(40).description("Acoes de Valor").build(),
                        PortfolioAsset.builder().type("FIIs").percentage(30).description("Fundos Imobiliarios").build(),
                        PortfolioAsset.builder().type("Dividendos").percentage(20).description("Acoes de Dividendos").build(),
                        PortfolioAsset.builder().type("Cripto").percentage(10).description("Criptomoedas").build()
                );
            case "ARROJADO":
                return Arrays.asList(
                        PortfolioAsset.builder().type("Acoes").percentage(40).description("Acoes Crescimento").build(),
                        PortfolioAsset.builder().type("Small Caps").percentage(25).description("Empresas Menores").build(),
                        PortfolioAsset.builder().type("Cripto").percentage(20).description("Criptomoedas").build(),
                        PortfolioAsset.builder().type("Internacional").percentage(15).description("ETFs Globais").build()
                );
            default:
                return Arrays.asList(
                        PortfolioAsset.builder().type("Acoes").percentage(50).description("Acoes").build(),
                        PortfolioAsset.builder().type("FIIs").percentage(30).description("FIIs").build(),
                        PortfolioAsset.builder().type("Cripto").percentage(20).description("Cripto").build()
                );
        }
    }

    private List<String> getCharacteristics(String riskProfile) {
        switch (riskProfile.toUpperCase()) {
            case "CONSERVADOR":
                return Arrays.asList(
                        "Foco em renda passiva",
                        "Baixa volatilidade",
                        "Dividendos mensais"
                );
            case "MODERADO":
                return Arrays.asList(
                        "Equilibrio crescimento e renda",
                        "Diversificacao moderada",
                        "Volatilidade controlada"
                );
            case "ARROJADO":
                return Arrays.asList(
                        "Alto potencial de valorizacao",
                        "Maior volatilidade",
                        "Exposicao a ativos de risco"
                );
            default:
                return Arrays.asList("Carteira personalizada");
        }
    }

    private String formatRiskLevel(String riskProfile) {
        switch (riskProfile.toUpperCase()) {
            case "CONSERVADOR":
                return "Baixo";
            case "MODERADO":
                return "Moderado";
            case "ARROJADO":
                return "Alto";
            default:
                return riskProfile;
        }
    }

    private void saveUserPortfolio(Long userId, String riskProfile, InvestmentPortfolio portfolio) {
        try {
            // Deletar carteira anterior se existir
            userPortfolioRepository.findByUserId(userId)
                    .ifPresent(existing -> userPortfolioRepository.delete(existing));

            // Converter ativos para JSON
            String assetsJson = objectMapper.writeValueAsString(portfolio.getRecommendedAssets());

            UserPortfolio userPortfolio = UserPortfolio.builder()
                    .userId(userId)
                    .riskProfile(riskProfile)
                    .assetsJson(assetsJson)
                    .aiRationale(portfolio.getDescription())
                    .riskAssessment(portfolio.getStrategy())
                    .build();

            userPortfolioRepository.save(userPortfolio);
            log.info("Carteira salva para usuario {}", userId);

        } catch (Exception e) {
            log.error("Erro ao salvar carteira: {}", e.getMessage());
        }
    }

    private InvestmentPortfolio convertToInvestmentPortfolio(UserPortfolio userPortfolio) {
        try {
            List<RecommendedAsset> assets = objectMapper.readValue(
                    userPortfolio.getAssetsJson(),
                    new TypeReference<List<RecommendedAsset>>() {}
            );

            // Atualizar precos
            assetPriceService.enrichWithPrices(assets);

            return InvestmentPortfolio.builder()
                    .name("Minha Carteira")
                    .description(userPortfolio.getAiRationale())
                    .strategy(userPortfolio.getRiskAssessment())
                    .riskLevel(formatRiskLevel(userPortfolio.getRiskProfile()))
                    .icon("👤")
                    .suggestedComposition(getSuggestedComposition(userPortfolio.getRiskProfile()))
                    .recommendedAssets(assets)
                    .characteristics(getCharacteristics(userPortfolio.getRiskProfile()))
                    .build();

        } catch (Exception e) {
            log.error("Erro ao converter UserPortfolio: {}", e.getMessage());
            return null;
        }
    }
}
