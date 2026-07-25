package com.managehouse.money.service;

import com.managehouse.money.dto.*;
import com.managehouse.money.entity.CompanyCategory;
import com.managehouse.money.entity.CompanyExpense;
import com.managehouse.money.entity.User;
import com.managehouse.money.repository.CompanyCategoryRepository;
import com.managehouse.money.repository.CompanyExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyExpenseService {

    private final CompanyExpenseRepository expenseRepository;
    private final CompanyCategoryRepository categoryRepository;
    private final UserService userService;

    // ---- Categorias ----

    @Transactional
    public CompanyCategoryResponse createCategory(CompanyCategoryRequest request) {
        User user = userService.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        if (isBlank(request.getName())) {
            throw new IllegalArgumentException("Nome da categoria é obrigatório");
        }
        CompanyCategory c = new CompanyCategory();
        c.setUser(user);
        c.setName(request.getName().trim());
        c.setType(normalizeType(request.getType()));
        return toCategoryResponse(categoryRepository.save(c));
    }

    public List<CompanyCategoryResponse> getCategories(Long userId) {
        return categoryRepository.findByUserId(userId).stream()
                .map(this::toCategoryResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteCategory(Long id) {
        if (expenseRepository.countByCategoryId(id) > 0) {
            throw new IllegalArgumentException("Categoria em uso por lançamentos; remova-os primeiro");
        }
        categoryRepository.deleteById(id);
    }

    // ---- Lançamentos ----

    @Transactional
    public CompanyExpenseResponse create(CompanyExpenseRequest request) {
        User user = userService.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        CompanyCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada"));
        validate(request);

        CompanyExpense e = new CompanyExpense();
        e.setUser(user);
        e.setCategory(category);
        e.setDescription(request.getDescription().trim());
        e.setAmount(request.getAmount());
        e.setDueDate(request.getDueDate());
        e.setMonth(request.getMonth());
        e.setYear(request.getYear());
        return toResponse(expenseRepository.save(e));
    }

    @Transactional
    public CompanyExpenseResponse update(Long id, CompanyExpenseRequest request) {
        CompanyExpense e = expenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lançamento não encontrado"));
        if (request.getCategoryId() != null) {
            e.setCategory(categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada")));
        }
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            e.setDescription(request.getDescription().trim());
        }
        if (request.getAmount() != null) {
            if (request.getAmount().signum() <= 0) {
                throw new IllegalArgumentException("amount deve ser maior que zero");
            }
            e.setAmount(request.getAmount());
        }
        if (request.getDueDate() != null) e.setDueDate(request.getDueDate());
        if (request.getMonth() != null) e.setMonth(request.getMonth());
        if (request.getYear() != null) e.setYear(request.getYear());
        return toResponse(expenseRepository.save(e));
    }

    public List<CompanyExpenseResponse> getByMonth(Long userId, Integer year, Integer month) {
        return expenseRepository.findByUserAndMonth(userId, year, month).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long id) {
        expenseRepository.deleteById(id);
    }

    // ---- helpers ----

    private void validate(CompanyExpenseRequest r) {
        if (isBlank(r.getDescription())) throw new IllegalArgumentException("Descrição é obrigatória");
        if (r.getAmount() == null || r.getAmount().signum() <= 0)
            throw new IllegalArgumentException("Valor deve ser maior que zero");
        if (r.getDueDate() == null) throw new IllegalArgumentException("Vencimento é obrigatório");
        if (r.getMonth() == null || r.getYear() == null)
            throw new IllegalArgumentException("Mês e ano são obrigatórios");
    }

    private String normalizeType(String t) {
        String up = t == null ? "CONTA" : t.trim().toUpperCase();
        return up.equals("IMPOSTO") ? "IMPOSTO" : "CONTA";
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private CompanyCategoryResponse toCategoryResponse(CompanyCategory c) {
        return new CompanyCategoryResponse(c.getId(), c.getName(), c.getType());
    }

    private CompanyExpenseResponse toResponse(CompanyExpense e) {
        return new CompanyExpenseResponse(
                e.getId(),
                e.getCategory().getId(),
                e.getCategory().getName(),
                e.getCategory().getType(),
                e.getDescription(),
                e.getAmount(),
                e.getDueDate(),
                e.getMonth(),
                e.getYear(),
                e.getCreatedAt());
    }
}
