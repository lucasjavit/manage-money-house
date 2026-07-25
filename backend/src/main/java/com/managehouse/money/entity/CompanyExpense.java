package com.managehouse.money.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Conta a pagar ou imposto da empresa (LTDA) do Lucas. Lançamento manual por mês,
 * com categoria livre (ex: Contador, Software, DAS, ISS). Sem cálculo tributário.
 */
@Entity
@Table(name = "company_expenses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompanyExpense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Categoria escolhida no select (cadastrada pelo usuário).
    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private CompanyCategory category;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false)
    private Integer month;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
