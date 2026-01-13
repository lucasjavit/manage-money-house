package com.managehouse.money.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_portfolios")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPortfolio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String riskProfile; // CONSERVADOR, MODERADO, ARROJADO

    @Column(columnDefinition = "TEXT")
    private String assetsJson; // JSON com lista de ativos selecionados

    @Column(columnDefinition = "TEXT")
    private String aiRationale; // Explicacao da IA sobre a selecao

    @Column
    private Double expectedDY; // DY esperado da carteira

    @Column(columnDefinition = "TEXT")
    private String riskAssessment; // Avaliacao de risco pela IA

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
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
