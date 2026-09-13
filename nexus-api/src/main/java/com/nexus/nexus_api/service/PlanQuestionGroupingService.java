package com.nexus.nexus_api.service;

import com.nexus.nexus_api.dto.ExistingSubjectSummary;
import com.nexus.nexus_api.dto.ExistingTopicSummary;
import com.nexus.nexus_api.dto.PlanPdfExtractionResponse;
import com.nexus.nexus_api.dto.QuestionGroup;
import com.nexus.nexus_api.dto.QuestionRequest;
import com.nexus.nexus_api.model.StudyPlan;
import com.nexus.nexus_api.util.NameNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Roda a extração de PDF (via {@link PdfQuestionExtractionService}, uma única vez — nenhuma
 * lógica de extração é duplicada aqui) e agrupa o resultado por (matéria, assunto) sugeridos
 * pela IA, no contexto de um plano específico. Usado por
 * POST /api/study-plans/{planId}/questions/extract-pdf.
 */
@Service
@RequiredArgsConstructor
public class PlanQuestionGroupingService {

    private static final String SEM_MATERIA = "Sem matéria identificada";
    private static final String ASSUNTO_GERAL = "Geral";

    private final PdfQuestionExtractionService extractionService;
    private final SubjectService subjectService;
    private final TopicService topicService;

    public PlanPdfExtractionResponse extractAndGroup(StudyPlan plan, MultipartFile file) {
        PdfQuestionExtractionService.ExtractionResult result = extractionService.extractDetailed(file);

        // Chave de agrupamento = nomes normalizados (mesma regra do find-or-create), mas o nome
        // de EXIBIÇÃO na prévia é sempre a primeira grafia "bonita" encontrada pra aquela chave —
        // evita a prévia mostrar "português" minúsculo só porque foi assim que a IA escreveu numa
        // questão específica.
        Map<String, String> subjectDisplayByKey = new LinkedHashMap<>();
        Map<String, Map<String, String>> topicDisplayByKey = new LinkedHashMap<>();
        Map<String, Map<String, List<QuestionRequest>>> questoesPorChave = new LinkedHashMap<>();

        for (QuestionRequest q : result.questoes()) {
            String subjectDisplay = NameNormalizer.isBlank(q.disciplinaSugerida())
                    ? SEM_MATERIA : q.disciplinaSugerida().trim();
            String subjectKey = NameNormalizer.normalize(subjectDisplay);

            String topicDisplay = NameNormalizer.isBlank(q.assuntoSugerido())
                    ? ASSUNTO_GERAL : q.assuntoSugerido().trim();
            String topicKey = NameNormalizer.normalize(topicDisplay);

            subjectDisplayByKey.putIfAbsent(subjectKey, subjectDisplay);
            topicDisplayByKey.computeIfAbsent(subjectKey, k -> new LinkedHashMap<>()).putIfAbsent(topicKey, topicDisplay);
            questoesPorChave
                    .computeIfAbsent(subjectKey, k -> new LinkedHashMap<>())
                    .computeIfAbsent(topicKey, k -> new ArrayList<>())
                    .add(q);
        }

        List<QuestionGroup> grupos = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<QuestionRequest>>> subjectEntry : questoesPorChave.entrySet()) {
            String subjectKey = subjectEntry.getKey();
            String subjectDisplay = subjectDisplayByKey.get(subjectKey);
            for (Map.Entry<String, List<QuestionRequest>> topicEntry : subjectEntry.getValue().entrySet()) {
                String topicDisplay = topicDisplayByKey.get(subjectKey).get(topicEntry.getKey());
                grupos.add(new QuestionGroup(subjectDisplay, topicDisplay, topicEntry.getValue()));
            }
        }

        List<ExistingSubjectSummary> materiasExistentes = subjectService.listByStudyPlan(plan.getId()).stream()
                .map(subject -> new ExistingSubjectSummary(
                        subject.getId(),
                        subject.getNome(),
                        topicService.listBySubject(subject.getId()).stream()
                                .map(topic -> new ExistingTopicSummary(topic.getId(), topic.getNome()))
                                .toList()
                ))
                .toList();

        return new PlanPdfExtractionResponse(
                grupos,
                materiasExistentes,
                result.questoes().size(),
                result.possivelTotalNoPdf(),
                result.numerosAusentes(),
                result.numerosDuplicados(),
                result.chunksProcessados(),
                result.chunksComFalha()
        );
    }
}
