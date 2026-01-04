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

