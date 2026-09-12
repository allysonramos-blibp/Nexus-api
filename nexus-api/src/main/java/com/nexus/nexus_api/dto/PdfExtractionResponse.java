package com.nexus.nexus_api.dto;

import java.util.List;

/**
 * @param total               quantidade de questões efetivamente extraídas (após dedupe) — o que
 *                            o frontend deve tratar como "processadas".
 * @param possivelTotalNoPdf  estimativa HEURÍSTICA (regex) de quantas questões o PDF parece ter.
 *                            Não é uma contagem confiável (pode errar pra mais ou pra menos) —
 *                            serve só pra alertar o usuário se `total` ficou muito abaixo disso.
 * @param chunksProcessados   em quantos pedaços o texto do PDF foi dividido para processar.
 * @param chunksComFalha      quantos desses pedaços falharam na extração (perdidos, não incluídos em `questoes`).
 */
public record PdfExtractionResponse(
        List<QuestionRequest> questoes,
        int total,
        Integer possivelTotalNoPdf,
        int chunksProcessados,
        int chunksComFalha
) {
    public static PdfExtractionResponse of(List<QuestionRequest> questoes, Integer possivelTotalNoPdf,
                                            int chunksProcessados, int chunksComFalha) {
        return new PdfExtractionResponse(questoes, questoes.size(), possivelTotalNoPdf, chunksProcessados, chunksComFalha);
    }
}
