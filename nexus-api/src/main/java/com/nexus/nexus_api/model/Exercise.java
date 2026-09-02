package com.nexus.nexus_api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "exercises",
        indexes = {
                @Index(name = "idx_exercise_user", columnList = "user_id"),
                @Index(name = "idx_exercise_grupo_muscular", columnList = "grupo_muscular")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(name = "grupo_muscular")
    private String grupoMuscular;

    @Column(columnDefinition = "TEXT")
    private String descricaoExecucao;

    /** Nulo = exercício padrão do catálogo do sistema. Preenchido = exercício customizado do usuário. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
