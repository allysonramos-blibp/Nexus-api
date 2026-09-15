package com.nexus.nexus_api.service;

import com.nexus.nexus_api.dto.TopicRequest;
import com.nexus.nexus_api.exception.ResourceNotFoundException;
import com.nexus.nexus_api.model.Subject;
import com.nexus.nexus_api.model.Topic;
import com.nexus.nexus_api.repository.TopicRepository;
import com.nexus.nexus_api.util.NameNormalizer;
import com.nexus.nexus_api.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.nexus.nexus_api.model.Question;
import com.nexus.nexus_api.model.StudyError;
import com.nexus.nexus_api.repository.AnswerRepository;
import com.nexus.nexus_api.repository.MockExamQuestionRepository;
import com.nexus.nexus_api.repository.QuestionRepository;
import com.nexus.nexus_api.repository.ReviewRepository;
import com.nexus.nexus_api.repository.StudyErrorRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;
    private final SubjectService subjectService;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final MockExamQuestionRepository mockExamQuestionRepository;
    private final StudyErrorRepository studyErrorRepository;
    private final ReviewRepository reviewRepository;

    public Topic create(Long subjectId, TopicRequest request) {
        Subject subject = subjectService.findByIdOwnedByCurrentUser(subjectId);

        Topic topic = Topic.builder()
                .nome(request.nome())
                .ordem(request.ordem())
                .subject(subject)
                .build();

        return topicRepository.save(topic);
    }

    public List<Topic> listBySubject(Long subjectId) {
        subjectService.findByIdOwnedByCurrentUser(subjectId);
        return topicRepository.findBySubjectId(subjectId);
    }

    /**
     * Mesma lógica de {@link SubjectService#findOrCreateByNome}, no nível de assunto —
     * busca por nome normalizado dentro da matéria já resolvida, cria só se não achar.
     * Quando {@code nomeSugerido} vier ausente/inválido (a IA não conseguiu identificar
     * o assunto), usa o fallback fixo "Geral".
     */
    public Topic findOrCreateByNome(Subject subject, String nomeSugerido) {
        String nome = NameNormalizer.isBlank(nomeSugerido) ? "Geral" : nomeSugerido.trim();
        String chave = NameNormalizer.normalize(nome);

        for (Topic existente : topicRepository.findBySubjectId(subject.getId())) {
            if (NameNormalizer.normalize(existente.getNome()).equals(chave)) {
                return existente;
            }
        }

        Topic novo = Topic.builder()
                .nome(nome)
                .subject(subject)
                .build();
        return topicRepository.save(novo);
    }

    public Topic findByIdOwnedByCurrentUser(Long id) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assunto não encontrado com ID: " + id));
        SecurityUtils.assertOwnership(topic.getSubject().getStudyPlan().getUser().getId());
        return topic;
    }

    public Topic update(Long id, TopicRequest request) {
        Topic topic = findByIdOwnedByCurrentUser(id);
        topic.setNome(request.nome());
        topic.setOrdem(request.ordem());
        return topicRepository.save(topic);
    }

    @Transactional
    public void delete(Long id) {
        Topic topic = findByIdOwnedByCurrentUser(id);

        List<Question> questions = questionRepository.findByTopicId(id);
        List<Long> questionIds = questions.stream().map(Question::getId).toList();

        if (!questionIds.isEmpty()) {
            List<StudyError> errors = studyErrorRepository.findByQuestionIdIn(questionIds);
            if (!errors.isEmpty()) {
                List<Long> errorIds = errors.stream().map(StudyError::getId).toList();
                reviewRepository.deleteByStudyErrorIdIn(errorIds);
            }
            studyErrorRepository.deleteByQuestionIdIn(questionIds);

            answerRepository.deleteByQuestionIdIn(questionIds);
            mockExamQuestionRepository.deleteByQuestionIdIn(questionIds);
        }

        reviewRepository.deleteByTopicId(id);
        questionRepository.deleteAll(questions);
        topicRepository.delete(topic);
    }
}
