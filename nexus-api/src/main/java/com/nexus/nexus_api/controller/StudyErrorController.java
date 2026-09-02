package com.nexus.nexus_api.controller;

import com.nexus.nexus_api.dto.StudyErrorRequest;
import com.nexus.nexus_api.dto.StudyErrorResponse;
import com.nexus.nexus_api.service.StudyErrorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/study-errors")
@RequiredArgsConstructor
public class StudyErrorController {

    private final StudyErrorService studyErrorService;

    @PostMapping
    public ResponseEntity<StudyErrorResponse> registerOrUpdate(@Valid @RequestBody StudyErrorRequest request) {
        var saved = studyErrorService.registerOrUpdate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(StudyErrorResponse.from(saved));
    }

    @GetMapping
    public List<StudyErrorResponse> listMine() {
        return studyErrorService.listMine().stream().map(StudyErrorResponse::from).toList();
    }

    @GetMapping("/pendentes-revisao")
    public List<StudyErrorResponse> listPendentesRevisao() {
        return studyErrorService.listPendentesRevisao().stream().map(StudyErrorResponse::from).toList();
    }

    @PatchMapping("/{id}/resolver")
    public StudyErrorResponse markResolved(@PathVariable Long id) {
        return StudyErrorResponse.from(studyErrorService.markResolved(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        studyErrorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
