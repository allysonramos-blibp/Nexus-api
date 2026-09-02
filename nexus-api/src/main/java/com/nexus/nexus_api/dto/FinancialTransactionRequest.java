package com.nexus.nexus_api.dto;

import com.nexus.nexus_api.model.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FinancialTransactionRequest(

        @NotBlank(message = "A descrição é obrigatória.")
        String descricao,

        @NotNull(message = "O valor é obrigatório.")
        @Positive(message = "O valor deve ser positivo.")
        BigDecimal valor,

        @NotNull(message = "O tipo é obrigatório.")
        TransactionType tipo,

        @NotNull(message = "A data é obrigatória.")
        LocalDate data
) {}
