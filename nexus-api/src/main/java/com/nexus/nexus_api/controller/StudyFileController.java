package com.nexus.nexus_api.controller;

import com.nexus.nexus_api.dto.StudyFileResponse;
import com.nexus.nexus_api.model.StudyFile;
import com.nexus.nexus_api.service.StudyFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/study-files")
@RequiredArgsConstructor
public class StudyFileController {

    private final StudyFileService studyFileService;

    @PostMapping(value = "/upload/user/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudyFileResponse> upload(@PathVariable Long userId, @RequestParam("file") MultipartFile file) {
        StudyFile saved = studyFileService.upload(userId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(StudyFileResponse.from(saved));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<StudyFileResponse>> listByUser(@PathVariable Long userId) {
        List<StudyFileResponse> files = studyFileService.listByUser(userId).stream()
                .map(StudyFileResponse::from)
                .toList();
        return ResponseEntity.ok(files);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long id) {
        // findByIdOwnedByCurrentUser barra com 403 se o arquivo pertencer a outro usuário.
        StudyFile file = studyFileService.findByIdOwnedByCurrentUser(id);
        InputStreamResource resource = new InputStreamResource(studyFileService.getFileStream(file.getNomeArmazenado()));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getTipoConteudo()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getNomeOriginal() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        studyFileService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
