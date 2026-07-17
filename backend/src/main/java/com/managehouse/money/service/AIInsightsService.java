package com.managehouse.money.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.managehouse.money.config.ChatModelFactory;
import com.managehouse.money.dto.AIMonthlyAnalysisResponse;
import com.managehouse.money.entity.Expense;
import com.managehouse.money.repository.ExpenseRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AIInsightsService {

    private static final Logger logger = LoggerFactory.getLogger(AIInsightsService.class);

    @Autowired
    private ChatModelFactory chatModelFactory;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private EconomicDataService economicDataService;

    @Autowired
    private HouseholdIncomeService householdIncomeService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Obtém o ChatLanguageModel dinamicamente usando a API key do banco.
     * Retorna null se não houver API key configurada.
     */
    private ChatLanguageModel getChatModel() {
        String apiKey = configurationService.getActiveProviderKey();
        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("OpenAI API key not configured. AI features will not work.");
            return null;
        }
        return chatModelFactory.createChatModel(apiKey);
    }

    /**
     * Gera análise mensal completa usando LangChain4j com contexto econômico
     */
    public AIMonthlyAnalysisResponse generateMonthlyAnalysis(Long userId, Integer month, Integer year) {
        ChatLanguageModel chatModel = getChatModel();
        if (chatModel == null) {
            logger.warn("ChatLanguageModel not configured. Returning default analysis.");
            return getDefaultAnalysis();
        }

        try {
            // 1. Coletar dados de gastos
            List<Expense> currentMonthExpenses = expenseRepository.findByUserIdAndMonthAndYear(userId, month, year);
            List<Expense> historicalExpenses = getHistoricalExpenses(userId, month, year, 6);

            if (currentMonthExpenses.isEmpty()) {
                return getDefaultAnalysis();
            }

            // 2. Buscar contexto econômico (IPCA, SELIC, USD, IGP-M)
            com.managehouse.money.dto.EconomicContextResponse economicContext = null;
            try {
                economicContext = economicDataService.fetchEconomicContext();
                logger.info("Contexto econômico obtido com sucesso");
            } catch (Exception e) {
                logger.warn("Não foi possível obter contexto econômico. Continuando sem ele.", e);
            }

            // 2.5. Buscar análise de renda da casa (Mariana + Lucas)
            com.managehouse.money.dto.HouseholdIncomeAnalysis householdIncome = null;
            try {
                householdIncome = householdIncomeService.analyzeHouseholdIncome(month, year);
                logger.info("Análise de renda da casa obtida com sucesso");
            } catch (Exception e) {
                logger.warn("Não foi possível obter análise de renda da casa. Continuando sem ela.", e);
            }

            // 3. Construir contexto de gastos
            String context = buildAnalysisContext(currentMonthExpenses, historicalExpenses, month, year, economicContext, householdIncome);

            // 4. Criar prompt estruturado com contexto econômico
            String prompt = buildMonthlyAnalysisPrompt(context);

            // 5. Chamar LangChain4j
            String response = chatModel.generate(prompt);

            // 6. Parse JSON response
            AIMonthlyAnalysisResponse analysis = parseMonthlyAnalysisResponse(response);

            // 7. Adicionar contexto econômico, dados históricos e renda da casa à resposta
            analysis.setEconomicContext(economicContext);
            analysis.setHistoricalData(buildHistoricalData(userId, month, year, 6));
            analysis.setHouseholdIncome(householdIncome);

            return analysis;

        } catch (Exception e) {
            logger.error("Error generating AI analysis", e);
            return getDefaultAnalysis();
        }
    }

    /**
     * Gera sugestão personalizada para um alerta específico
     */
    public String generateAlertSuggestion(
            String expenseTypeName,
            BigDecimal currentValue,
            BigDecimal averageValue,
            Double percentageAboveAverage) {

        ChatLanguageModel chatModel = getChatModel();
        if (chatModel == null) {
            return String.format("Considere revisar seus gastos com %s.", expenseTypeName);
        }

        try {
            String prompt = String.format("""
                Você é um consultor financeiro. Gere UMA sugestão PRÁTICA e ESPECÍFICA
                (máximo 15 palavras) para o usuário reduzir gastos com %s.

                Contexto:
                - Gasto atual: R$ %.2f
                - Média histórica: R$ %.2f
                - Aumento: %.1f%%

                Retorne APENAS a sugestão em português, sem preâmbulo ou explicação.
                Seja direto e prático.
                """,
                    expenseTypeName,
                    currentValue,
                    averageValue,
                    percentageAboveAverage
            );

            return chatModel.generate(prompt).trim();

        } catch (Exception e) {
            logger.error("Error generating alert suggestion", e);
            return String.format("Considere revisar seus gastos com %s.", expenseTypeName);
        }
    }

    /**
     * Detecta padrões de gastos
     */
    public List<AIMonthlyAnalysisResponse.Pattern> detectSpendingPatterns(List<Expense> expenses) {
        ChatLanguageModel chatModel = getChatModel();
        if (chatModel == null || expenses.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            String context = buildPatternContext(expenses);

            String prompt = String.format("""
                Analise os seguintes gastos e identifique até 3 padrões relevantes.

                Dados:
                %s

                Retorne em JSON (apenas o array, sem markdown):
                [
                  {
                    "type": "temporal ou category ou trend ou anomaly",
                    "description": "Descrição curta (máx 10 palavras)",
                    "insight": "O que isso significa (máx 15 palavras)",
                    "icon": "emoji adequado"
                  }
                ]

                Seja objetivo e use números reais dos dados.
                """, context);

            String response = chatModel.generate(prompt);
            return parsePatterns(response);

        } catch (Exception e) {
            logger.error("Error detecting patterns", e);
            return Collections.emptyList();
        }
    }

    // ==================== HELPER METHODS ====================

    private List<Expense> getHistoricalExpenses(Long userId, Integer currentMonth, Integer currentYear, int monthsBack) {
        List<Expense> historical = new ArrayList<>();
        YearMonth current = YearMonth.of(currentYear, currentMonth);

        for (int i = 1; i <= monthsBack; i++) {
            YearMonth past = current.minusMonths(i);
            List<Expense> monthExpenses = expenseRepository.findByUserIdAndMonthAndYear(
                    userId,
                    past.getMonthValue(),
                    past.getYear()
            );
            historical.addAll(monthExpenses);
        }

        return historical;
    }

    private String buildAnalysisContext(List<Expense> current, List<Expense> historical, Integer month, Integer year,
                                        com.managehouse.money.dto.EconomicContextResponse economicContext,
                                        com.managehouse.money.dto.HouseholdIncomeAnalysis householdIncome) {
        StringBuilder sb = new StringBuilder();

        // Mês atual
        BigDecimal currentTotal = current.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> currentByCategory = current.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getExpenseType().getName(),
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));

        sb.append("=== MÊS ATUAL (").append(month).append("/").append(year).append(") ===\n");
        sb.append("Total: R$ ").append(currentTotal).append("\n");
        sb.append("Transações: ").append(current.size()).append("\n");
        sb.append("Categorias:\n");
        currentByCategory.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> sb.append("  - ").append(e.getKey()).append(": R$ ").append(e.getValue()).append("\n"));

        // Histórico (6 meses)
        if (!historical.isEmpty()) {
            BigDecimal historicalTotal = historical.stream()
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal avgMonthly = historicalTotal.divide(BigDecimal.valueOf(6), 2, RoundingMode.HALF_UP);

            sb.append("\n=== HISTÓRICO (6 meses anteriores) ===\n");
            sb.append("Total: R$ ").append(historicalTotal).append("\n");
            sb.append("Média mensal: R$ ").append(avgMonthly).append("\n");
            sb.append("Transações: ").append(historical.size()).append("\n");
        }

        // Contexto Econômico
        if (economicContext != null) {
            sb.append("\n=== CONTEXTO ECONÔMICO ===\n");

            if (economicContext.getIpca() != null) {
                sb.append("IPCA: ").append(economicContext.getIpca().getValue()).append("% ")
                  .append("(período: ").append(economicContext.getIpca().getPeriod()).append(")\n");
            }

            if (economicContext.getIgpm() != null) {
                sb.append("IGP-M: ").append(economicContext.getIgpm().getValue()).append("% ")
                  .append("(período: ").append(economicContext.getIgpm().getPeriod()).append(")\n");
            }

            if (economicContext.getSelic() != null) {
                sb.append("SELIC: ").append(economicContext.getSelic().getValue()).append("%\n");
            }

            if (economicContext.getUsdBrl() != null) {
                sb.append("Dólar (USD/BRL): R$ ").append(economicContext.getUsdBrl().getValue())
                  .append(" (variação: ").append(economicContext.getUsdBrl().getVariation()).append("%)\n");
            }

            sb.append("\nIMPORTANTE: Compare o crescimento dos gastos com a inflação (IPCA/IGP-M).\n");
            sb.append("Considere o contexto de SELIC alta/baixa nas recomendações de investimento vs gasto.\n");
        }

        // Contexto de Renda da Casa (Mariana + Lucas)
        if (householdIncome != null) {
            sb.append("\n=== RENDA DA CASA (MARIANA + LUCAS) ===\n");
            sb.append("Renda Mariana (fixa): R$ ").append(householdIncome.getMarianaIncome()).append("\n");
            sb.append("Renda Lucas (variável USD): R$ ").append(householdIncome.getLucasNetIncome()).append("\n");
            sb.append("  (Bruto USD convertido: R$ ").append(householdIncome.getLucasGrossIncome()).append(")\n");
            sb.append("Renda Total da Casa: R$ ").append(householdIncome.getTotalHouseholdIncome()).append("\n");
            sb.append("Gastos Totais: R$ ").append(householdIncome.getTotalExpenses()).append("\n");
            sb.append("Poupança: R$ ").append(householdIncome.getSavings());
            sb.append(" (").append(String.format("%.1f%%", householdIncome.getSavingsRate())).append(" da renda)\n");
            sb.append("Proporção Gastos/Renda: ").append(String.format("%.1f%%", householdIncome.getExpenseToIncomeRatio())).append("\n");
            sb.append("Estabilidade de Renda: ").append(householdIncome.getIncomeStabilityStatus())
              .append(" (score: ").append(householdIncome.getIncomeStabilityScore()).append("/100)\n");

            sb.append("\nIMPORTANTE:\n");
            sb.append("- Analise se estão gastando dentro do orçamento familiar (renda vs gastos)\n");
            sb.append("- Se gastos > renda, ALERTAR IMEDIATAMENTE com recomendações urgentes\n");
            sb.append("- Se poupança < 10%, sugerir áreas específicas para cortar gastos\n");
            sb.append("- Considere que Lucas tem renda variável em USD, então há volatilidade cambial\n");
            sb.append("- Compare a poupança atual com metas saudáveis (idealmente 20%+)\n");
            sb.append("- Recomende estratégias considerando a renda total da casa\n");
        }

        return sb.toString();
    }

    private String buildPatternContext(List<Expense> expenses) {
        StringBuilder sb = new StringBuilder();

        Map<String, BigDecimal> byCategory = expenses.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getExpenseType().getName(),
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));

        Map<Integer, BigDecimal> byMonth = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getMonth,
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));

        sb.append("Gastos por categoria:\n");
        byCategory.forEach((cat, amount) ->
                sb.append("  - ").append(cat).append(": R$ ").append(amount).append("\n"));

        sb.append("\nGastos por mês:\n");
        byMonth.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> sb.append("  - Mês ").append(e.getKey()).append(": R$ ").append(e.getValue()).append("\n"));

        return sb.toString();
    }

    private String buildMonthlyAnalysisPrompt(String context) {
        return String.format("""
            Você é um consultor financeiro pessoal. Analise os gastos e gere uma análise mensal.

            %s

            Retorne em JSON (apenas o objeto, sem markdown):
            {
              "executiveSummary": "Resumo em 2 frases (máx 30 palavras)",
              "financialHealthScore": 0-100,
              "patternsDetected": [
                {
                  "type": "trend",
                  "description": "Descrição curta",
                  "insight": "Insight",
                  "icon": "📈"
                }
              ],
              "recommendations": ["Recomendação 1", "Recomendação 2"],
              "nextMonthPrediction": {
                "predictedAmount": 0.00,
                "confidence": 0.75,
                "reasoning": "Explicação breve",
                "assumptions": ["Premissa 1"]
              },
              "comparison": {
                "vsLastMonth": "Texto comparativo",
                "vsAverage": "Texto comparativo",
                "trend": "increasing ou decreasing ou stable"
              }
            }

            Seja ESPECÍFICO com números. Seja PRÁTICO nas recomendações.
            Score: 70-100=bom, 40-69=atenção, 0-39=crítico
            """, context);
    }

    private AIMonthlyAnalysisResponse parseMonthlyAnalysisResponse(String jsonResponse) {
        try {
            // Remover markdown code blocks se existirem
            String cleanJson = jsonResponse
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();

            return objectMapper.readValue(cleanJson, AIMonthlyAnalysisResponse.class);

        } catch (JsonProcessingException e) {
            logger.error("Error parsing AI response JSON", e);
            return getDefaultAnalysis();
        }
    }

    private List<AIMonthlyAnalysisResponse.Pattern> parsePatterns(String jsonResponse) {
        try {
            String cleanJson = jsonResponse
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();

            return objectMapper.readValue(cleanJson, new TypeReference<List<AIMonthlyAnalysisResponse.Pattern>>() {});

        } catch (JsonProcessingException e) {
            logger.error("Error parsing patterns JSON", e);
            return Collections.emptyList();
        }
    }

    /**
     * Constrói dados históricos de 6 meses para os gráficos do frontend
     */
    private List<com.managehouse.money.dto.MonthlySpendingData> buildHistoricalData(Long userId, Integer currentMonth, Integer currentYear, int monthsBack) {
        List<com.managehouse.money.dto.MonthlySpendingData> historicalData = new ArrayList<>();
        YearMonth current = YearMonth.of(currentYear, currentMonth);

        // Incluir o mês atual + 5 meses anteriores = 6 meses total
        for (int i = monthsBack - 1; i >= 0; i--) {
            YearMonth targetMonth = current.minusMonths(i);
            List<Expense> monthExpenses = expenseRepository.findByUserIdAndMonthAndYear(
                    userId,
                    targetMonth.getMonthValue(),
                    targetMonth.getYear()
            );

            // Calcular total do mês
            BigDecimal total = monthExpenses.stream()
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Agrupar por categoria
            Map<String, BigDecimal> byCategory = monthExpenses.stream()
                    .collect(Collectors.groupingBy(
                            e -> e.getExpenseType().getName(),
                            Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                    ));

            List<com.managehouse.money.dto.MonthlySpendingData.CategoryAmount> categories = byCategory.entrySet().stream()
                    .map(e -> new com.managehouse.money.dto.MonthlySpendingData.CategoryAmount(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());

            com.managehouse.money.dto.MonthlySpendingData monthData = new com.managehouse.money.dto.MonthlySpendingData();
            monthData.setMonth(targetMonth.toString()); // Formato: "2025-08"
            monthData.setTotal(total);
            monthData.setTransactionCount(monthExpenses.size());
            monthData.setCategories(categories);

            historicalData.add(monthData);
        }

        return historicalData;
    }

    private AIMonthlyAnalysisResponse getDefaultAnalysis() {
        AIMonthlyAnalysisResponse response = new AIMonthlyAnalysisResponse();
        response.setExecutiveSummary("Não há dados suficientes para gerar análise.");
        response.setFinancialHealthScore(50);
        response.setPatternsDetected(Collections.emptyList());
        response.setRecommendations(List.of("Registre mais despesas para análises mais precisas."));

        AIMonthlyAnalysisResponse.Prediction prediction = new AIMonthlyAnalysisResponse.Prediction();
        prediction.setPredictedAmount(BigDecimal.ZERO);
        prediction.setConfidence(0.0);
        prediction.setReasoning("Dados insuficientes");
        prediction.setAssumptions(Collections.emptyList());
        response.setNextMonthPrediction(prediction);

        AIMonthlyAnalysisResponse.Comparison comparison = new AIMonthlyAnalysisResponse.Comparison();
        comparison.setVsLastMonth("Sem comparação disponível");
        comparison.setVsAverage("Sem comparação disponível");
        comparison.setTrend("stable");
        response.setComparison(comparison);

        return response;
    }
}
