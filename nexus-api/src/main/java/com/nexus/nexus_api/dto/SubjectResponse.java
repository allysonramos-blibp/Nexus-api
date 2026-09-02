package com.nexus.nexus_api.dto;

import com.nexus.nexus_api.model.Subject;

public record SubjectResponse(
        Long id,
        String nome,
        Integer pesoNoEdital,
        Long studyPlanId
) {
    public static SubjectResponse from(Subject subject) {
        return new SubjectResponse(
                subject.getId(),
                subject.getNome(),
                subject.getPesoNoEdital(),
                subject.getStudyPlan().getId()
        );
    }
}
