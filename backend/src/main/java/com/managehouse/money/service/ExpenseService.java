package com.managehouse.money.service;

import com.managehouse.money.dto.ExpenseRequest;
import com.managehouse.money.dto.ExpenseResponse;
import com.managehouse.money.entity.Expense;
import com.managehouse.money.entity.ExpenseType;
import com.managehouse.money.entity.User;
import com.managehouse.money.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseService {
    private final ExpenseRepository expenseRepository;
    private final UserService userService;
    private final ExpenseTypeService expenseTypeService;

    public List<ExpenseResponse> getExpensesByYear(Integer year) {
        return expenseRepository.findByYear(year).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ExpenseResponse createOrUpdateExpense(ExpenseRequest request) {
        User user = userService.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        ExpenseType expenseType = expenseTypeService.findById(request.getExpenseTypeId())
                .orElseThrow(() -> new RuntimeException("Expense type not found"));

        // Verifica se já existe uma despesa para este usuário, tipo, mês e ano
        // Se existir, atualiza; se não, cria nova
        Expense expense = expenseRepository
                .findByYearAndMonthAndExpenseTypeAndUser(
                        request.getYear(),
                        request.getMonth(),
                        request.getExpenseTypeId(),
                        user.getId()
                )
                .orElse(new Expense());

        expense.setUser(user);
        expense.setExpenseType(expenseType);
        expense.setAmount(request.getAmount());
        expense.setMonth(request.getMonth());
        expense.setYear(request.getYear());

        Expense saved = expenseRepository.save(expense);
        return toResponse(saved);
    }

    @Transactional
    public void deleteExpense(Long id) {
        expenseRepository.deleteById(id);
    }

    /**
     * Calcula a dívida do Lucas para um mês/ano específico usando a lógica Splitwise.
     * Retorna um valor positivo se Lucas deve para Mariana, negativo se Mariana deve para Lucas, ou zero se estão quites.
     */
    public BigDecimal calculateLucasDebt(Integer year, Integer month) {
        // Buscar todas as despesas do mês
        List<Expense> monthExpenses = expenseRepository.findByYearAndMonth(year, month);
        
        if (monthExpenses.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        // Calcular total do mês
        BigDecimal totalMonth = monthExpenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Cada um deveria pagar (metade do total) - lógica Splitwise
        BigDecimal eachShouldPay = totalMonth.divide(new BigDecimal("2"), 2, java.math.RoundingMode.HALF_UP);
        
        // O que cada um pagou
        BigDecimal lucasPaid = monthExpenses.stream()
                .filter(e -> "blue".equals(e.getUser().getColor()))
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Saldo do Lucas: o que ele pagou - o que ele deveria pagar
        // Se negativo: Lucas deve para Mariana
        // Se positivo: Mariana deve para Lucas
        BigDecimal lucasBalance = lucasPaid.subtract(eachShouldPay);
        
        // Retornar o valor que Lucas deve (negativo do saldo)
        // Se lucasBalance é negativo, ele deve o valor absoluto
        // Se lucasBalance é positivo, Mariana deve (retornar negativo para indicar)
        return lucasBalance.negate();
    }

    private ExpenseResponse toResponse(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getUser().getId(),
                expense.getUser().getName(),
                expense.getUser().getColor(),
                expense.getExpenseType().getId(),
                expense.getExpenseType().getName(),
                expense.getAmount(),
                expense.getMonth(),
                expense.getYear(),
                expense.getRecurringExpense() != null ? expense.getRecurringExpense().getId() : null,
                expense.getCreatedAt()
        );
    }
}

