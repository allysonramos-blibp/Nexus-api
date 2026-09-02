package com.nexus.nexus_api.dto;

import com.nexus.nexus_api.model.Workout;

import java.time.LocalDate;
import java.util.List;

public record WorkoutResponse(
        Long id,
        String grupoMuscular,
        String exerciciosExecutados,
        LocalDate dataTreino,
        Boolean concluido,
        String imagemUrl,
        List<WorkoutExerciseDto> exercicios,
        Long userId
) {
    public static WorkoutResponse from(Workout workout) {
        return new WorkoutResponse(
                workout.getId(),
                workout.getGrupoMuscular(),
                workout.getExerciciosExecutados(),
                workout.getDataTreino(),
                workout.getConcluido(),
                workout.getImagemUrl(),
                workout.getExercicios().stream().map(WorkoutExerciseDto::from).toList(),
                workout.getUser().getId()
        );
    }
}
