package com.nexus.nexus_api.model;

/**
 * Status de fluxo de uma tarefa de rotina comum (ehTopicoEdital = false).
 * Não confundir com {@link TaskStatus}, que é o progresso de estudo de um
 * tópico do edital (TEORIA_VISTA/QUESTOES_FEITAS/DOMINADO) — os dois convivem
 * no mesmo Task, cada um usado exclusivamente por um tipo de tarefa.
 */
public enum TaskWorkflowStatus {
    PENDENTE,
    EM_ANDAMENTO,
    CONCLUIDA,
    CANCELADA
}
