package com.nexus.nexus_api.controller;

import com.nexus.nexus_api.dto.StudyPlanRequest;
import com.nexus.nexus_api.dto.StudyPlanResponse;
import com.nexus.nexus_api.model.StudyPlan;
import com.nexus.nexus_api.service.StudyPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/study-plans")
@RequiredArgsConstructor
public class StudyPlanController {

    private final StudyPlanService studyPlanService;

    @PostMapping
    public ResponseEntity<StudyPlanResponse> create(@Valid @RequestBody StudyPlanRequest request) {
        StudyPlan created = studyPlanService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(studyPlanService.toResponse(created));
    }

    @GetMapping
    public List<StudyPlanResponse> listMine() {
        return studyPlanService.listMine().stream().map(studyPlanService::toResponse).toList();
    }

    @GetMapping("/{id}")
    public StudyPlanResponse getOne(@PathVariable Long id) {
        return studyPlanService.toResponse(studyPlanService.findByIdOwnedByCurrentUser(id));
    }

    @PutMapping("/{id}")
    public StudyPlanResponse update(@PathVariable Long id, @Valid @RequestBody StudyPlanRequest request) {
        return studyPlanService.toResponse(studyPlanService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        studyPlanService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
