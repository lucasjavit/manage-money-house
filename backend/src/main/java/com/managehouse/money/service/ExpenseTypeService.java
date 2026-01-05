package com.managehouse.money.service;

import com.managehouse.money.dto.ExpenseTypeRequest;
import com.managehouse.money.dto.ExpenseTypeResponse;
import com.managehouse.money.entity.ExpenseType;
import com.managehouse.money.repository.ExpenseRepository;
import com.managehouse.money.repository.ExpenseTypeRepository;
import com.managehouse.money.repository.ExtractTransactionRepository;
import com.managehouse.money.repository.RecurringExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseTypeService {
    private final ExpenseTypeRepository expenseTypeRepository;
    private final ExpenseRepository expenseRepository;
    private final RecurringExpenseRepository recurringExpenseRepository;
    private final ExtractTransactionRepository extractTransactionRepository;

    public List<ExpenseTypeResponse> getAll() {
        return expenseTypeRepository.findAll().stream()
                .map(et -> new ExpenseTypeResponse(et.getId(), et.getName()))
                .collect(Collectors.toList());
    }

    public Optional<ExpenseType> findById(Long id) {
        return expenseTypeRepository.findById(id);
    }

    @Transactional
    public ExpenseTypeResponse create(ExpenseTypeRequest request) {
        ExpenseType expenseType = new ExpenseType();
        expenseType.setName(request.getName());
        ExpenseType saved = expenseTypeRepository.save(expenseType);
        return new ExpenseTypeResponse(saved.getId(), saved.getName());
    }

    @Transactional
    public void delete(Long id) {
        // Verificar se existe algum registro associado a este tipo
        long expensesCount = expenseRepository.countByExpenseTypeId(id);
        long recurringExpensesCount = recurringExpenseRepository.countByExpenseTypeId(id);
        long extractTransactionsCount = extractTransactionRepository.countByExpenseTypeId(id);
        
        if (expensesCount > 0 || recurringExpensesCount > 0 || extractTransactionsCount > 0) {
            throw new RuntimeException("Não é possível excluir este tipo de despesa pois existem registros associados a ele em algum ano/mês.");
        }
        expenseTypeRepository.deleteById(id);
    }
}

