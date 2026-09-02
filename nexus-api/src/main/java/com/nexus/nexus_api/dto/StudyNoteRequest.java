package com.nexus.nexus_api.dto;

import jakarta.validation.constraints.NotBlank;

public record StudyNoteRequest(

        @NotBlank(message = "O título é obrigatório.")
        String titulo,

        String conteudo
) {}
