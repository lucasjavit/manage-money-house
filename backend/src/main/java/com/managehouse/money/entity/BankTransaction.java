package com.managehouse.money.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Transação capturada pelo app Android a partir de notificações de banco.
 * Entidade separada de ExtractTransaction (aba Cartão de Crédito) para não poluir
 * aquela aba nem sofrer com o delete em massa dela.
 *
 * externalId é um hash gerado no celular (packageName + texto + minuto), garantindo
 * idempotência: a mesma notificação reenviada não duplica.
 */
@Entity
@Table(name = "bank_transactions", uniqueConstraints = {
        @UniqueConstraint(name = "ux_bank_tx_external_id", columnNames = "external_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(length = 500)
    private String description;

    // Nulo quando a IA não conseguiu extrair (needsReview = true).
    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate transactionDate;

    // true quando a IA não extraiu o valor: aguarda o usuário completar antes de virar lançamento.
    @Column(nullable = false)
    private boolean needsReview = false;

    // packageName do app de banco que originou a notificação (ex: com.nubank.app)
    @Column(name = "source_package")
    private String sourcePackage;

    // Texto cru da notificação, para auditoria e reprocessamento futuro
    @Column(columnDefinition = "TEXT")
    private String rawText;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
