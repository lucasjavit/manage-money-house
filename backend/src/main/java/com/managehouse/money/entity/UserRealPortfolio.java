package com.managehouse.money.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_real_portfolios")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRealPortfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Integer reportMonth;

    @Column(nullable = false)
    private Integer reportYear;

    @Column(precision = 18, scale = 2)
    private BigDecimal totalStocks;

    @Column(precision = 18, scale = 2)
    private BigDecimal totalFiis;

    @Column(precision = 18, scale = 2)
    private BigDecimal totalFixedIncome;

    @Column(precision = 18, scale = 2)
    private BigDecimal totalFunds;

    @Column(precision = 18, scale = 2)
    private BigDecimal grandTotal;

    @Column(precision = 18, scale = 2)
    private BigDecimal totalDividends;

    @Column(columnDefinition = "TEXT")
    private String aiAnalysis;

    // Score de saude da carteira (0-100)
    @Column(precision = 5, scale = 2)
    private BigDecimal healthScore;

    @Column(columnDefinition = "TEXT")
    private String healthScoreDetails; // JSON com detalhes do score

    private LocalDateTime uploadedAt;
    private LocalDateTime analyzedAt;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }
}
