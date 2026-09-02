package com.nexus.nexus_api.dto;

import com.nexus.nexus_api.model.StudyPlanStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

public record StudyPlanRequest(

        @NotBlank(message = "O nome do plano é obrigatório.")
        String nome,

        String objetivo,

        String descricao,

        LocalDate dataInicio,

        /** Data da prova, do exame final, ou de qualquer marco-objetivo do plano. */
        LocalDate dataAlvo,

        @PositiveOrZero(message = "Horas disponíveis não pode ser negativo.")
        Integer horasDisponiveis,

        StudyPlanStatus status
) {}
