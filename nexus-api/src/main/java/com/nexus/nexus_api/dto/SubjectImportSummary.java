package com.nexus.nexus_api.dto;

import java.util.List;

public record SubjectImportSummary(
        Long subjectId,
        String subjectNome,
        List<TopicImportSummary> topicos
) {
}
