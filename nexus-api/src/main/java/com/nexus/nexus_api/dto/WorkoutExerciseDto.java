package com.nexus.nexus_api.dto;

import com.nexus.nexus_api.model.WorkoutExercise;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record WorkoutExerciseDto(
        Long id,

        @NotBlank(message = "O nome do exercício é obrigatório.")
        String nome,

        @NotNull(message = "O número de séries é obrigatório.")
        @Positive(message = "O número de séries deve ser positivo.")
        Integer series,

        @NotNull(message = "O número de repetições é obrigatório.")
        @Positive(message = "O número de repetições deve ser positivo.")
        Integer repeticoes,

        BigDecimal carga
) {
    public static WorkoutExerciseDto from(WorkoutExercise exercise) {
        return new WorkoutExerciseDto(
                exercise.getId(),
                exercise.getNome(),
                exercise.getSeries(),
                exercise.getRepeticoes(),
                exercise.getCarga()
        );
    }
}
