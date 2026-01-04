package com.managehouse.money.service;

import com.managehouse.money.dto.ExpenseTypeResponse;
import com.managehouse.money.entity.ExpenseType;
import com.managehouse.money.repository.ExpenseTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}

