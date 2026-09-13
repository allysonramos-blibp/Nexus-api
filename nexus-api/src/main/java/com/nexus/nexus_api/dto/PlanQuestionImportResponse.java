package com.nexus.nexus_api.dto;

import java.util.List;

public record PlanQuestionImportResponse(
        int totalSalvo,
        List<SubjectImportSummary> resumo
) {
}
