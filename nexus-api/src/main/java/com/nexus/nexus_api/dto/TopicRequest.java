package com.nexus.nexus_api.dto;

import jakarta.validation.constraints.NotBlank;

public record TopicRequest(

        @NotBlank(message = "O nome do assunto é obrigatório.")
        String nome,

        Integer ordem
) {}
