package com.nexus.nexus_api.service;

import com.nexus.nexus_api.dto.QuestionRequest;
import com.nexus.nexus_api.exception.ResourceNotFoundException;
import com.nexus.nexus_api.model.Question;
import com.nexus.nexus_api.model.Topic;
import com.nexus.nexus_api.repository.QuestionRepository;
import com.nexus.nexus_api.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final TopicService topicService;

    public Question create(Long topicId, QuestionRequest request) {
        Topic topic = topicService.findByIdOwnedByCurrentUser(topicId);

        Question question = Question.builder()
                .numero(request.numero())
                .enunciado(request.enunciado())
                .alternativas(new ArrayList<>(request.alternativas()))
                .dificuldade(request.dificuldade())
                .gabarito(request.gabarito())
                .explicacao(request.explicacao())
                .banca(request.banca())
                .ano(request.ano())
                .topic(topic)
                .build();

        return questionRepository.save(question);
    }

    public List<Question> listByTopic(Long topicId) {
        topicService.findByIdOwnedByCurrentUser(topicId);
        return questionRepository.findByTopicId(topicId);
    }

    public Question findByIdOwnedByCurrentUser(Long id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Questão não encontrada com ID: " + id));
        SecurityUtils.assertOwnership(question.getTopic().getSubject().getStudyPlan().getUser().getId());
        return question;
    }

    public Question update(Long id, QuestionRequest request) {
        Question question = findByIdOwnedByCurrentUser(id);

        question.setNumero(request.numero());
        question.setEnunciado(request.enunciado());
        question.setAlternativas(new ArrayList<>(request.alternativas()));
        question.setDificuldade(request.dificuldade());
        question.setGabarito(request.gabarito());
        question.setExplicacao(request.explicacao());
        question.setBanca(request.banca());
        question.setAno(request.ano());

        return questionRepository.save(question);
    }

    public void delete(Long id) {
        Question question = findByIdOwnedByCurrentUser(id);
        questionRepository.delete(question);
    }
}
