package com.nexus.nexus_api.model;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "workout_goals")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkoutGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer metaTreinosPorSemana;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
}