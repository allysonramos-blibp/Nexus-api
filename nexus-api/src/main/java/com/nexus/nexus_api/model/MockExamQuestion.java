package com.nexus.nexus_api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "mock_exam_questions",
        indexes = {
                @Index(name = "idx_meq_mock_exam", columnList = "mock_exam_id"),
                @Index(name = "idx_meq_question", columnList = "question_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_meq_exam_question", columnNames = {"mock_exam_id", "question_id"})
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MockExamQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Ordem de exibição da questão dentro do simulado. */
    @Column(nullable = false)
    private Integer ordem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mock_exam_id", nullable = false)
    private MockExam mockExam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;
}
