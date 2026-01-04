package com.managehouse.money.service;

import com.managehouse.money.dto.SalaryDeductionRequest;
import com.managehouse.money.dto.SalaryDeductionResponse;
import com.managehouse.money.entity.SalaryDeduction;
import com.managehouse.money.entity.User;
import com.managehouse.money.repository.SalaryDeductionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalaryDeductionService {
    private final SalaryDeductionRepository salaryDeductionRepository;
    private final UserService userService;

    @Transactional
    public SalaryDeductionResponse createDeduction(SalaryDeductionRequest request) {
        User user = userService.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        SalaryDeduction deduction = new SalaryDeduction();
        deduction.setUser(user);
        deduction.setDescription(request.getDescription());
        deduction.setAmount(request.getAmount());
        deduction.setDueDate(request.getDueDate());
        deduction.setMonth(request.getMonth());
        deduction.setYear(request.getYear());

        SalaryDeduction saved = salaryDeductionRepository.save(deduction);
        return toResponse(saved);
    }

    public List<SalaryDeductionResponse> getDeductionsByMonthAndYear(Long userId, Integer month, Integer year) {
        List<SalaryDeduction> deductions = salaryDeductionRepository.findByUserIdAndMonthAndYear(userId, month, year);
        return deductions.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteDeduction(Long id) {
        salaryDeductionRepository.deleteById(id);
    }

    public java.math.BigDecimal getTotalDeductionsByMonthAndYear(Long userId, Integer month, Integer year) {
        List<SalaryDeduction> deductions = salaryDeductionRepository.findByUserIdAndMonthAndYear(userId, month, year);
        return deductions.stream()
                .map(SalaryDeduction::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }

    private SalaryDeductionResponse toResponse(SalaryDeduction deduction) {
        return new SalaryDeductionResponse(
                deduction.getId(),
                deduction.getUser().getId(),
                deduction.getUser().getName(),
                deduction.getDescription(),
                deduction.getAmount(),
                deduction.getDueDate(),
                deduction.getMonth(),
                deduction.getYear(),
                deduction.getCreatedAt()
        );
    }
}

