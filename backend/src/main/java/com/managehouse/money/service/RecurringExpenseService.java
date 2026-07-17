package com.managehouse.money.service;

import com.managehouse.money.dto.RecurringExpenseRequest;
import com.managehouse.money.dto.RecurringExpenseResponse;
import com.managehouse.money.entity.Expense;
import com.managehouse.money.entity.ExpenseType;
import com.managehouse.money.entity.RecurringExpense;
import com.managehouse.money.entity.User;
import com.managehouse.money.repository.ExpenseRepository;
import com.managehouse.money.repository.RecurringExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecurringExpenseService {
    private final RecurringExpenseRepository recurringExpenseRepository;
    private final ExpenseRepository expenseRepository;
    private final UserService userService;
    private final ExpenseTypeService expenseTypeService;

    @Transactional
    public RecurringExpenseResponse createRecurringExpense(RecurringExpenseRequest request) {
        User user = userService.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        ExpenseType expenseType = expenseTypeService.findById(request.getExpenseTypeId())
                .orElseThrow(() -> new RuntimeException("Expense type not found"));

        RecurringExpense recurringExpense = new RecurringExpense();
        recurringExpense.setUser(user);
        recurringExpense.setExpenseType(expenseType);
        recurringExpense.setMonthlyAmount(request.getMonthlyAmount());
        recurringExpense.setStartDate(request.getStartDate());
        recurringExpense.setEndDate(request.getEndDate());

        RecurringExpense saved = recurringExpenseRepository.save(recurringExpense);

        // Gerar expenses para cada mês do período
        generateExpensesForPeriod(saved);

        return toResponse(saved);
    }

    @Transactional
    public RecurringExpenseResponse updateRecurringExpense(Long id, RecurringExpenseRequest request) {
        RecurringExpense recurringExpense = recurringExpenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recurring expense not found"));

        User user = userService.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        ExpenseType expenseType = expenseTypeService.findById(request.getExpenseTypeId())
                .orElseThrow(() -> new RuntimeException("Expense type not found"));

        // Remover todas as expenses antigas relacionadas a esta dívida recorrente
        deleteExpensesForRecurringExpense(id);

        // Atualizar dados da dívida recorrente
        recurringExpense.setUser(user);
        recurringExpense.setExpenseType(expenseType);
        recurringExpense.setMonthlyAmount(request.getMonthlyAmount());
        recurringExpense.setStartDate(request.getStartDate());
        recurringExpense.setEndDate(request.getEndDate());

        RecurringExpense saved = recurringExpenseRepository.save(recurringExpense);

        // Gerar novas expenses para o período atualizado
        generateExpensesForPeriod(saved);

        return toResponse(saved);
    }

    @Transactional
    public void deleteRecurringExpense(Long id) {
        // Remover todas as expenses relacionadas
        deleteExpensesForRecurringExpense(id);
        // Remover a dívida recorrente
        recurringExpenseRepository.deleteById(id);
    }

    private void generateExpensesForPeriod(RecurringExpense recurringExpense) {
        LocalDate start = recurringExpense.getStartDate();
        LocalDate end = recurringExpense.getEndDate();

        // Obter o primeiro mês completo (mês da data de início)
        YearMonth startMonth = YearMonth.from(start);
        // Obter o último mês completo (mês da data de fim)
        YearMonth endMonth = YearMonth.from(end);

        // Cada mês do período ganha um lançamento próprio da recorrente, convivendo
        // com os lançamentos manuais da mesma célula em vez de somar-se a eles.
        YearMonth current = startMonth;
        while (!current.isAfter(endMonth)) {
            Expense expense = new Expense();
            expense.setUser(recurringExpense.getUser());
            expense.setExpenseType(recurringExpense.getExpenseType());
            expense.setAmount(recurringExpense.getMonthlyAmount());
            expense.setMonth(current.getMonthValue());
            expense.setYear(current.getYear());
            expense.setRecurringExpense(recurringExpense);
            expenseRepository.save(expense);

            current = current.plusMonths(1);
        }
    }

    private void deleteExpensesForRecurringExpense(Long recurringExpenseId) {
        // Os lançamentos da recorrente são só dela: remove todos, sem tocar nos manuais.
        expenseRepository.deleteAll(expenseRepository.findByRecurringExpenseId(recurringExpenseId));
    }

    public List<RecurringExpenseResponse> getAllRecurringExpenses() {
        return recurringExpenseRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public RecurringExpenseResponse getRecurringExpenseById(Long id) {
        RecurringExpense recurringExpense = recurringExpenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recurring expense not found"));
        return toResponse(recurringExpense);
    }

    private RecurringExpenseResponse toResponse(RecurringExpense recurringExpense) {
        return new RecurringExpenseResponse(
                recurringExpense.getId(),
                recurringExpense.getUser().getId(),
                recurringExpense.getUser().getName(),
                recurringExpense.getUser().getColor(),
                recurringExpense.getExpenseType().getId(),
                recurringExpense.getExpenseType().getName(),
                recurringExpense.getMonthlyAmount(),
                recurringExpense.getStartDate(),
                recurringExpense.getEndDate(),
                recurringExpense.getCreatedAt()
        );
    }
}

