package com.nexus.nexus_api.controller;

import com.nexus.nexus_api.dto.StudyNoteRequest;
import com.nexus.nexus_api.dto.StudyNoteResponse;
import com.nexus.nexus_api.model.StudyNote;
import com.nexus.nexus_api.service.StudyNoteService;
import com.nexus.nexus_api.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/study-notes")
@RequiredArgsConstructor
public class StudyNoteController {

    private final StudyNoteService studyNoteService;

    @PostMapping
    public ResponseEntity<StudyNoteResponse> create(@Valid @RequestBody StudyNoteRequest request) {
        StudyNote created = studyNoteService.create(request, SecurityUtils.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(StudyNoteResponse.from(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StudyNoteResponse> update(@PathVariable Long id, @Valid @RequestBody StudyNoteRequest request) {
        StudyNote updated = studyNoteService.update(id, request);
        return ResponseEntity.ok(StudyNoteResponse.from(updated));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<StudyNoteResponse>> listByUser(@PathVariable Long userId) {
        List<StudyNoteResponse> notes = studyNoteService.listByUser(userId).stream()
                .map(StudyNoteResponse::from)
                .toList();
        return ResponseEntity.ok(notes);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        studyNoteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
