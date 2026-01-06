package com.managehouse.money.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "salary_conversions", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "month", "year"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalaryConversion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer month; // 1-12

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private LocalDate conversionDate; // Data da conversão

    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal exchangeRate; // Cotação

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amountUSD; // Valor do saque em USD

    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal vet; // VET (Valor Efetivo da Taxa)

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal finalAmountBRL; // Valor convertido final em BRL

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

