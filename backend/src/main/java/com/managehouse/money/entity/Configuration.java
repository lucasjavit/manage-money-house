package com.managehouse.money.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "configurations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Configuration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String key;

    @Column(columnDefinition = "TEXT")
    private String value;

    @Column(length = 500)
    private String description;
}

