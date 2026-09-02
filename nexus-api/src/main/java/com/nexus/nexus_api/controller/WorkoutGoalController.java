package com.nexus.nexus_api.controller;


import com.nexus.nexus_api.dto.WorkoutGoalResponse;
import com.nexus.nexus_api.service.WorkoutGoalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/workout-goals")
@RequiredArgsConstructor
public class WorkoutGoalController {

    private final WorkoutGoalService workoutGoalService;

    @PutMapping("/user/{userId}")
    public ResponseEntity<WorkoutGoalResponse> setGoal(@PathVariable Long userId, @RequestBody Map<String, Integer> body) {
        var goal = workoutGoalService.setGoal(userId, body.get("metaTreinosPorSemana"));
        return ResponseEntity.ok(WorkoutGoalResponse.from(goal));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<WorkoutGoalResponse> getGoal(@PathVariable Long userId) {
        var goal = workoutGoalService.getGoal(userId);
        return ResponseEntity.ok(WorkoutGoalResponse.from(goal));
    }
}
