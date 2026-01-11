package com.managehouse.money.service;

import com.managehouse.money.dto.HouseholdIncomeAnalysis;
import com.managehouse.money.dto.MonthlyIncomeData;
import com.managehouse.money.dto.SalaryCalculationRequest;
import com.managehouse.money.dto.SalaryCalculationResponse;
import com.managehouse.money.dto.SalaryResponse;
import com.managehouse.money.entity.Expense;
import com.managehouse.money.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HouseholdIncomeService {
    private final SalaryService salaryService;
    private final ExpenseRepository expenseRepository;

    // IDs dos usuários (baseado no User.java)
    private static final Long LUCAS_ID = 1L;  // vyeiralucas@gmail.com
    private static final Long MARIANA_ID = 2L; // marii_borges@hotmail.com

    public HouseholdIncomeAnalysis analyzeHouseholdIncome(Integer month, Integer year) {
        log.info("Analyzing household income for month={}, year={}", month, year);

        // 1. Get Mariana's fixed salary
        BigDecimal marianaIncome = getMarianaIncome();

        // 2. Get Lucas's calculated salary (net after deductions and debt)
        SalaryCalculationResponse lucasSalary = getLucasSalary(month, year);
        BigDecimal lucasGrossIncome = lucasSalary.getTotalAmountBRL(); // Gross BRL before deductions
        BigDecimal lucasNetIncome = lucasSalary.getNetSalaryBRL(); // Net after deductions and debt

        // 3. Sum total household income (use net for both)
        BigDecimal totalHouseholdIncome = marianaIncome.add(lucasNetIncome);

        // 4. Get total expenses for the month
        BigDecimal totalExpenses = getTotalExpenses(month, year);

        // 5. Calculate metrics
        BigDecimal savings = totalHouseholdIncome.subtract(totalExpenses);
        Double savingsRate = calculatePercentage(savings, totalHouseholdIncome);
        Double expenseToIncomeRatio = calculatePercentage(totalExpenses, totalHouseholdIncome);

        // 6. Get historical data (6 months)
        List<MonthlyIncomeData> historicalData = getHistoricalIncomeData(6, month, year);

        // 7. Calculate income stability score
        Double incomeStabilityScore = calculateIncomeStabilityScore(historicalData);
        String incomeStabilityStatus = getIncomeStabilityStatus(incomeStabilityScore);

        // 8. Determine budget status and recommendations
        String budgetStatus = getBudgetStatus(expenseToIncomeRatio, savingsRate);
        List<String> recommendations = generateRecommendations(
            expenseToIncomeRatio,
            savingsRate,
            savings.compareTo(BigDecimal.ZERO)
        );

        log.info("Household Analysis - Total Income: {}, Expenses: {}, Savings: {}, Savings Rate: {}%",
                totalHouseholdIncome, totalExpenses, savings, savingsRate);

        return HouseholdIncomeAnalysis.builder()
                .marianaIncome(marianaIncome)
                .lucasGrossIncome(lucasGrossIncome)
                .lucasNetIncome(lucasNetIncome)
                .totalHouseholdIncome(totalHouseholdIncome)
                .totalExpenses(totalExpenses)
                .savings(savings)
                .savingsRate(savingsRate)
                .expenseToIncomeRatio(expenseToIncomeRatio)
                .incomeStabilityScore(incomeStabilityScore)
                .incomeStabilityStatus(incomeStabilityStatus)
                .historicalData(historicalData)
                .budgetStatus(budgetStatus)
                .recommendations(recommendations)
                .build();
    }

    private BigDecimal getMarianaIncome() {
        try {
            SalaryResponse marianaSalary = salaryService.getSalaryByUser(MARIANA_ID);
            if (marianaSalary != null && marianaSalary.getFixedAmount() != null) {
                return marianaSalary.getFixedAmount();
            }
            log.warn("Mariana's salary not found or not configured");
            return BigDecimal.ZERO;
        } catch (Exception e) {
            log.error("Error fetching Mariana's salary", e);
            return BigDecimal.ZERO;
        }
    }

    private SalaryCalculationResponse getLucasSalary(Integer month, Integer year) {
        try {
            SalaryCalculationRequest request = new SalaryCalculationRequest();
            request.setUserId(LUCAS_ID);
            request.setMonth(month);
            request.setYear(year);
            return salaryService.calculateVariableSalary(request);
        } catch (Exception e) {
            log.error("Error calculating Lucas's salary for month={}, year={}", month, year, e);
            // Return empty response with zeros
            return new SalaryCalculationResponse(
                BigDecimal.ZERO, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, "USD", month, year
            );
        }
    }

    private BigDecimal getTotalExpenses(Integer month, Integer year) {
        try {
            List<Expense> expenses = expenseRepository.findByYearAndMonth(year, month);
            return expenses.stream()
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception e) {
            log.error("Error fetching expenses for month={}, year={}", month, year, e);
            return BigDecimal.ZERO;
        }
    }

    private List<MonthlyIncomeData> getHistoricalIncomeData(int numberOfMonths, Integer currentMonth, Integer currentYear) {
        List<MonthlyIncomeData> historicalData = new ArrayList<>();

        int month = currentMonth;
        int year = currentYear;

        for (int i = 0; i < numberOfMonths; i++) {
            // Go back one month
            month--;
            if (month < 1) {
                month = 12;
                year--;
            }

            BigDecimal marianaIncome = getMarianaIncome(); // Fixed, same every month
            SalaryCalculationResponse lucasSalary = getLucasSalary(month, year);
            BigDecimal lucasIncome = lucasSalary.getNetSalaryBRL();
            BigDecimal totalIncome = marianaIncome.add(lucasIncome);
            BigDecimal expenses = getTotalExpenses(month, year);
            BigDecimal savings = totalIncome.subtract(expenses);

            String monthStr = String.format("%d-%02d", year, month);

            MonthlyIncomeData data = MonthlyIncomeData.builder()
                    .month(monthStr)
                    .marianaIncome(marianaIncome)
                    .lucasIncome(lucasIncome)
                    .totalIncome(totalIncome)
                    .expenses(expenses)
                    .savings(savings)
                    .build();

            historicalData.add(0, data); // Add to beginning to maintain chronological order
        }

        return historicalData;
    }

    private Double calculateIncomeStabilityScore(List<MonthlyIncomeData> historicalData) {
        if (historicalData == null || historicalData.size() < 2) {
            return 50.0; // Default mid score if not enough data
        }

        // Calculate coefficient of variation for total income
        // Lower variance = higher stability
        List<BigDecimal> incomes = historicalData.stream()
                .map(MonthlyIncomeData::getTotalIncome)
                .filter(income -> income.compareTo(BigDecimal.ZERO) > 0)
                .toList();

        if (incomes.isEmpty()) {
            return 50.0;
        }

        // Calculate mean
        BigDecimal sum = incomes.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal mean = sum.divide(new BigDecimal(incomes.size()), 2, RoundingMode.HALF_UP);

        // Calculate variance
        BigDecimal variance = incomes.stream()
                .map(income -> income.subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(incomes.size()), 2, RoundingMode.HALF_UP);

        // Calculate standard deviation
        double stdDev = Math.sqrt(variance.doubleValue());

        // Calculate coefficient of variation (CV = stdDev / mean)
        double cv = mean.compareTo(BigDecimal.ZERO) > 0 ?
                    (stdDev / mean.doubleValue()) : 0;

        // Convert CV to stability score (0-100)
        // CV of 0 = 100 (perfectly stable)
        // CV of 0.3 or more = 0 (very volatile)
        double score = Math.max(0, Math.min(100, 100 - (cv * 333.33)));

        return Math.round(score * 10.0) / 10.0; // Round to 1 decimal place
    }

    private String getIncomeStabilityStatus(Double score) {
        if (score >= 70) return "Estável";
        if (score >= 40) return "Moderado";
        return "Volátil";
    }

    private String getBudgetStatus(Double expenseRatio, Double savingsRate) {
        if (expenseRatio > 100) return "Crítico"; // Spending more than earning
        if (savingsRate < 10) return "Atenção"; // Saving less than 10%
        if (savingsRate < 20) return "Bom"; // Saving 10-20%
        return "Excelente"; // Saving 20%+
    }

    private List<String> generateRecommendations(Double expenseRatio, Double savingsRate, int savingsSign) {
        List<String> recommendations = new ArrayList<>();

        if (expenseRatio > 100) {
            recommendations.add("⚠️ ALERTA: Vocês estão gastando mais do que ganham! Revisem urgentemente os gastos.");
        }

        if (savingsSign < 0) {
            recommendations.add("Eliminem gastos não essenciais imediatamente para evitar dívidas.");
        }

        if (savingsRate < 10) {
            recommendations.add("Meta: Tentem poupar pelo menos 10% da renda mensal.");
            recommendations.add("Identifiquem as 3 maiores categorias de gasto e busquem reduções.");
        } else if (savingsRate < 20) {
            recommendations.add("Bom trabalho! Tentem aumentar a poupança para 20% do total.");
        } else {
            recommendations.add("Excelente controle financeiro! Mantenham essa disciplina.");
        }

        recommendations.add("Considerem criar uma reserva de emergência equivalente a 6 meses de despesas.");
        recommendations.add("Aproveitem a renda em dólar (Lucas) para se proteger contra variações cambiais.");

        return recommendations;
    }

    private Double calculatePercentage(BigDecimal part, BigDecimal whole) {
        if (whole.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return part.divide(whole, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .doubleValue();
    }
}
