package com.nexus.nexus_api.controller;

import com.nexus.nexus_api.dto.AnswerRequest;
import com.nexus.nexus_api.dto.AnswerResponse;
import com.nexus.nexus_api.service.AnswerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/answers")
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerService answerService;

    @PostMapping
    public ResponseEntity<AnswerResponse> register(@Valid @RequestBody AnswerRequest request) {
        var saved = answerService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(AnswerResponse.from(saved));
    }

    @GetMapping
    public List<AnswerResponse> listMine() {
        return answerService.listMine().stream().map(AnswerResponse::from).toList();
    }

    @GetMapping("/{id}")
    public AnswerResponse getOne(@PathVariable Long id) {
        return AnswerResponse.from(answerService.findByIdOwnedByCurrentUser(id));
    }
}
