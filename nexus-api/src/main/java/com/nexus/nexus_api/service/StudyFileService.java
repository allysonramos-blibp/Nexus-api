package com.nexus.nexus_api.service;


import com.nexus.nexus_api.exception.ResourceNotFoundException;
import com.nexus.nexus_api.model.StudyFile;
import com.nexus.nexus_api.model.User;
import com.nexus.nexus_api.repository.StudyFileRepository;
import com.nexus.nexus_api.repository.UserRepository;
import com.nexus.nexus_api.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyFileService {

    private final StudyFileRepository studyFileRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public StudyFile upload(Long userId, MultipartFile file) {
        SecurityUtils.assertOwnership(userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + userId));

        String storedFilename = fileStorageService.store(file);

        StudyFile studyFile = StudyFile.builder()
                .nomeOriginal(file.getOriginalFilename())
                .nomeArmazenado(storedFilename)
                .tipoConteudo(file.getContentType())
                .dataUpload(LocalDateTime.now())
                .user(user)
                .build();

        return studyFileRepository.save(studyFile);
    }

    public List<StudyFile> listByUser(Long userId) {
        SecurityUtils.assertOwnership(userId);
        return studyFileRepository.findByUserIdOrderByDataUploadDesc(userId);
    }

    /** Uso interno (ex.: download) — não checa ownership sozinho, ver findByIdOwnedByCurrentUser. */
    public StudyFile findById(Long id) {
        return studyFileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Arquivo não encontrado com ID: " + id));
    }

    /** Busca o arquivo e garante que pertence ao usuário autenticado antes de devolvê-lo. */
    public StudyFile findByIdOwnedByCurrentUser(Long id) {
        StudyFile file = findById(id);
        SecurityUtils.assertOwnership(file.getUser().getId());
        return file;
    }

    public InputStream getFileStream(String storedFilename) {
        return fileStorageService.load(storedFilename);
    }

    public void delete(Long id) {
        StudyFile file = findByIdOwnedByCurrentUser(id);
        fileStorageService.delete(file.getNomeArmazenado());
        studyFileRepository.delete(file);
    }
}
