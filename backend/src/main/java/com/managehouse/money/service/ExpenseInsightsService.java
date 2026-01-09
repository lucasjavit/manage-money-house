package com.managehouse.money.service;

import com.managehouse.money.dto.ExpenseInsightsResponse;
import com.managehouse.money.dto.OpenAIRequest;
import com.managehouse.money.dto.OpenAIResponse;
import com.managehouse.money.entity.ExtractTransaction;
import com.managehouse.money.repository.ExtractTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseInsightsService {
    
    private final ExtractTransactionRepository extractTransactionRepository;
    private final ConfigurationService configurationService;
    
    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;
    
    @Value("${openai.model:gpt-4o-mini}")
    private String model;
    
    private WebClient webClient;
    
    public ExpenseInsightsResponse generateInsights(Long userId, Integer month, Integer year) {
        log.info("Gerando insights para userId: {}, mês: {}, ano: {}", userId, month, year);
        
        // Calcular período: mês de pagamento (month/year) corresponde a transações do mês anterior
        int transactionMonth = month - 1;
        int transactionYear = year;
        if (transactionMonth < 1) {
            transactionMonth = 12;
            transactionYear = year - 1;
        }
        
        LocalDate startDate = LocalDate.of(transactionYear, transactionMonth, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        
        List<ExtractTransaction> transactions = extractTransactionRepository
                .findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(userId, startDate, endDate);
        
        if (transactions.isEmpty()) {
            log.warn("Nenhuma transação encontrada para o período");
            return createEmptyInsights();
        }
        
        // Calcular estatísticas básicas
        BigDecimal totalSpent = transactions.stream()
                .map(ExtractTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Média por transação
        BigDecimal averagePerTransaction = transactions.size() > 0
                ? totalSpent.divide(new BigDecimal(transactions.size()), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        
        // Dia da semana com mais gastos
        Map<String, BigDecimal> dayOfWeekMap = new HashMap<>();
        Map<String, Integer> dayOfWeekCount = new HashMap<>();
        for (ExtractTransaction t : transactions) {
            String dayName = t.getTransactionDate().getDayOfWeek().toString();
            dayOfWeekMap.put(dayName, dayOfWeekMap.getOrDefault(dayName, BigDecimal.ZERO).add(t.getAmount()));
            dayOfWeekCount.put(dayName, dayOfWeekCount.getOrDefault(dayName, 0) + 1);
        }
        String mostExpensiveDay = dayOfWeekMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> translateDayOfWeek(e.getKey()))
                .orElse("N/A");
        
        // Agrupar por categoria
        Map<String, CategoryStats> categoryMap = new HashMap<>();
        for (ExtractTransaction t : transactions) {
            String category = t.getExtractExpenseType().getName();
            categoryMap.computeIfAbsent(category, k -> new CategoryStats(category))
                    .addTransaction(t.getAmount());
        }
        
        List<ExpenseInsightsResponse.CategorySummary> categories = categoryMap.values().stream()
                .map(stats -> new ExpenseInsightsResponse.CategorySummary(
                        stats.name,
                        stats.total,
                        stats.count,
                        totalSpent.compareTo(BigDecimal.ZERO) > 0
                                ? stats.total.divide(totalSpent, 4, RoundingMode.HALF_UP)
                                        .multiply(new BigDecimal("100")).doubleValue()
                                : 0.0,
                        stats.highestExpense
                ))
                .sorted((a, b) -> b.getTotal().compareTo(a.getTotal()))
                .collect(Collectors.toList());
        
        String mostActiveCategory = categories.isEmpty() ? "N/A" : categories.get(0).getCategoryName();
        
        // Top 5 maiores gastos
        List<ExpenseInsightsResponse.TopExpense> topExpenses = transactions.stream()
                .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
                .limit(5)
                .map(t -> new ExpenseInsightsResponse.TopExpense(
                        t.getDescription(),
                        t.getAmount(),
                        t.getExtractExpenseType().getName(),
                        t.getTransactionDate().toString()
                ))
                .collect(Collectors.toList());
        
        // Gerar insights com IA (com mais dados)
        ExpenseInsightsResponse.AIInsights aiInsights = generateAIInsights(transactions, totalSpent, categories, averagePerTransaction, mostExpensiveDay);
        
        // Calcular tendências (comparar com mês anterior se disponível)
        List<ExpenseInsightsResponse.Trend> trends = calculateTrends(userId, transactionMonth, transactionYear, totalSpent, categories);
        
        // Quick Stats inteligentes
        List<ExpenseInsightsResponse.QuickStat> quickStats = calculateQuickStats(transactions, totalSpent, averagePerTransaction, categories);
        
        return new ExpenseInsightsResponse(
                totalSpent,
                transactions.size(),
                averagePerTransaction,
                mostExpensiveDay,
                mostActiveCategory,
                categories,
                topExpenses,
                aiInsights,
                trends,
                quickStats
        );
    }
    
    private String translateDayOfWeek(String day) {
        Map<String, String> days = new HashMap<>();
        days.put("MONDAY", "Segunda");
        days.put("TUESDAY", "Terça");
        days.put("WEDNESDAY", "Quarta");
        days.put("THURSDAY", "Quinta");
        days.put("FRIDAY", "Sexta");
        days.put("SATURDAY", "Sábado");
        days.put("SUNDAY", "Domingo");
        return days.getOrDefault(day, day);
    }
    
    private List<ExpenseInsightsResponse.QuickStat> calculateQuickStats(
            List<ExtractTransaction> transactions,
            BigDecimal totalSpent,
            BigDecimal averagePerTransaction,
            List<ExpenseInsightsResponse.CategorySummary> categories) {
        
        List<ExpenseInsightsResponse.QuickStat> stats = new ArrayList<>();
        
        // Média por transação
        stats.add(new ExpenseInsightsResponse.QuickStat(
                "Média/Transação",
                "R$ " + averagePerTransaction.setScale(0, RoundingMode.HALF_UP),
                "📊",
                "blue"
        ));
        
        // Maior gasto único
        if (!transactions.isEmpty()) {
            BigDecimal maxExpense = transactions.stream()
                    .map(ExtractTransaction::getAmount)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            stats.add(new ExpenseInsightsResponse.QuickStat(
                    "Maior Gasto",
                    "R$ " + maxExpense.setScale(0, RoundingMode.HALF_UP),
                    "🔥",
                    "red"
            ));
        }
        
        // Categoria dominante
        if (!categories.isEmpty()) {
            ExpenseInsightsResponse.CategorySummary topCat = categories.get(0);
            stats.add(new ExpenseInsightsResponse.QuickStat(
                    "Top Categoria",
                    topCat.getCategoryName() + " (" + topCat.getPercentage().intValue() + "%)",
                    "🏆",
                    "purple"
            ));
        }
        
        // Transações por dia (média)
        if (!transactions.isEmpty()) {
            long daysInMonth = transactions.stream()
                    .map(ExtractTransaction::getTransactionDate)
                    .distinct()
                    .count();
            double avgPerDay = transactions.size() / (double) Math.max(daysInMonth, 1);
            stats.add(new ExpenseInsightsResponse.QuickStat(
                    "Transações/Dia",
                    String.format("%.1f", avgPerDay),
                    "📅",
                    "green"
            ));
        }
        
        return stats;
    }
    
    private ExpenseInsightsResponse.AIInsights generateAIInsights(
            List<ExtractTransaction> transactions,
            BigDecimal totalSpent,
            List<ExpenseInsightsResponse.CategorySummary> categories,
            BigDecimal averagePerTransaction,
            String mostExpensiveDay) {
        
        String apiKey = configurationService.getOpenAIKey();
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("OpenAI API key não configurada para insights");
            return createDefaultAIInsights();
        }
        
        try {
            // Preparar dados para a IA com mais contexto
            StringBuilder dataSummary = new StringBuilder();
            dataSummary.append("Análise de gastos do cartão de crédito:\n\n");
            dataSummary.append("Total gasto: R$ ").append(totalSpent.setScale(2, RoundingMode.HALF_UP)).append("\n");
            dataSummary.append("Total de transações: ").append(transactions.size()).append("\n");
            dataSummary.append("Média por transação: R$ ").append(averagePerTransaction.setScale(2, RoundingMode.HALF_UP)).append("\n");
            dataSummary.append("Dia com mais gastos: ").append(mostExpensiveDay).append("\n\n");
            dataSummary.append("Gastos por categoria:\n");
            for (ExpenseInsightsResponse.CategorySummary cat : categories) {
                dataSummary.append(String.format("- %s: R$ %.2f (%.1f%%, %d transações)\n", 
                        cat.getCategoryName(), cat.getTotal(), cat.getPercentage(), cat.getCount()));
            }
            dataSummary.append("\nTop 5 maiores gastos:\n");
            transactions.stream()
                    .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
                    .limit(5)
                    .forEach(t -> dataSummary.append(String.format("- %s: R$ %.2f (%s, %s)\n",
                            t.getDescription(), t.getAmount(), t.getExtractExpenseType().getName(),
                            t.getTransactionDate().toString())));
            
            // Detectar padrões
            dataSummary.append("\nPadrões detectados:\n");
            if (categories.stream().anyMatch(c -> c.getCategoryName().equals("Delivery") && c.getPercentage() > 15)) {
                dataSummary.append("- Alto gasto com delivery (>15% do total)\n");
            }
            if (categories.stream().anyMatch(c -> c.getCategoryName().equals("Lazer") && c.getPercentage() > 20)) {
                dataSummary.append("- Alto gasto com lazer (>20% do total)\n");
            }
            if (averagePerTransaction.compareTo(new BigDecimal("200")) > 0) {
                dataSummary.append("- Média de transação alta (>R$ 200)\n");
            }
            
            String prompt = buildInsightsPrompt(dataSummary.toString());
            
            List<OpenAIRequest.Message> messages = new ArrayList<>();
            messages.add(new OpenAIRequest.Message("system",
                    "Você é um consultor financeiro especializado em análise de gastos pessoais. " +
                    "Analise os dados fornecidos e forneça insights práticos, sugestões de economia e alertas sobre padrões de gastos. " +
                    "Seja MUITO objetivo e direto - máximo 1 frase por sugestão/alerta. " +
                    "Foque em ações concretas e acionáveis. " +
                    "Identifique padrões preocupantes e oportunidades de economia específicas."));
            messages.add(new OpenAIRequest.Message("user", prompt));
            
            OpenAIRequest openAIRequest = new OpenAIRequest(model, messages, 0.7);
            
            if (webClient == null) {
                webClient = WebClient.builder()
                        .baseUrl(apiUrl)
                        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                        .build();
            }
            
            OpenAIResponse response = webClient.post()
                    .bodyValue(openAIRequest)
                    .retrieve()
                    .bodyToMono(OpenAIResponse.class)
                    .block();
            
            if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                String content = response.getChoices().get(0).getMessage().getContent();
                return parseAIInsights(content);
            }
            
        } catch (Exception e) {
            log.error("Erro ao gerar insights com IA", e);
        }
        
        return createDefaultAIInsights();
    }
    
    private String buildInsightsPrompt(String dataSummary) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analise os seguintes dados de gastos do cartão de crédito e forneça insights em formato JSON:\n\n");
        prompt.append(dataSummary);
        prompt.append("\n\nForneça insights MUITO CONCISOS:\n");
        prompt.append("1. Resumo: 1 frase apenas\n");
        prompt.append("2. 3-4 sugestões práticas: 1 frase cada, máximo 60 caracteres\n");
        prompt.append("3. 1-2 alertas críticos: 1 frase cada, máximo 60 caracteres\n");
        prompt.append("4. Análise: 2-3 frases curtas\n\n");
        prompt.append("Retorne APENAS um JSON válido no seguinte formato:\n");
        prompt.append("{\n");
        prompt.append("  \"summary\": \"Resumo geral dos gastos\",\n");
        prompt.append("  \"suggestions\": [\"Sugestão 1\", \"Sugestão 2\", ...],\n");
        prompt.append("  \"warnings\": [\"Alerta 1\", \"Alerta 2\", ...],\n");
        prompt.append("  \"analysis\": \"Análise detalhada\"\n");
        prompt.append("}\n");
        
        return prompt.toString();
    }
    
    private ExpenseInsightsResponse.AIInsights parseAIInsights(String content) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String jsonContent = extractJSON(content);
            if (jsonContent == null) {
                return createDefaultAIInsights();
            }
            
            Map<String, Object> result = mapper.readValue(jsonContent,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            
            @SuppressWarnings("unchecked")
            List<String> suggestions = (List<String>) result.getOrDefault("suggestions", Collections.emptyList());
            @SuppressWarnings("unchecked")
            List<String> warnings = (List<String>) result.getOrDefault("warnings", Collections.emptyList());
            
            return new ExpenseInsightsResponse.AIInsights(
                    (String) result.getOrDefault("summary", ""),
                    suggestions,
                    warnings,
                    (String) result.getOrDefault("analysis", "")
            );
        } catch (Exception e) {
            log.warn("Erro ao parsear insights da IA: {}", e.getMessage());
            return createDefaultAIInsights();
        }
    }
    
    private String extractJSON(String text) {
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return null;
    }
    
    private List<ExpenseInsightsResponse.Trend> calculateTrends(
            Long userId, int month, int year, BigDecimal currentTotal, 
            List<ExpenseInsightsResponse.CategorySummary> currentCategories) {
        
        List<ExpenseInsightsResponse.Trend> trends = new ArrayList<>();
        
        // Comparar com mês anterior
        int prevMonth = month - 1;
        int prevYear = year;
        if (prevMonth < 1) {
            prevMonth = 12;
            prevYear = year - 1;
        }
        
        LocalDate prevStart = LocalDate.of(prevYear, prevMonth, 1);
        LocalDate prevEnd = prevStart.withDayOfMonth(prevStart.lengthOfMonth());
        
        List<ExtractTransaction> prevTransactions = extractTransactionRepository
                .findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(userId, prevStart, prevEnd);
        
        if (!prevTransactions.isEmpty()) {
            BigDecimal prevTotal = prevTransactions.stream()
                    .map(ExtractTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal difference = currentTotal.subtract(prevTotal);
            double percentChange = prevTotal.compareTo(BigDecimal.ZERO) > 0
                    ? difference.divide(prevTotal, 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100")).doubleValue()
                    : 0.0;
            
            String type = difference.compareTo(BigDecimal.ZERO) > 0 ? "increase" 
                    : difference.compareTo(BigDecimal.ZERO) < 0 ? "decrease" : "stable";
            
            trends.add(new ExpenseInsightsResponse.Trend(
                    "Comparação com mês anterior",
                    String.format("%s R$ %.2f (%.1f%%)", 
                            difference.compareTo(BigDecimal.ZERO) > 0 ? "+" : "",
                            difference.abs(), Math.abs(percentChange)),
                    type
            ));
        }
        
        return trends;
    }
    
    private ExpenseInsightsResponse createEmptyInsights() {
        return new ExpenseInsightsResponse(
                BigDecimal.ZERO,
                0,
                BigDecimal.ZERO,
                "N/A",
                "N/A",
                Collections.emptyList(),
                Collections.emptyList(),
                createDefaultAIInsights(),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }
    
    private ExpenseInsightsResponse.AIInsights createDefaultAIInsights() {
        return new ExpenseInsightsResponse.AIInsights(
                "Nenhum dado disponível para análise.",
                Collections.emptyList(),
                Collections.emptyList(),
                "Carregue transações para ver insights personalizados."
        );
    }
    
    private static class CategoryStats {
        String name;
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal highestExpense = BigDecimal.ZERO;
        int count = 0;

        CategoryStats(String name) {
            this.name = name;
        }

        void addTransaction(BigDecimal amount) {
            this.total = this.total.add(amount);
            this.count++;
            if (amount.compareTo(this.highestExpense) > 0) {
                this.highestExpense = amount;
            }
        }
    }
}

