package com.nexus.nexus_api.service;

import com.nexus.nexus_api.dto.PlanQuestionImportRequest;
import com.nexus.nexus_api.dto.PlanQuestionImportResponse;
import com.nexus.nexus_api.dto.QuestionGroupImportRequest;
import com.nexus.nexus_api.dto.QuestionRequest;
import com.nexus.nexus_api.dto.SubjectImportSummary;
import com.nexus.nexus_api.dto.TopicImportSummary;
import com.nexus.nexus_api.model.Question;
import com.nexus.nexus_api.model.StudyPlan;
import com.nexus.nexus_api.model.Subject;
import com.nexus.nexus_api.model.Topic;
import com.nexus.nexus_api.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Importação definitiva da prévia revisada (POST /api/study-plans/{planId}/questions/import).
 * Resolve cada grupo (find-or-create de Subject/Topic por nome normalizado, ou reaproveita um
 * subjectId/topicId já existente informado pelo frontend) e salva todas as questões numa única
 * transação — se qualquer grupo falhar a validação, nada é salvo.
 */
@Service
@RequiredArgsConstructor
public class PlanQuestionImportService {

    private final StudyPlanService studyPlanService;
    private final SubjectService subjectService;
    private final TopicService topicService;
    private final QuestionService questionService;
    private final QuestionRepository questionRepository;

    @Transactional
    public PlanQuestionImportResponse importGroups(Long planId, PlanQuestionImportRequest request) {
        StudyPlan plan = studyPlanService.findByIdOwnedByCurrentUser(planId);

        if (request.grupos() == null || request.grupos().isEmpty()) {
            throw new IllegalArgumentException("Nenhum grupo de questões enviado para importação.");
        }

        // LinkedHashMap por subjectId preserva a ordem de chegada dos grupos no resumo final.
        Map<Long, SubjectAccumulator> resumo = new LinkedHashMap<>();
        int totalSalvo = 0;

        for (QuestionGroupImportRequest grupo : request.grupos()) {
            List<QuestionRequest> questoes = grupo.questoes();
            if (questoes == null || questoes.isEmpty()) {
                // Grupo vazio (ex.: usuário removeu todas as questões dele na prévia) — ignora,
                // não é erro.
                continue;
            }

            for (QuestionRequest q : questoes) {
                if (q.enunciado() == null || q.enunciado().isBlank()) {
                    throw new IllegalArgumentException(
                            "Uma questão sem enunciado foi enviada para importação — revise a prévia antes de confirmar.");
                }
                if (q.alternativas() == null || q.alternativas().size() < 2) {
                    throw new IllegalArgumentException(
                            "Uma questão com menos de 2 alternativas foi enviada para importação — revise a prévia antes de confirmar.");
                }
            }

            Subject subject = resolveSubject(plan, grupo);
            Topic topic = resolveTopic(subject, grupo);

            List<Question> paraSalvar = questoes.stream()
                    .map(q -> questionService.build(topic, q))
                    .toList();
            List<Question> salvas = questionRepository.saveAll(paraSalvar);
            totalSalvo += salvas.size();

            resumo.computeIfAbsent(subject.getId(), id -> new SubjectAccumulator(subject.getId(), subject.getNome()))
                    .registrarTopico(topic.getId(), topic.getNome(), salvas.size());
        }

        List<SubjectImportSummary> resumoFinal = resumo.values().stream()
                .map(SubjectAccumulator::toSummary)
                .toList();

        return new PlanQuestionImportResponse(totalSalvo, resumoFinal);
    }

    private Subject resolveSubject(StudyPlan plan, QuestionGroupImportRequest grupo) {
        if (grupo.subjectId() != null) {
            Subject subject = subjectService.findByIdOwnedByCurrentUser(grupo.subjectId());
            if (!subject.getStudyPlan().getId().equals(plan.getId())) {
                throw new AccessDeniedException("A matéria informada não pertence a este plano.");
            }
            return subject;
        }
        return subjectService.findOrCreateByNome(plan, grupo.subjectNome());
    }

    private Topic resolveTopic(Subject subject, QuestionGroupImportRequest grupo) {
        if (grupo.topicId() != null) {
            Topic topic = topicService.findByIdOwnedByCurrentUser(grupo.topicId());
            if (!topic.getSubject().getId().equals(subject.getId())) {
                throw new AccessDeniedException("O assunto informado não pertence à matéria resolvida para este grupo.");
            }
            return topic;
        }
        return topicService.findOrCreateByNome(subject, grupo.topicNome());
    }

    /** Acumula, por Subject, quantas questões foram salvas em cada Topic — só para o resumo final. */
    private static final class SubjectAccumulator {
        private final Long subjectId;
        private final String subjectNome;
        private final Map<Long, TopicAccumulator> topicos = new LinkedHashMap<>();

        SubjectAccumulator(Long subjectId, String subjectNome) {
            this.subjectId = subjectId;
            this.subjectNome = subjectNome;
        }

        void registrarTopico(Long topicId, String topicNome, int quantidade) {
            topicos.computeIfAbsent(topicId, id -> new TopicAccumulator(topicId, topicNome))
                    .somar(quantidade);
        }

        SubjectImportSummary toSummary() {
            List<TopicImportSummary> resumoTopicos = topicos.values().stream()
                    .map(t -> new TopicImportSummary(t.topicId, t.topicNome, t.quantidade))
                    .toList();
            return new SubjectImportSummary(subjectId, subjectNome, resumoTopicos);
        }
    }

    private static final class TopicAccumulator {
        private final Long topicId;
        private final String topicNome;
        private int quantidade = 0;

        TopicAccumulator(Long topicId, String topicNome) {
            this.topicId = topicId;
            this.topicNome = topicNome;
        }

        void somar(int n) {
            quantidade += n;
        }
    }
}
