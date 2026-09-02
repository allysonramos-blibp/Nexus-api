package com.nexus.nexus_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "study_plans",
        indexes = { @Index(name = "idx_study_plan_user", columnList = "user_id") }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StudyPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    private String objetivo;

    private Integer horasDisponiveis;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private StudyPlanStatus status = StudyPlanStatus.PLANEJADO;

    private LocalDate dataInicio;

    private LocalDate dataFim;

    @Builder.Default
    @Column(nullable = false)
    private Boolean ativo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
