package com.nexus.nexus_api.dto;

import com.nexus.nexus_api.model.ErrorReason;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record StudyErrorRequest(

        @NotNull(message = "O id da questão é obrigatório.")
        Long questionId,

        Long answerId,

        @NotNull(message = "O motivo do erro é obrigatório.")
        ErrorReason motivo,

        String observacao,

        /** Se não informado, o serviço agenda automaticamente (hoje + 3 dias). */
        LocalDate proximaRevisao
) {}
