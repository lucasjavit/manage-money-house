package com.managehouse.money.service;

import com.managehouse.money.dto.ExpenseAlertsResponse;
import com.managehouse.money.entity.Expense;
import com.managehouse.money.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseAlertsService {

    private final ExpenseRepository expenseRepository;

    // Limites para alertas
    private static final double WARNING_THRESHOLD = 20.0; // 20% acima da média
    private static final double CRITICAL_THRESHOLD = 40.0; // 40% acima da média

    public ExpenseAlertsResponse generateAlerts(Long userId, Integer month, Integer year) {
        log.info("Gerando alertas para userId: {}, mês: {}, ano: {}", userId, month, year);

        // Buscar despesas do mês atual
        List<Expense> currentMonthExpenses = expenseRepository.findByUserIdAndMonthAndYear(userId, month, year);

        if (currentMonthExpenses.isEmpty()) {
            return createEmptyResponse(month, year);
        }

        // Buscar histórico dos últimos 6 meses (excluindo o mês atual)
        List<Expense> historicalExpenses = getHistoricalExpenses(userId, month, year, 6);

        // Agrupar despesas por tipo
        Map<String, List<Expense>> currentByType = groupByExpenseType(currentMonthExpenses);
        Map<String, List<Expense>> historicalByType = groupByExpenseType(historicalExpenses);

        // Calcular totais
        BigDecimal totalCurrentMonth = calculateTotal(currentMonthExpenses);
        BigDecimal averageMonthSpent = calculateAverageMonthlySpent(userId, month, year, 6);

        // Gerar alertas por tipo de despesa
        List<ExpenseAlertsResponse.Alert> alerts = new ArrayList<>();

        for (Map.Entry<String, List<Expense>> entry : currentByType.entrySet()) {
            String expenseType = entry.getKey();
            List<Expense> currentExpenses = entry.getValue();
            List<Expense> historicalExpensesForType = historicalByType.getOrDefault(expenseType, Collections.emptyList());

            BigDecimal currentTotal = calculateTotal(currentExpenses);
            BigDecimal averageTotal = calculateAverageForType(historicalExpensesForType, 6);
            BigDecimal maxHistorical = calculateMaxForType(historicalExpensesForType);

            // Se não há histórico, não gera alerta
            if (averageTotal.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            // Calcular variação percentual
            BigDecimal difference = currentTotal.subtract(averageTotal);
            double percentageAbove = averageTotal.compareTo(BigDecimal.ZERO) > 0
                    ? difference.divide(averageTotal, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100")).doubleValue()
                    : 0.0;

            // Verificar se é máximo histórico
            boolean isHistoricalMax = currentTotal.compareTo(maxHistorical) > 0;

            // Determinar severidade e criar alerta se necessário
            String severity = determineSeverity(percentageAbove);
            if (!severity.equals("normal") || isHistoricalMax) {
                ExpenseAlertsResponse.Alert alert = new ExpenseAlertsResponse.Alert(
                        severity,
                        expenseType,
                        currentTotal,
                        averageTotal,
                        maxHistorical,
                        percentageAbove,
                        isHistoricalMax,
                        generateSuggestion(expenseType, percentageAbove, isHistoricalMax),
                        getIconForSeverity(severity)
                );
                alerts.add(alert);
            }
        }

        // Ordenar alertas por severidade (crítico primeiro)
        alerts.sort((a, b) -> {
            int severityCompare = getSeverityOrder(b.getSeverity()) - getSeverityOrder(a.getSeverity());
            if (severityCompare != 0) return severityCompare;
            return b.getPercentageAboveAverage().compareTo(a.getPercentageAboveAverage());
        });

        // Criar sumário
        ExpenseAlertsResponse.Summary summary = createSummary(alerts, totalCurrentMonth, averageMonthSpent);

        return new ExpenseAlertsResponse(month, year, alerts, summary);
    }

    private List<Expense> getHistoricalExpenses(Long userId, Integer currentMonth, Integer currentYear, int monthsBack) {
        List<Expense> historical = new ArrayList<>();
        LocalDate currentDate = LocalDate.of(currentYear, currentMonth, 1);

        for (int i = 1; i <= monthsBack; i++) {
            LocalDate pastDate = currentDate.minusMonths(i);
            List<Expense> monthExpenses = expenseRepository.findByUserIdAndMonthAndYear(
                    userId, pastDate.getMonthValue(), pastDate.getYear());
            historical.addAll(monthExpenses);
        }

        return historical;
    }

    private Map<String, List<Expense>> groupByExpenseType(List<Expense> expenses) {
        return expenses.stream()
                .collect(Collectors.groupingBy(e -> e.getExpenseType().getName()));
    }

    private BigDecimal calculateTotal(List<Expense> expenses) {
        return expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateAverageForType(List<Expense> historicalExpenses, int months) {
        if (historicalExpenses.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Agrupar por mês e somar
        Map<String, BigDecimal> monthlyTotals = new HashMap<>();
        for (Expense expense : historicalExpenses) {
            String key = expense.getYear() + "-" + expense.getMonth();
            monthlyTotals.put(key, monthlyTotals.getOrDefault(key, BigDecimal.ZERO).add(expense.getAmount()));
        }

        // Calcular média
        BigDecimal total = monthlyTotals.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int divisor = Math.max(monthlyTotals.size(), 1);
        return total.divide(new BigDecimal(divisor), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateMaxForType(List<Expense> historicalExpenses) {
        if (historicalExpenses.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Agrupar por mês e somar
        Map<String, BigDecimal> monthlyTotals = new HashMap<>();
        for (Expense expense : historicalExpenses) {
            String key = expense.getYear() + "-" + expense.getMonth();
            monthlyTotals.put(key, monthlyTotals.getOrDefault(key, BigDecimal.ZERO).add(expense.getAmount()));
        }

        return monthlyTotals.values().stream()
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal calculateAverageMonthlySpent(Long userId, Integer currentMonth, Integer currentYear, int monthsBack) {
        List<Expense> historical = getHistoricalExpenses(userId, currentMonth, currentYear, monthsBack);

        if (historical.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Agrupar por mês
        Map<String, BigDecimal> monthlyTotals = new HashMap<>();
        for (Expense expense : historical) {
            String key = expense.getYear() + "-" + expense.getMonth();
            monthlyTotals.put(key, monthlyTotals.getOrDefault(key, BigDecimal.ZERO).add(expense.getAmount()));
        }

        BigDecimal total = monthlyTotals.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int divisor = Math.max(monthlyTotals.size(), 1);
        return total.divide(new BigDecimal(divisor), 2, RoundingMode.HALF_UP);
    }

    private String determineSeverity(double percentageAbove) {
        if (percentageAbove >= CRITICAL_THRESHOLD) {
            return "critical";
        } else if (percentageAbove >= WARNING_THRESHOLD) {
            return "warning";
        }
        return "normal";
    }

    private int getSeverityOrder(String severity) {
        switch (severity) {
            case "critical": return 3;
            case "warning": return 2;
            default: return 1;
        }
    }

    private String getIconForSeverity(String severity) {
        switch (severity) {
            case "critical": return "🔥";
            case "warning": return "⚠️";
            default: return "ℹ️";
        }
    }

    private String generateSuggestion(String expenseType, double percentageAbove, boolean isHistoricalMax) {
        if (isHistoricalMax) {
            return String.format("Atenção: este é o maior gasto em %s já registrado!", expenseType);
        }

        if (percentageAbove >= CRITICAL_THRESHOLD) {
            return String.format("Considere revisar seus gastos com %s. Valor muito acima do normal.", expenseType);
        } else if (percentageAbove >= WARNING_THRESHOLD) {
            return String.format("Gastos com %s estão aumentando. Monitore nos próximos meses.", expenseType);
        }

        return "";
    }

    private ExpenseAlertsResponse.Summary createSummary(
            List<ExpenseAlertsResponse.Alert> alerts,
            BigDecimal totalCurrentMonth,
            BigDecimal averageMonthSpent) {

        int criticalCount = (int) alerts.stream()
                .filter(a -> "critical".equals(a.getSeverity()))
                .count();

        int warningCount = (int) alerts.stream()
                .filter(a -> "warning".equals(a.getSeverity()))
                .count();

        String overallStatus;
        if (criticalCount > 0) {
            overallStatus = "critical";
        } else if (warningCount > 0) {
            overallStatus = "attention";
        } else {
            overallStatus = "good";
        }

        return new ExpenseAlertsResponse.Summary(
                alerts.size(),
                criticalCount,
                warningCount,
                totalCurrentMonth,
                averageMonthSpent,
                overallStatus
        );
    }

    private ExpenseAlertsResponse createEmptyResponse(Integer month, Integer year) {
        return new ExpenseAlertsResponse(
                month,
                year,
                Collections.emptyList(),
                new ExpenseAlertsResponse.Summary(0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, "good")
        );
    }
}
