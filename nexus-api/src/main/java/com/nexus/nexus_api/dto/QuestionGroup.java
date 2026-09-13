package com.nexus.nexus_api.dto;

import java.util.List;

/**
 * Um grupo de questões da PRÉVIA (ainda não salvo) — todas as questões que a IA
 * classificou sob a mesma matéria+assunto. `subjectNome`/`topicNome` aqui são nomes
 * de EXIBIÇÃO (o texto sugerido pela IA, ou o nome já existente quando o grupo bateu
 * com uma matéria/assunto que o plano já tinha) — o find-or-create de verdade só
 * acontece na hora de confirmar a importação (ver {@link QuestionGroupImportRequest}).
 */
public record QuestionGroup(
        String subjectNome,
        String topicNome,
        List<QuestionRequest> questoes
) {
}
