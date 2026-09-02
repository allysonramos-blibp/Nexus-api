package com.nexus.nexus_api.dto;

import jakarta.validation.constraints.NotBlank;

public record SubjectRequest(

        @NotBlank(message = "O nome da matéria é obrigatório.")
        String nome,

        Integer pesoNoEdital
) {}
