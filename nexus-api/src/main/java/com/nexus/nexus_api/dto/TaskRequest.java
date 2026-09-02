package com.nexus.nexus_api.dto;

import com.nexus.nexus_api.model.TaskPriority;
import com.nexus.nexus_api.model.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record TaskRequest(

        @NotBlank(message = "O título é obrigatório.")
        String titulo,

        String descricao,

        @NotNull(message = "O status é obrigatório.")
        TaskStatus status,

        @NotNull(message = "A prioridade é obrigatória.")
        TaskPriority prioridade,

        LocalDate dataLimite,

        @NotNull(message = "Informe se a tarefa é um tópico do edital.")
        Boolean ehTopicoEdital
) {}
