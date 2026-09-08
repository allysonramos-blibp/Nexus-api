package com.nexus.nexus_api.dto;

import com.nexus.nexus_api.model.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CategoryRequest(
        @NotBlank(message = "O nome é obrigatório.")
        String nome,

        @NotNull(message = "O tipo é obrigatório.")
        CategoryType tipo,

        // Opcional — cor em hex (ex.: "#22C55E") pra UI pintar o gráfico/badge.
        String cor
) {}
