package com.managehouse.money.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Parametrização do PTO do Lucas (Aditi). Um por usuário.
 * A partir de baseDate, acumula 1/25 de dia por dia corrido (200h/8h = 25 dias = 1 dia de PTO).
 */
@Entity
@Table(name = "pto_config", uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PtoConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // Data em que o saldo inicial vale.
    @Column(nullable = false)
    private LocalDate baseDate;

    // Saldo de PTO (em dias) na baseDate. Fração para acompanhar o acúmulo contínuo.
    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal initialBalance;

    // "BR" (só federais) ou "US".
    @Column(nullable = false, length = 2)
    private String country;

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
