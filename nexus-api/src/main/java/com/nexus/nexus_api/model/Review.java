package com.nexus.nexus_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "reviews",
        indexes = {
                @Index(name = "idx_review_user", columnList = "user_id"),
                @Index(name = "idx_review_data_agendada", columnList = "data_agendada")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate dataAgendada;

    @Builder.Default
    @Column(nullable = false)
    private Boolean concluida = false;

    private LocalDate dataConclusao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Revisão geral de um assunto. Nulo se a revisão for de um erro específico. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    /** Revisão de um erro específico do caderno de erros. Nulo se for revisão geral de assunto. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_error_id")
    private StudyError studyError;
}
