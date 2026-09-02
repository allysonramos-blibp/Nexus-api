package com.nexus.nexus_api.dto;

import com.nexus.nexus_api.model.FinancialTransaction;
import com.nexus.nexus_api.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FinancialTransactionResponse(
        Long id,
        String descricao,
        BigDecimal valor,
        TransactionType tipo,
        LocalDate data,
        Long userId
) {
    public static FinancialTransactionResponse from(FinancialTransaction tx) {
        return new FinancialTransactionResponse(
                tx.getId(),
                tx.getDescricao(),
                tx.getValor(),
                tx.getTipo(),
                tx.getData(),
                tx.getUser().getId()
        );
    }
}
