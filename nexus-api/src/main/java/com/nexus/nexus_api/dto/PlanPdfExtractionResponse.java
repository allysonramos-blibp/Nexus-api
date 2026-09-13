package com.nexus.nexus_api.dto;

import java.util.List;

/**
 * Resposta de POST /api/study-plans/{planId}/questions/extract-pdf.
 *
 * IMPORTANTE (por que estes campos de integridade existem): não basta devolver
 * `totalExtraido` — se o PDF tinha 70 questões e só 50 vieram, o usuário PRECISA saber
 * disso antes de confirmar, não depois. `numerosAusentes` e `numerosDuplicados` vêm da
 * mesma heurística/dedupe já usados em {@code PdfQuestionExtractionService}, nunca são
 * garantias 100% exatas (a heurística de detecção pode ter falso positivo/negativo),
 * mas dão um sinal concreto — bem melhor do que confiar só em `possivelTotalNoPdf`.
 *
 * @param grupos               questões agrupadas por (matéria, assunto) sugeridos pela IA — ainda não salvo.
 * @param materiasExistentes   matérias (e assuntos) que o plano já tem, para o usuário escolher reaproveitar.
 * @param totalExtraido        soma de questões em todos os grupos (após dedupe).
 * @param possivelTotalNoPdf   estimativa heurística (regex) de quantas questões o PDF parece ter.
 * @param numerosAusentes      números de questão detectados pela heurística mas que NÃO vieram na extração final.
 * @param numerosDuplicados    números de questão que apareceram mais de uma vez antes do dedupe (esperado por
 *                             causa do overlap entre chunks; alto demais pode indicar chunk mal dividido).
 * @param chunksProcessados    em quantos pedaços o texto do PDF foi dividido.
 * @param chunksComFalha       quantos desses pedaços falharam na extração.
 */
public record PlanPdfExtractionResponse(
        List<QuestionGroup> grupos,
        List<ExistingSubjectSummary> materiasExistentes,
        int totalExtraido,
        int possivelTotalNoPdf,
        List<Integer> numerosAusentes,
        List<Integer> numerosDuplicados,
        int chunksProcessados,
        int chunksComFalha
) {
}
