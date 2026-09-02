package com.nexus.nexus_api.controller;

import com.nexus.nexus_api.dto.TopicRequest;
import com.nexus.nexus_api.dto.TopicResponse;
import com.nexus.nexus_api.service.TopicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;

    @PostMapping("/api/subjects/{subjectId}/topics")
    public ResponseEntity<TopicResponse> create(@PathVariable Long subjectId, @Valid @RequestBody TopicRequest request) {
        var created = topicService.create(subjectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(TopicResponse.from(created));
    }

    @GetMapping("/api/subjects/{subjectId}/topics")
    public List<TopicResponse> listBySubject(@PathVariable Long subjectId) {
        return topicService.listBySubject(subjectId).stream().map(TopicResponse::from).toList();
    }

    @PutMapping("/api/topics/{id}")
    public TopicResponse update(@PathVariable Long id, @Valid @RequestBody TopicRequest request) {
        return TopicResponse.from(topicService.update(id, request));
    }

    @DeleteMapping("/api/topics/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        topicService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
