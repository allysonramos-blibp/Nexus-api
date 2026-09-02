package com.nexus.nexus_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "financial_goals",
        indexes = { @Index(name = "idx_financial_goal_user", columnList = "user_id") }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FinancialGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valorAlvo;

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valorAtual = BigDecimal.ZERO;

    private LocalDate prazo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
