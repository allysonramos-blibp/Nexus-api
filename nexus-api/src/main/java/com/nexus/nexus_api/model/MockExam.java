package com.nexus.nexus_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "mock_exams",
        indexes = { @Index(name = "idx_mock_exam_user", columnList = "user_id") }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MockExam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private LocalDate dataRealizacao;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private MockExamStatus status = MockExamStatus.CRIADO;

    private Integer duracaoMinutos;

    private Integer totalQuestoes;

    private Integer acertos;

    private BigDecimal notaObtida;

    private LocalDateTime iniciadoEm;

    private LocalDateTime finalizadoEm;

    /** Plano de estudo de referência (opcional — um simulado pode ser avulso). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_plan_id")
    private StudyPlan studyPlan;

    /** Matérias selecionadas para sorteio das questões do simulado. */
    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "mock_exam_subjects",
            joinColumns = @JoinColumn(name = "mock_exam_id"),
            inverseJoinColumns = @JoinColumn(name = "subject_id")
    )
    private List<Subject> subjects = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
