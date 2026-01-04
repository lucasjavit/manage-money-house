package com.managehouse.money.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "salary_deductions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalaryDeduction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 500)
    private String description; // Nome/descrição do boleto

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount; // Valor do boleto

    @Column(nullable = false)
    private LocalDate dueDate; // Data de vencimento

    @Column(nullable = false)
    private Integer month; // Mês vinculado

    @Column(nullable = false)
    private Integer year; // Ano vinculado

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

