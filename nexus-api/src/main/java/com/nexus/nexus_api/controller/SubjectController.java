package com.nexus.nexus_api.controller;

import com.nexus.nexus_api.dto.SubjectRequest;
import com.nexus.nexus_api.dto.SubjectResponse;
import com.nexus.nexus_api.service.SubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @PostMapping("/api/study-plans/{studyPlanId}/subjects")
    public ResponseEntity<SubjectResponse> create(@PathVariable Long studyPlanId, @Valid @RequestBody SubjectRequest request) {
        var created = subjectService.create(studyPlanId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SubjectResponse.from(created));
    }

    @GetMapping("/api/study-plans/{studyPlanId}/subjects")
    public List<SubjectResponse> listByStudyPlan(@PathVariable Long studyPlanId) {
        return subjectService.listByStudyPlan(studyPlanId).stream().map(SubjectResponse::from).toList();
    }

    @PutMapping("/api/subjects/{id}")
    public SubjectResponse update(@PathVariable Long id, @Valid @RequestBody SubjectRequest request) {
        return SubjectResponse.from(subjectService.update(id, request));
    }

    @DeleteMapping("/api/subjects/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        subjectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
