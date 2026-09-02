package com.nexus.nexus_api.dto;

import com.nexus.nexus_api.model.StudyFile;

import java.time.LocalDateTime;

public record StudyFileResponse(
        Long id,
        String nomeOriginal,
        String tipoConteudo,
        LocalDateTime dataUpload,
        Long userId
) {
    public static StudyFileResponse from(StudyFile file) {
        return new StudyFileResponse(
                file.getId(),
                file.getNomeOriginal(),
                file.getTipoConteudo(),
                file.getDataUpload(),
                file.getUser().getId()
        );
    }
}
