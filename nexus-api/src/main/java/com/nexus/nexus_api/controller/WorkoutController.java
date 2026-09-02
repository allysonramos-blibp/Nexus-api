package com.nexus.nexus_api.controller;

import com.nexus.nexus_api.dto.WorkoutRequest;
import com.nexus.nexus_api.dto.WorkoutResponse;
import com.nexus.nexus_api.model.Workout;
import com.nexus.nexus_api.service.WorkoutService;
import com.nexus.nexus_api.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/workouts")
@RequiredArgsConstructor
public class WorkoutController {

    private final WorkoutService workoutService;

    @PostMapping
    public ResponseEntity<WorkoutResponse> create(@Valid @RequestBody WorkoutRequest request) {
        Workout saved = workoutService.create(request, SecurityUtils.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(WorkoutResponse.from(saved));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<WorkoutResponse>> listByUser(@PathVariable Long userId) {
        List<WorkoutResponse> workouts = workoutService.listByUser(userId).stream()
                .map(WorkoutResponse::from)
                .toList();
        return ResponseEntity.ok(workouts);
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<WorkoutResponse> uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        Workout updated = workoutService.uploadImage(id, file);
        return ResponseEntity.ok(WorkoutResponse.from(updated));
    }

    @GetMapping("/image/{filename}")
    public ResponseEntity<InputStreamResource> getImage(@PathVariable String filename) {
        InputStreamResource resource = new InputStreamResource(workoutService.getImageStream(filename));
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }
}
