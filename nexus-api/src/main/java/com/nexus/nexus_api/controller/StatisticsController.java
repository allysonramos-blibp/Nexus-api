package com.nexus.nexus_api.controller;

import com.nexus.nexus_api.dto.*;
import com.nexus.nexus_api.service.StatisticsService;
import com.nexus.nexus_api.service.StudyErrorService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/study-stats")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final StudyErrorService studyErrorService;

    @GetMapping("/geral")
    public OverallStatsResponse geral() {
        return statisticsService.overall();
    }

    @GetMapping("/por-materia")
    public List<SubjectPerformanceDto> porMateria() {
        return statisticsService.porMateria();
    }

    @GetMapping("/por-assunto")
    public List<TopicPerformanceDto> porAssunto() {
        return statisticsService.porAssunto();
    }

    @GetMapping("/por-periodo")
    public OverallStatsResponse porPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim
    ) {
        return statisticsService.porPeriodo(inicio, fim);
    }

    @GetMapping("/pendentes-revisao")
    public PendingReviewResponse pendentesRevisao() {
        List<StudyErrorResponse> itens = studyErrorService.listPendentesRevisao().stream()
                .map(StudyErrorResponse::from)
                .toList();
        return new PendingReviewResponse(itens.size(), itens);
    }
}
