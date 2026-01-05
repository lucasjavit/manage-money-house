package com.managehouse.money.service;

import com.managehouse.money.dto.ExpenseTypeRequest;
import com.managehouse.money.dto.ExpenseTypeResponse;
import com.managehouse.money.entity.ExpenseType;
import com.managehouse.money.repository.ExpenseTypeRepository;
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
        expenseTypeRepository.deleteById(id);
    }
}

