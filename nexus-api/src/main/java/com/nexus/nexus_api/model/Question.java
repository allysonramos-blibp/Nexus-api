package com.nexus.nexus_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "questions",
        indexes = { @Index(name = "idx_question_topic", columnList = "topic_id") }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String enunciado;

    private Integer numero;

    private String banca;

    private Integer ano;

    @Enumerated(EnumType.STRING)
    private QuestionDifficulty dificuldade;

    /** Alternativas simples (sem identidade própria) — coleção mapeada em tabela auxiliar. */
    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "question_options", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "texto", columnDefinition = "TEXT")
    private List<String> alternativas = new ArrayList<>();

    /** Resposta correta (letra ou texto da alternativa). */
    private String gabarito;

    @Column(columnDefinition = "TEXT")
    private String explicacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;
}
