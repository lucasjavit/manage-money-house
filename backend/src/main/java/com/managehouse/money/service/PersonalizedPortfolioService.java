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
        String apiKey = configurationService.getActiveProviderKey();
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
            Voce raciocina como um investidor de VALOR na linha de Warren Buffett e Benjamin Graham.
            Filosofia (aplique como criterio central):
            - MARGEM DE SEGURANCA: prefira ativos com preco atual bem ABAIXO do valor justo/teto.
            - QUALIDADE E PREVISIBILIDADE: empresas/FIIs com fundamentos solidos, historico consistente,
              baixo endividamento e vantagem competitiva duravel ("moat").
            - LONGO PRAZO: carteira para segurar por anos, nao para especular.
            - CIRCULO DE COMPETENCIA: evite o que e complexo/imprevisivel demais; peso baixo em
              ativos especulativos (cripto, small caps sem lucro) mesmo no perfil arrojado.
            - "Preco e o que voce paga, valor e o que voce leva": DY alto so importa se for sustentavel.

            Monte uma carteira com perfil %s usando os MELHORES ativos da lista abaixo, sob essa filosofia.

            ALOCACAO POR PERFIL (inclua SEMPRE uma fatia de Renda Fixa):
            - CONSERVADOR: 50%% Renda Fixa + 30%% FIIs + 20%% Acoes de Dividendos de qualidade.
            - MODERADO: 30%% Renda Fixa + 30%% Acoes de Valor + 25%% FIIs + 15%% Acoes de Dividendos.
            - ARROJADO: 15%% Renda Fixa + 40%% Acoes de Valor + 20%% FIIs + 15%% Small Caps de qualidade + 10%% Internacional.

            RENDA FIXA (a fatia acima): distribua entre os tipos e explique a logica no reason:
            - Tesouro Selic: reserva de oportunidade e liquidez (parte da fatia).
            - Tesouro IPCA+ / CDB IPCA+: proteger poder de compra no longo prazo.
            - CDB/LCI/LCA (% do CDI): renda previsivel; LCI/LCA isentas de IR.
            Represente a renda fixa como itens com ticker "RENDA-FIXA" (ou o ticker do ativo de RF se houver na lista),
            somando ao percentual do perfil.

            CRITERIOS DE SELECAO (ordem de prioridade):
            1. Margem de seguranca: preco atual ABAIXO do teto (descarte os muito acima do teto).
            2. Qualidade dos fundamentos (conforme rationale) e previsibilidade.
            3. Diversificacao setorial (nao concentrar).
            4. DY sustentavel para a parte de dividendos/FIIs.

            ATIVOS DISPONIVEIS:
            %s

            IMPORTANTE: os percentuais de selectedAssets DEVEM somar 100. Responda APENAS com JSON valido, sem markdown:
            {
              "selectedAssets": [
                {"ticker": "RENDA-FIXA", "allocation": 50, "reason": "Tesouro Selic (reserva) + IPCA+ (longo prazo)"},
                {"ticker": "TICKER1", "allocation": 10, "reason": "Margem de seguranca: preco abaixo do teto, fundamentos solidos"}
              ],
              "portfolioRationale": "Explicacao de 2-3 frases sob a otica de value investing (margem de seguranca, qualidade, longo prazo)",
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

        // Agrupar por tipo NORMALIZADO: as carteiras estáticas usam variações
        // ("Ação", "Ações", "FIIs", "Tesouro", "CDBs"...) que não casavam com as
        // chaves do switch e deixavam a carteira sem ações.
        Map<String, List<RecommendedAsset>> byType = assets.stream()
                .collect(Collectors.groupingBy(a -> normalizeAssetType(a.getType())));

        // Selecao value investing: renda fixa em TODOS os perfis, pouco especulativo.
        switch (riskProfile.toUpperCase()) {
            // ~12 ativos: diversificacao suficiente sem virar indice (consenso 12-18 papeis).
            case "CONSERVADOR":
                addAssetsByType(selectedAssets, byType, "Renda Fixa", 3, 17); // ~50%
                addAssetsByType(selectedAssets, byType, "FII", 5, 6);          // ~30%
                addAssetsByType(selectedAssets, byType, "Acao", 4, 5);         // ~20%
                break;
            case "MODERADO":
                addAssetsByType(selectedAssets, byType, "Renda Fixa", 2, 15);  // 30%
                addAssetsByType(selectedAssets, byType, "Acao", 5, 9);         // ~45% (valor+dividendos)
                addAssetsByType(selectedAssets, byType, "FII", 4, 6);          // ~25%
                break;
            case "ARROJADO":
                addAssetsByType(selectedAssets, byType, "Renda Fixa", 1, 15);  // 15%
                addAssetsByType(selectedAssets, byType, "Acao", 6, 8);         // ~48%
                addAssetsByType(selectedAssets, byType, "FII", 3, 7);          // ~21%
                addAssetsByType(selectedAssets, byType, "ETF", 2, 8);          // ~16%
                break;
            default:
                addAssetsByType(selectedAssets, byType, "Renda Fixa", 2, 15);
                addAssetsByType(selectedAssets, byType, "Acao", 6, 9);
                addAssetsByType(selectedAssets, byType, "FII", 4, 6);
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

    /** Mapeia as variações de tipo das carteiras estáticas para as chaves canônicas do fallback. */
    private String normalizeAssetType(String type) {
        if (type == null) return "Outro";
        String t = java.text.Normalizer.normalize(type, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")   // remove acentos
                .toLowerCase().trim();
        if (t.startsWith("acao") || t.startsWith("acoes") || t.equals("mid caps")) return "Acao";
        if (t.equals("small caps")) return "Small Caps";
        if (t.startsWith("fii")) return "FII";
        if (t.startsWith("etf") || t.equals("reits")) return "ETF";
        if (t.startsWith("cripto") || t.equals("bitcoin") || t.equals("ethereum")
                || t.startsWith("altcoin") || t.startsWith("stablecoin")) return "Cripto";
        if (t.equals("renda fixa") || t.equals("tesouro") || t.startsWith("cdb") || t.startsWith("lci")) return "Renda Fixa";
        return type;
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
                        PortfolioAsset.builder().type("Renda Fixa").percentage(50).description("Tesouro Selic/IPCA+, CDB, LCI/LCA").build(),
                        PortfolioAsset.builder().type("FIIs").percentage(30).description("Fundos Imobiliarios de qualidade").build(),
                        PortfolioAsset.builder().type("Dividendos").percentage(20).description("Acoes pagadoras consistentes").build()
                );
            case "MODERADO":
                return Arrays.asList(
                        PortfolioAsset.builder().type("Renda Fixa").percentage(30).description("Tesouro IPCA+, CDB, LCI/LCA").build(),
                        PortfolioAsset.builder().type("Acoes").percentage(30).description("Acoes de Valor (margem de seguranca)").build(),
                        PortfolioAsset.builder().type("FIIs").percentage(25).description("Fundos Imobiliarios").build(),
                        PortfolioAsset.builder().type("Dividendos").percentage(15).description("Acoes de Dividendos").build()
                );
            case "ARROJADO":
                return Arrays.asList(
                        PortfolioAsset.builder().type("Renda Fixa").percentage(15).description("Tesouro Selic (reserva de oportunidade)").build(),
                        PortfolioAsset.builder().type("Acoes").percentage(40).description("Acoes de Valor").build(),
                        PortfolioAsset.builder().type("FIIs").percentage(20).description("Fundos Imobiliarios").build(),
                        PortfolioAsset.builder().type("Small Caps").percentage(15).description("Empresas menores de qualidade").build(),
                        PortfolioAsset.builder().type("Internacional").percentage(10).description("ETFs Globais").build()
                );
            default:
                return Arrays.asList(
                        PortfolioAsset.builder().type("Renda Fixa").percentage(30).description("Tesouro/CDB").build(),
                        PortfolioAsset.builder().type("Acoes").percentage(45).description("Acoes de Valor").build(),
                        PortfolioAsset.builder().type("FIIs").percentage(25).description("FIIs").build()
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
