package com.nexus.nexus_api.controller;

import com.nexus.nexus_api.dto.PdfExtractionResponse;
import com.nexus.nexus_api.dto.QuestionRequest;
import com.nexus.nexus_api.dto.QuestionResponse;
import com.nexus.nexus_api.service.PdfQuestionExtractionService;
import com.nexus.nexus_api.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;
    private final PdfQuestionExtractionService pdfQuestionExtractionService;

    @PostMapping("/api/topics/{topicId}/questions")
    public ResponseEntity<QuestionResponse> create(@PathVariable Long topicId, @Valid @RequestBody QuestionRequest request) {
        var created = questionService.create(topicId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(QuestionResponse.from(created));
    }

    /** Cria várias questões de uma vez (usado depois da importação por PDF, já revisada pelo usuário). */
    @PostMapping("/api/topics/{topicId}/questions/bulk")
    public ResponseEntity<List<QuestionResponse>> createBulk(@PathVariable Long topicId, @RequestBody List<QuestionRequest> requests) {
        var created = questionService.createBulk(topicId, requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(created.stream().map(QuestionResponse::from).toList());
    }

    /**
     * Extrai questões de um PDF via IA — só devolve o que foi encontrado, NÃO salva nada.
     * O usuário revisa/edita no frontend e confirma com POST .../questions/bulk.
     */
    @PostMapping(value = "/api/questions/extract-pdf", consumes = "multipart/form-data")
    public PdfExtractionResponse extractFromPdf(@RequestParam("file") MultipartFile file) {
        var questoes = pdfQuestionExtractionService.extract(file);
        return PdfExtractionResponse.of(questoes);
    }

    @GetMapping("/api/topics/{topicId}/questions")
    public List<QuestionResponse> listByTopic(@PathVariable Long topicId) {
        return questionService.listByTopic(topicId).stream().map(QuestionResponse::from).toList();
    }

    @GetMapping("/api/questions/{id}")
    public QuestionResponse getOne(@PathVariable Long id) {
        return QuestionResponse.from(questionService.findByIdOwnedByCurrentUser(id));
    }

    @PutMapping("/api/questions/{id}")
    public QuestionResponse update(@PathVariable Long id, @Valid @RequestBody QuestionRequest request) {
        return QuestionResponse.from(questionService.update(id, request));
    }

    @DeleteMapping("/api/questions/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
