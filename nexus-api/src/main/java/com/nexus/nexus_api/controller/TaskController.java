package com.nexus.nexus_api.controller;

import com.nexus.nexus_api.dto.TaskRequest;
import com.nexus.nexus_api.dto.TaskResponse;
import com.nexus.nexus_api.model.Task;
import com.nexus.nexus_api.model.TaskStatus;
import com.nexus.nexus_api.service.TaskService;
import com.nexus.nexus_api.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskRequest request) {
        Task saved = taskService.create(request, SecurityUtils.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(TaskResponse.from(saved));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TaskResponse>> listByUser(@PathVariable Long userId) {
        List<TaskResponse> tasks = taskService.listByUser(userId).stream()
                .map(TaskResponse::from)
                .toList();
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/user/{userId}/edital")
    public ResponseEntity<List<TaskResponse>> listEditalProgress(@PathVariable Long userId) {
        List<TaskResponse> tasks = taskService.listEditalProgress(userId).stream()
                .map(TaskResponse::from)
                .toList();
        return ResponseEntity.ok(tasks);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TaskResponse> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String statusValue = body.get("status");
        if (statusValue == null || statusValue.isBlank()) {
            throw new IllegalArgumentException("O campo 'status' é obrigatório.");
        }
        TaskStatus status = TaskStatus.valueOf(statusValue);
        Task updated = taskService.updateStatus(id, status);
        return ResponseEntity.ok(TaskResponse.from(updated));
    }
}
