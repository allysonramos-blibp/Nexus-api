package com.nexus.nexus_api.model;

public enum TransactionStatus {
    /** Ainda não aconteceu — é uma "conta a pagar" ou "a receber". */
    PENDENTE,
    /** Já foi paga/recebida — entra no saldo realizado. */
    CONCLUIDA
}
