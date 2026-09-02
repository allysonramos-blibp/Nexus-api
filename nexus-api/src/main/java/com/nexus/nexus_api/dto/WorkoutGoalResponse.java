package com.nexus.nexus_api.dto;

import com.nexus.nexus_api.model.WorkoutGoal;

public record WorkoutGoalResponse(
        Integer metaTreinosPorSemana
) {
    public static WorkoutGoalResponse from(WorkoutGoal goal) {
        return new WorkoutGoalResponse(goal.getMetaTreinosPorSemana());
    }
}
