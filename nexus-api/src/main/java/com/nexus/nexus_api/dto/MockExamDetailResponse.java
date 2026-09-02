package com.nexus.nexus_api.dto;

import java.util.List;

/** Detalhe do simulado + suas questões (sem gabarito enquanto não finalizado). */
public record MockExamDetailResponse(
        MockExamResponse exam,
        List<QuestionResponse> questoes
) {}
