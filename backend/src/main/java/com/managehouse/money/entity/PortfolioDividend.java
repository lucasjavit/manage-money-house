package com.managehouse.money.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "portfolio_dividends")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioDividend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private UserRealPortfolio portfolio;

    private String ticker;

    private String productName;

    private LocalDate paymentDate;

    private String eventType; // Dividendo, JCP, Rendimento

    @Column(precision = 18, scale = 8)
    private BigDecimal quantity;

    @Column(precision = 18, scale = 4)
    private BigDecimal unitPrice;

    @Column(precision = 18, scale = 2)
    private BigDecimal netValue;
}
