package com.nexus.nexus_api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    public String store(MultipartFile file) {
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String storedFilename = UUID.randomUUID() + extractExtension(file.getOriginalFilename());
            Path destination = uploadPath.resolve(storedFilename);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            return storedFilename;
        } catch (IOException e) {
            throw new RuntimeException("Falha ao salvar o arquivo: " + file.getOriginalFilename(), e);
        }
    }

    public InputStream load(String storedFilename) {
        try {
            return Files.newInputStream(Paths.get(uploadDir).resolve(storedFilename));
        } catch (IOException e) {
            throw new RuntimeException("Arquivo não encontrado: " + storedFilename, e);
        }
    }

    public void delete(String storedFilename) {
        try {
            Files.deleteIfExists(Paths.get(uploadDir).resolve(storedFilename));
        } catch (IOException e) {
            throw new RuntimeException("Falha ao excluir o arquivo: " + storedFilename, e);
        }
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) return "";
        return originalFilename.substring(originalFilename.lastIndexOf('.'));
    }
}