package com.nexus.nexus_api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record WorkoutRequest(

        @NotBlank(message = "O grupo muscular é obrigatório.")
        String grupoMuscular,

        String exerciciosExecutados,

        @NotNull(message = "A data do treino é obrigatória.")
        LocalDate dataTreino,

        @NotNull(message = "Informe se o treino foi concluído.")
        Boolean concluido,

        @Valid
        List<WorkoutExerciseDto> exercicios
) {}
