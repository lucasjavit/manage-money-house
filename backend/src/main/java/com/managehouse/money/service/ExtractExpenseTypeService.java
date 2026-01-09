package com.managehouse.money.service;

import com.managehouse.money.entity.ExtractExpenseType;
import com.managehouse.money.repository.ExtractExpenseTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExtractExpenseTypeService {
    private final ExtractExpenseTypeRepository extractExpenseTypeRepository;

    public List<ExtractExpenseType> getAll() {
        return extractExpenseTypeRepository.findAll();
    }

    public Optional<ExtractExpenseType> findById(Long id) {
        return extractExpenseTypeRepository.findById(id);
    }

    public Optional<ExtractExpenseType> findByName(String name) {
        return extractExpenseTypeRepository.findByName(name);
    }
}

