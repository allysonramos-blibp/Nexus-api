package com.nexus.nexus_api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "subjects",
        indexes = { @Index(name = "idx_subject_study_plan", columnList = "study_plan_id") },
        uniqueConstraints = { @UniqueConstraint(name = "uk_subject_plan_nome", columnNames = {"study_plan_id", "nome"}) }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    /** Peso da matéria no edital (opcional, usado para priorizar estudo). */
    private Integer pesoNoEdital;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_plan_id", nullable = false)
    private StudyPlan studyPlan;
}
