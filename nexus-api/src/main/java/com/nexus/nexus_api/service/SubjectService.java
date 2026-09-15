package com.nexus.nexus_api.service;

import com.nexus.nexus_api.dto.SubjectRequest;
import com.nexus.nexus_api.exception.ResourceNotFoundException;
import com.nexus.nexus_api.model.StudyPlan;
import com.nexus.nexus_api.model.Subject;
import com.nexus.nexus_api.repository.SubjectRepository;
import com.nexus.nexus_api.util.NameNormalizer;
import com.nexus.nexus_api.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.nexus.nexus_api.model.Question;
import com.nexus.nexus_api.model.StudyError;
import com.nexus.nexus_api.model.Topic;
import com.nexus.nexus_api.repository.AnswerRepository;
import com.nexus.nexus_api.repository.MockExamQuestionRepository;
import com.nexus.nexus_api.repository.QuestionRepository;
import com.nexus.nexus_api.repository.ReviewRepository;
import com.nexus.nexus_api.repository.StudyErrorRepository;
import com.nexus.nexus_api.repository.TopicRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final StudyPlanService studyPlanService;
    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final MockExamQuestionRepository mockExamQuestionRepository;
    private final StudyErrorRepository studyErrorRepository;
    private final ReviewRepository reviewRepository;

    public Subject create(Long studyPlanId, SubjectRequest request) {
        // findByIdOwnedByCurrentUser já barra com 403 se o plano não for do usuário autenticado.
        StudyPlan plan = studyPlanService.findByIdOwnedByCurrentUser(studyPlanId);

        Subject subject = Subject.builder()
                .nome(request.nome())
                .pesoNoEdital(request.pesoNoEdital())
                .studyPlan(plan)
                .build();

        return subjectRepository.save(subject);
    }

    public List<Subject> listByStudyPlan(Long studyPlanId) {
        studyPlanService.findByIdOwnedByCurrentUser(studyPlanId);
        return subjectRepository.findByStudyPlanId(studyPlanId);
    }

    /**
     * Busca uma matéria do plano cujo nome normalizado (trim, espaços duplicados,
     * case-insensitive, sem diacríticos — ver {@link com.nexus.nexus_api.util.NameNormalizer})
     * bata com {@code nomeSugerido}; se não achar, cria uma nova com esse nome (o texto
     * exibido é sempre o texto original passado, nunca a versão normalizada). Usado pelo
     * fluxo de importação de PDF por plano — nunca faz correspondência semântica (ex.:
     * "Direito Constitucional" e "Direito Administrativo" nunca são tratados como iguais).
     */
    public Subject findOrCreateByNome(StudyPlan plan, String nomeSugerido) {
        String nome = NameNormalizer.isBlank(nomeSugerido) ? "Sem matéria identificada" : nomeSugerido.trim();
        String chave = NameNormalizer.normalize(nome);

        for (Subject existente : subjectRepository.findByStudyPlanId(plan.getId())) {
            if (NameNormalizer.normalize(existente.getNome()).equals(chave)) {
                return existente;
            }
        }

        Subject novo = Subject.builder()
                .nome(nome)
                .studyPlan(plan)
                .build();
        return subjectRepository.save(novo);
    }

    public Subject findByIdOwnedByCurrentUser(Long id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Matéria não encontrada com ID: " + id));
        SecurityUtils.assertOwnership(subject.getStudyPlan().getUser().getId());
        return subject;
    }

    public Subject update(Long id, SubjectRequest request) {
        Subject subject = findByIdOwnedByCurrentUser(id);
        subject.setNome(request.nome());
        subject.setPesoNoEdital(request.pesoNoEdital());
        return subjectRepository.save(subject);
    }

    @Transactional
    public void delete(Long id) {
        Subject subject = findByIdOwnedByCurrentUser(id);

        List<Topic> topics = topicRepository.findBySubjectId(id);
        List<Long> questionIds = new ArrayList<>();
        List<Long> topicIds = new ArrayList<>();

        for (Topic t : topics) {
            topicIds.add(t.getId());
            List<Question> questions = questionRepository.findByTopicId(t.getId());
            for (Question q : questions) {
                questionIds.add(q.getId());
            }
        }

        if (!questionIds.isEmpty()) {
            answerRepository.deleteByQuestionIdIn(questionIds);
            mockExamQuestionRepository.deleteByQuestionIdIn(questionIds);

            List<StudyError> errors = studyErrorRepository.findByQuestionIdIn(questionIds);
            if (!errors.isEmpty()) {
                List<Long> errorIds = errors.stream().map(StudyError::getId).toList();
                reviewRepository.deleteByStudyErrorIdIn(errorIds);
            }
            studyErrorRepository.deleteByQuestionIdIn(questionIds);
        }

        if (!topicIds.isEmpty()) {
            reviewRepository.deleteByTopicIdIn(topicIds);
        }

        for (Topic t : topics) {
            List<Question> questions = questionRepository.findByTopicId(t.getId());
            questionRepository.deleteAll(questions);
            topicRepository.delete(t);
        }

        subjectRepository.delete(subject);
    }
}
