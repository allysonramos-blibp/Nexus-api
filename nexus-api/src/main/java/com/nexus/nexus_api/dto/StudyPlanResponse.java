package com.nexus.nexus_api.dto;

import com.nexus.nexus_api.model.StudyPlan;
import com.nexus.nexus_api.model.StudyPlanStatus;

import java.time.LocalDate;

public record StudyPlanResponse(
        Long id,
        String nome,
        String objetivo,
        String descricao,
        LocalDate dataInicio,
        LocalDate dataAlvo,
        Integer horasDisponiveis,
        StudyPlanStatus status,
        Boolean ativo,
        int totalMaterias,
        int totalAssuntos,
        double progresso,
        Long userId
) {
    public static StudyPlanResponse from(StudyPlan plan, int totalMaterias, int totalAssuntos, double progresso) {
        return new StudyPlanResponse(
                plan.getId(),
                plan.getTitulo(),
                plan.getObjetivo(),
                plan.getDescricao(),
                plan.getDataInicio(),
                plan.getDataFim(),
                plan.getHorasDisponiveis(),
                plan.getStatus(),
                plan.getAtivo(),
                totalMaterias,
                totalAssuntos,
                progresso,
                plan.getUser().getId()
        );
    }
}
