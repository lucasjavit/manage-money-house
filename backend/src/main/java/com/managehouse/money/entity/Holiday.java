package com.managehouse.money.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Feriado nacional (BR) ou federal (US), buscado da Nager.Date e cacheado no banco.
 * Cache permanente por (country, date) — sobrevive a restart, busca 1x por ano.
 */
@Entity
@Table(name = "holidays", uniqueConstraints = @UniqueConstraint(columnNames = {"country", "date"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Holiday {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2)
    private String country;

    @Column(nullable = false)
    private LocalDate date;

    @Column(length = 200)
    private String name;

    @Column(nullable = false)
    private Integer year;
}
