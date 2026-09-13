package com.nexus.nexus_api.controller;

import com.nexus.nexus_api.dto.PlanPdfExtractionResponse;
import com.nexus.nexus_api.dto.PlanQuestionImportRequest;
import com.nexus.nexus_api.dto.PlanQuestionImportResponse;
import com.nexus.nexus_api.model.StudyPlan;
import com.nexus.nexus_api.service.PlanQuestionGroupingService;
import com.nexus.nexus_api.service.PlanQuestionImportService;
import com.nexus.nexus_api.service.StudyPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Importação de PDF no nível do PLANO — não exige que o usuário escolha uma matéria/assunto
 * antes do upload. A IA classifica cada questão por matéria+assunto durante a extração; o
 * find-or-create de Subject/Topic só acontece na confirmação (POST .../import), depois que o
 * usuário revisou a prévia.
 *
 * O fluxo antigo (POST /api/topics/{topicId}/questions/bulk, em QuestionController) continua
 * existindo, para quem já está dentro de um assunto específico e quer importar um PDF de uma
 * matéria só.
 */
@RestController
@RequestMapping("/api/study-plans/{planId}/questions")
@RequiredArgsConstructor
public class PlanQuestionController {

    private final StudyPlanService studyPlanService;
    private final PlanQuestionGroupingService groupingService;
    private final PlanQuestionImportService importService;

    @PostMapping(value = "/extract-pdf", consumes = "multipart/form-data")
    public PlanPdfExtractionResponse extractFromPdf(@PathVariable Long planId, @RequestParam("file") MultipartFile file) {
        // findByIdOwnedByCurrentUser já barra com 403 se o plano não for do usuário autenticado.
        StudyPlan plan = studyPlanService.findByIdOwnedByCurrentUser(planId);
        return groupingService.extractAndGroup(plan, file);
    }

    @PostMapping("/import")
    public PlanQuestionImportResponse importQuestions(@PathVariable Long planId,
                                                       @Valid @RequestBody PlanQuestionImportRequest request) {
        return importService.importGroups(planId, request);
    }
}
