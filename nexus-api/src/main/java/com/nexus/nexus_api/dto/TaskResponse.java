package com.nexus.nexus_api.dto;

import com.nexus.nexus_api.model.Task;
import com.nexus.nexus_api.model.TaskPriority;
import com.nexus.nexus_api.model.TaskStatus;
import com.nexus.nexus_api.model.TaskWorkflowStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record TaskResponse(
        Long id,
        String titulo,
        String descricao,
        TaskStatus status,
        TaskWorkflowStatus workflowStatus,
        TaskPriority prioridade,
        LocalDate dataLimite,
        LocalTime horario,
        LocalDateTime concluidaEm,
        Boolean ehTopicoEdital,
        Long categoryId,
        String categoryNome,
        String categoryCor,
        Long userId
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitulo(),
                task.getDescricao(),
                task.getStatus(),
                task.getWorkflowStatus(),
                task.getPrioridade(),
                task.getDataLimite(),
                task.getHorario(),
                task.getConcluidaEm(),
                task.getEhTopicoEdital(),
                task.getCategory() != null ? task.getCategory().getId() : null,
                task.getCategory() != null ? task.getCategory().getNome() : null,
                task.getCategory() != null ? task.getCategory().getCor() : null,
                task.getUser().getId()
        );
    }
}
