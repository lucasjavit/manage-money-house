package com.managehouse.money.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Categoria de conta da empresa, cadastrada pelo usuário (ex: Contador, Software, DAS).
 * Cada categoria é do tipo CONTA ou IMPOSTO.
 */
@Entity
@Table(name = "company_categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompanyCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    // "CONTA" ou "IMPOSTO"
    @Column(nullable = false, length = 20)
    private String type;
}
