package com.nexus.nexus_api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    // Progresso de estudo do tópico de edital (TEORIA_VISTA/QUESTOES_FEITAS/DOMINADO).
    // Continua NOT NULL e com os mesmos valores de sempre — nada mudou aqui.
    // Usado exclusivamente quando ehTopicoEdital = true.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    // Status de fluxo de uma tarefa de rotina comum (Tarefas V1). Nullable de propósito:
    // registros existentes ficam com NULL, sem precisar de migration/backfill. Usado
    // exclusivamente quando ehTopicoEdital = false.
    @Enumerated(EnumType.STRING)
    private TaskWorkflowStatus workflowStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskPriority prioridade;

    private LocalDate dataLimite;

    // Horário opcional da tarefa (ex.: 19:30). Null = tarefa sem horário definido.
    private LocalTime horario;

    // Preenchido quando workflowStatus vira CONCLUIDA; limpo se sair desse estado.
    private LocalDateTime concluidaEm;

    @Column(nullable = false)
    private Boolean ehTopicoEdital;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
