package com.nexus.nexus_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "study_errors",
        indexes = {
                @Index(name = "idx_study_error_user", columnList = "user_id"),
                @Index(name = "idx_study_error_question", columnList = "question_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_study_error_user_question", columnNames = {"user_id", "question_id"})
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StudyError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String anotacao;

    @Enumerated(EnumType.STRING)
    private ErrorReason motivo;

    private LocalDate proximaRevisao;

    @Builder.Default
    @Column(nullable = false)
    private Boolean resolvido = false;

    @Column(nullable = false)
    private LocalDateTime criadoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    /** A resposta específica que gerou esta entrada no caderno de erros, se houver. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id")
    private Answer answer;
}
