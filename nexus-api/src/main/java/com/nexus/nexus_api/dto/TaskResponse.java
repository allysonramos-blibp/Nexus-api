package com.nexus.nexus_api.dto;

import com.nexus.nexus_api.model.Task;
import com.nexus.nexus_api.model.TaskPriority;
import com.nexus.nexus_api.model.TaskStatus;

import java.time.LocalDate;

public record TaskResponse(
        Long id,
        String titulo,
        String descricao,
        TaskStatus status,
        TaskPriority prioridade,
        LocalDate dataLimite,
        Boolean ehTopicoEdital,
        Long userId
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitulo(),
                task.getDescricao(),
                task.getStatus(),
                task.getPrioridade(),
                task.getDataLimite(),
                task.getEhTopicoEdital(),
                task.getUser().getId()
        );
    }
}
