package com.nexus.nexus_api.dto;

import java.util.List;

/**
 * Um grupo já revisado pelo usuário na prévia, pronto para salvar.
 *
 * Se {@code subjectId} vier preenchido, o grupo é salvo na matéria já existente com
 * esse ID (validado que pertence ao plano). Se vier null, {@code subjectNome} é
 * obrigatório e o backend faz find-or-create por nome normalizado — reaproveita uma
 * matéria existente com nome equivalente, ou cria uma nova.
 *
 * Mesma lógica para {@code topicId}/{@code topicNome} — se ambos vierem nulos/vazios,
 * o backend usa "Geral" (find-or-create) dentro da matéria resolvida.
 */
public record QuestionGroupImportRequest(
        Long subjectId,
        String subjectNome,
        Long topicId,
        String topicNome,
        List<QuestionRequest> questoes
) {
}
