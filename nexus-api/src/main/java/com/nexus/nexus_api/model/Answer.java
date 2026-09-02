package com.nexus.nexus_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "answers",
        indexes = {
                @Index(name = "idx_answer_user", columnList = "user_id"),
                @Index(name = "idx_answer_question", columnList = "question_id"),
                @Index(name = "idx_answer_mock_exam", columnList = "mock_exam_id")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String respostaEscolhida;

    @Column(nullable = false)
    private Boolean correta;

    @Column(nullable = false)
    private LocalDateTime respondidoEm;

    private Integer tempoSegundos;

    private Integer numeroTentativa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    /** Nulo quando a resposta foi dada em prática avulsa (fora de um simulado). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mock_exam_id")
    private MockExam mockExam;
}
