package com.nexus.nexus_api.dto;

import java.util.List;

public record PdfExtractionResponse(
        List<QuestionRequest> questoes,
        int total
) {
    public static PdfExtractionResponse of(List<QuestionRequest> questoes) {
        return new PdfExtractionResponse(questoes, questoes.size());
    }
}
