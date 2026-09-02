package com.nexus.nexus_api.dto;

public record PendingReviewResponse(
        long totalPendentes,
        java.util.List<StudyErrorResponse> itens
) {}
