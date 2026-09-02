package com.nexus.nexus_api.controller;

import com.nexus.nexus_api.dto.*;
import com.nexus.nexus_api.model.MockExam;
import com.nexus.nexus_api.model.MockExamStatus;
import com.nexus.nexus_api.service.MockExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mock-exams")
@RequiredArgsConstructor
public class MockExamController {

    private final MockExamService mockExamService;

    @PostMapping
    public ResponseEntity<MockExamResponse> create(@Valid @RequestBody MockExamRequest request) {
        MockExam created = mockExamService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(MockExamResponse.from(created));
    }

    @GetMapping
    public List<MockExamResponse> listMine() {
        return mockExamService.listMine().stream().map(MockExamResponse::from).toList();
    }

    @GetMapping("/{id}")
    public MockExamDetailResponse getDetail(@PathVariable Long id) {
        MockExam exam = mockExamService.findByIdOwnedByCurrentUser(id);
        boolean finalizado = exam.getStatus() == MockExamStatus.FINALIZADO;

        List<QuestionResponse> questoes = mockExamService.listQuestions(id).stream()
                .map(q -> finalizado ? QuestionResponse.from(q) : QuestionResponse.fromWithoutGabarito(q))
                .toList();

        return new MockExamDetailResponse(MockExamResponse.from(exam), questoes);
    }

    @PostMapping("/{id}/iniciar")
    public MockExamResponse iniciar(@PathVariable Long id) {
        return MockExamResponse.from(mockExamService.iniciar(id));
    }

    @PostMapping("/{id}/finalizar")
    public MockExamResponse finalizar(@PathVariable Long id) {
        return MockExamResponse.from(mockExamService.finalizar(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        mockExamService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
