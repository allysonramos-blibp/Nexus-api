package com.nexus.nexus_api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "topics",
        indexes = { @Index(name = "idx_topic_subject", columnList = "subject_id") },
        uniqueConstraints = { @UniqueConstraint(name = "uk_topic_subject_nome", columnNames = {"subject_id", "nome"}) }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    /** Ordem de exibição/estudo dentro da matéria. */
    private Integer ordem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;
}
