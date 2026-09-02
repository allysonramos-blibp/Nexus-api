package com.nexus.nexus_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "workout_exercises")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkoutExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private Integer series;

    @Column(nullable = false)
    private Integer repeticoes;

    @Column(precision = 6, scale = 2)
    private BigDecimal carga;

    /** Vínculo opcional ao catálogo normalizado de exercícios. 'nome' acima é mantido como
     *  snapshot histórico do treino (não muda retroativamente se o catálogo for editado depois). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id")
    private Exercise exercise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_id", nullable = false)
    private Workout workout;
}