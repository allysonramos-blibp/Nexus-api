package com.nexus.nexus_api.dto;

import java.util.List;

public record ExistingSubjectSummary(
        Long id,
        String nome,
        List<ExistingTopicSummary> topics
) {
}
