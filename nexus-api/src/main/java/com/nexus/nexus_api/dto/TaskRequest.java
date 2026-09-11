package com.nexus.nexus_api.dto;

import com.nexus.nexus_api.model.TaskPriority;
import com.nexus.nexus_api.model.TaskStatus;
import com.nexus.nexus_api.model.TaskWorkflowStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record TaskRequest(

        @NotBlank(message = "O título é obrigatório.")
        String titulo,

        String descricao,

        // Só é considerado quando ehTopicoEdital = true. Para tarefas comuns, pode vir
        // null — o service resolve para PENDENTE internamente (campo legado, não é mais
        // usado pela UI de tarefas de rotina).
        TaskStatus status,

        // Só é considerado quando ehTopicoEdital = false. Pode vir null na criação —
        // o service resolve para PENDENTE.
        TaskWorkflowStatus workflowStatus,

        @NotNull(message = "A prioridade é obrigatória.")
        TaskPriority prioridade,

        LocalDate dataLimite,

        // Opcional — null significa "sem horário definido".
        LocalTime horario,

        @NotNull(message = "Informe se a tarefa é um tópico do edital.")
        Boolean ehTopicoEdital,

        // Opcional — categoria (CategoryType.TASK) do mesmo usuário.
        Long categoryId
) {}
