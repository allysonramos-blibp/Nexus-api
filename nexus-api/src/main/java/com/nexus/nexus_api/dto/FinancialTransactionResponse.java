package com.nexus.nexus_api.dto;

import com.nexus.nexus_api.model.FinancialTransaction;
import com.nexus.nexus_api.model.TransactionStatus;
import com.nexus.nexus_api.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FinancialTransactionResponse(
        Long id,
        String descricao,
        BigDecimal valor,
        TransactionType tipo,
        TransactionStatus status,
        LocalDate data,
        Long categoryId,
        String categoryNome,
        String categoryCor,
        Long userId
) {
    public static FinancialTransactionResponse from(FinancialTransaction tx) {
        return new FinancialTransactionResponse(
                tx.getId(),
                tx.getDescricao(),
                tx.getValor(),
                tx.getTipo(),
                // Linhas gravadas antes de "status" existir ficam NULL no banco — tratamos
                // como CONCLUIDA (já realizada), que é o comportamento antigo implícito.
                tx.getStatus() != null ? tx.getStatus() : TransactionStatus.CONCLUIDA,
                tx.getData(),
                tx.getCategory() != null ? tx.getCategory().getId() : null,
                tx.getCategory() != null ? tx.getCategory().getNome() : null,
                tx.getCategory() != null ? tx.getCategory().getCor() : null,
                tx.getUser().getId()
        );
    }
}
