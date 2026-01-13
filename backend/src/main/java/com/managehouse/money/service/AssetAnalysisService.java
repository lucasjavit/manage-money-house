package com.managehouse.money.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.managehouse.money.config.ChatModelFactory;
import com.managehouse.money.dto.AssetAnalysisResponse;
import com.managehouse.money.dto.EconomicContextResponse;
import com.managehouse.money.dto.RecommendedAsset;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssetAnalysisService {

    private final ChatModelFactory chatModelFactory;
    private final ConfigurationService configurationService;
    private final EconomicDataService economicDataService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Analisa um ativo específico usando IA
     */
    public AssetAnalysisResponse analyzeAsset(RecommendedAsset asset, String portfolioType) {
        log.info("Analisando ativo {} para carteira {}", asset.getTicker(), portfolioType);

        // Buscar contexto econômico
        EconomicContextResponse economicContext = null;
        try {
            economicContext = economicDataService.fetchEconomicContext();
        } catch (Exception e) {
            log.warn("Erro ao buscar contexto econômico: {}", e.getMessage());
        }

        // Tentar análise com IA
        String apiKey = configurationService.getOpenAIKey();
        if (apiKey != null && !apiKey.isEmpty()) {
            try {
                return generateAIAnalysis(asset, portfolioType, economicContext, apiKey);
            } catch (Exception e) {
                log.error("Erro ao gerar análise IA: {}", e.getMessage());
            }
        }

        // Fallback: análise básica sem IA
        return generateBasicAnalysis(asset, portfolioType, economicContext);
    }

    private AssetAnalysisResponse generateAIAnalysis(
            RecommendedAsset asset,
            String portfolioType,
            EconomicContextResponse economicContext,
            String apiKey) {

        ChatLanguageModel chatModel = chatModelFactory.createChatModel(apiKey);

        String prompt = buildAnalysisPrompt(asset, portfolioType, economicContext);
        log.debug("Prompt para análise: {}", prompt);

        String aiResponse = chatModel.generate(prompt);
        log.debug("Resposta da IA: {}", aiResponse);

        return parseAIResponse(aiResponse, asset, economicContext);
    }

    private String buildAnalysisPrompt(
            RecommendedAsset asset,
            String portfolioType,
            EconomicContextResponse economicContext) {

        Double selic = economicContext != null && economicContext.getSelic() != null
                ? economicContext.getSelic().getValue() : 13.25;
        Double ipca = economicContext != null && economicContext.getIpca() != null
                ? economicContext.getIpca().getValue() : 4.5;

        return String.format("""
            Você é um analista de investimentos brasileiro experiente e certificado (CEA/CNPI).
            Analise o seguinte ativo de forma profissional e didática para um investidor pessoa física.

            DADOS DO ATIVO:
            - Ticker: %s
            - Nome: %s
            - Tipo: %s
            - Carteira: %s
            - Preço Atual: R$ %.2f
            - Preço-Teto: R$ %.2f
            - Dividend Yield Esperado: %s
            - Viés Atual: %s
            - Análise Base: %s

            CONTEXTO ECONÔMICO BRASILEIRO:
            - Taxa SELIC: %.2f%% a.a.
            - IPCA (12 meses): %.2f%%

            Responda APENAS com um JSON válido (sem markdown, sem comentários) no seguinte formato:
            {
              "aiAnalysis": "Análise detalhada de 2-3 parágrafos explicando a tese de investimento, fundamentos da empresa/ativo, e posição no mercado",
              "investmentThesis": "Resumo em 1-2 frases do principal motivo para investir neste ativo agora",
              "risks": ["risco 1 específico", "risco 2 específico", "risco 3 específico"],
              "shortTermOutlook": "Perspectiva para os próximos 6-12 meses considerando o cenário atual",
              "sectorComparison": "Como este ativo se compara aos pares do mesmo setor",
              "selicImpact": "Positivo/Negativo/Neutro - breve explicação de como a SELIC afeta este ativo",
              "ipcaImpact": "Positivo/Negativo/Neutro - breve explicação de como a inflação afeta este ativo"
            }
            """,
                asset.getTicker(),
                asset.getName(),
                asset.getType(),
                portfolioType,
                asset.getCurrentPrice() != null ? asset.getCurrentPrice() : 0.0,
                asset.getCeilingPrice() != null ? asset.getCeilingPrice() : 0.0,
                asset.getExpectedDY() != null ? String.format("%.1f%%", asset.getExpectedDY()) : "N/A",
                asset.getBias() != null ? asset.getBias() : "N/A",
                asset.getRationale() != null ? asset.getRationale() : "N/A",
                selic,
                ipca
        );
    }

    private AssetAnalysisResponse parseAIResponse(
            String aiResponse,
            RecommendedAsset asset,
            EconomicContextResponse economicContext) {

        try {
            // Limpar resposta (remover markdown se presente)
            String cleanedResponse = aiResponse
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();

            JsonNode root = objectMapper.readTree(cleanedResponse);

            // Extrair riscos
            List<String> risks = new ArrayList<>();
            if (root.has("risks") && root.get("risks").isArray()) {
                for (JsonNode risk : root.get("risks")) {
                    risks.add(risk.asText());
                }
            }

            // Extrair impactos econômicos
            Double selic = economicContext != null && economicContext.getSelic() != null
                    ? economicContext.getSelic().getValue() : null;
            Double ipca = economicContext != null && economicContext.getIpca() != null
                    ? economicContext.getIpca().getValue() : null;

            AssetAnalysisResponse.EconomicImpact economicImpact = AssetAnalysisResponse.EconomicImpact.builder()
                    .selic(selic)
                    .selicImpact(getTextOrDefault(root, "selicImpact", "Não disponível"))
                    .ipca(ipca)
                    .ipcaImpact(getTextOrDefault(root, "ipcaImpact", "Não disponível"))
                    .build();

            return AssetAnalysisResponse.builder()
                    .ticker(asset.getTicker())
                    .name(asset.getName())
                    .type(asset.getType())
                    .currentPrice(asset.getCurrentPrice())
                    .ceilingPrice(asset.getCeilingPrice())
                    .expectedDY(asset.getExpectedDY())
                    .bias(asset.getBias())
                    .rationale(asset.getRationale())
                    .aiAnalysis(getTextOrDefault(root, "aiAnalysis", "Análise não disponível"))
                    .investmentThesis(getTextOrDefault(root, "investmentThesis", "Tese não disponível"))
                    .risks(risks)
                    .shortTermOutlook(getTextOrDefault(root, "shortTermOutlook", "Perspectiva não disponível"))
                    .sectorComparison(getTextOrDefault(root, "sectorComparison", "Comparação não disponível"))
                    .economicImpact(economicImpact)
                    .build();

        } catch (Exception e) {
            log.error("Erro ao parsear resposta da IA: {}", e.getMessage());
            return generateBasicAnalysis(asset, null, economicContext);
        }
    }

    private String getTextOrDefault(JsonNode root, String field, String defaultValue) {
        return root.has(field) ? root.get(field).asText() : defaultValue;
    }

    private AssetAnalysisResponse generateBasicAnalysis(
            RecommendedAsset asset,
            String portfolioType,
            EconomicContextResponse economicContext) {

        Double selic = economicContext != null && economicContext.getSelic() != null
                ? economicContext.getSelic().getValue() : null;
        Double ipca = economicContext != null && economicContext.getIpca() != null
                ? economicContext.getIpca().getValue() : null;

        // Gerar análise básica baseada no tipo de ativo
        String basicAnalysis = generateBasicAnalysisText(asset);
        List<String> basicRisks = generateBasicRisks(asset);
        String selicImpact = generateSelicImpact(asset);
        String ipcaImpact = generateIpcaImpact(asset);

        AssetAnalysisResponse.EconomicImpact economicImpact = AssetAnalysisResponse.EconomicImpact.builder()
                .selic(selic)
                .selicImpact(selicImpact)
                .ipca(ipca)
                .ipcaImpact(ipcaImpact)
                .build();

        return AssetAnalysisResponse.builder()
                .ticker(asset.getTicker())
                .name(asset.getName())
                .type(asset.getType())
                .currentPrice(asset.getCurrentPrice())
                .ceilingPrice(asset.getCeilingPrice())
                .expectedDY(asset.getExpectedDY())
                .bias(asset.getBias())
                .rationale(asset.getRationale())
                .aiAnalysis(basicAnalysis)
                .investmentThesis(asset.getRationale())
                .risks(basicRisks)
                .shortTermOutlook("Análise detalhada requer configuração da API OpenAI.")
                .sectorComparison("Comparação setorial não disponível sem IA.")
                .economicImpact(economicImpact)
                .build();
    }

    private String generateBasicAnalysisText(RecommendedAsset asset) {
        String type = asset.getType();
        if ("Ação".equals(type)) {
            return String.format(
                    "%s (%s) é uma ação listada na B3 com foco em %s. " +
                    "O ativo apresenta características que o tornam interessante para investidores que buscam %s.",
                    asset.getName(), asset.getTicker(),
                    asset.getRationale() != null ? asset.getRationale().split(",")[0] : "valorização",
                    asset.getExpectedDY() != null && asset.getExpectedDY() > 6 ? "dividendos" : "crescimento"
            );
        } else if ("FII".equals(type)) {
            return String.format(
                    "%s é um Fundo de Investimento Imobiliário que oferece exposição ao mercado imobiliário " +
                    "com distribuição mensal de rendimentos isentos de IR para pessoa física.",
                    asset.getName()
            );
        } else if ("Cripto".equals(type)) {
            return String.format(
                    "%s é um ativo digital descentralizado. Criptomoedas são investimentos de alto risco " +
                    "e alta volatilidade, recomendados apenas para uma pequena parcela do portfólio.",
                    asset.getName()
            );
        } else {
            return String.format(
                    "%s é um ativo de %s que pode contribuir para a diversificação do portfólio.",
                    asset.getName(), type
            );
        }
    }

    private List<String> generateBasicRisks(RecommendedAsset asset) {
        List<String> risks = new ArrayList<>();
        String type = asset.getType();

        if ("Ação".equals(type)) {
            risks.add("Risco de mercado: oscilações no preço da ação");
            risks.add("Risco setorial: fatores específicos do setor de atuação");
            risks.add("Risco de liquidez: dificuldade de vender em momentos de crise");
        } else if ("FII".equals(type)) {
            risks.add("Risco de vacância: imóveis podem ficar desocupados");
            risks.add("Risco de crédito: inadimplência de locatários");
            risks.add("Risco de mercado: variação nas cotas negociadas");
        } else if ("Cripto".equals(type)) {
            risks.add("Volatilidade extrema: variações de preço muito intensas");
            risks.add("Risco regulatório: possíveis restrições governamentais");
            risks.add("Risco de custódia: perda de acesso às chaves privadas");
        } else {
            risks.add("Risco de mercado");
            risks.add("Risco de crédito");
            risks.add("Risco de liquidez");
        }

        return risks;
    }

    private String generateSelicImpact(RecommendedAsset asset) {
        String type = asset.getType();
        if ("Ação".equals(type)) {
            return "Neutro a Negativo - Juros altos aumentam custo de capital das empresas e tornam renda fixa mais atrativa";
        } else if ("FII".equals(type)) {
            return "Negativo - SELIC alta compete com os rendimentos dos FIIs e pode pressionar os preços das cotas";
        } else if ("Cripto".equals(type)) {
            return "Negativo - Juros altos reduzem apetite por ativos de risco como criptomoedas";
        } else if ("Renda Fixa".equals(type)) {
            return "Positivo - SELIC alta beneficia diretamente títulos pós-fixados";
        } else {
            return "Neutro - Impacto variável dependendo das condições específicas";
        }
    }

    private String generateIpcaImpact(RecommendedAsset asset) {
        String type = asset.getType();
        if ("Ação".equals(type)) {
            return "Variável - Empresas com poder de precificação podem repassar inflação; outras sofrem com custos";
        } else if ("FII".equals(type)) {
            return "Positivo - Contratos de aluguel geralmente são reajustados pela inflação";
        } else if ("Cripto".equals(type)) {
            return "Positivo (teórico) - Bitcoin é visto por alguns como proteção contra inflação";
        } else if ("Renda Fixa".equals(type)) {
            return "Depende do tipo - IPCA+ protege contra inflação; prefixados podem perder valor real";
        } else {
            return "Neutro - Impacto variável dependendo das condições específicas";
        }
    }
}
