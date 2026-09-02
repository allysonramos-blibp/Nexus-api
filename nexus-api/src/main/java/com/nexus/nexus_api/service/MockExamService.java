package com.nexus.nexus_api.service;

import com.nexus.nexus_api.dto.MockExamRequest;
import com.nexus.nexus_api.exception.ResourceNotFoundException;
import com.nexus.nexus_api.model.*;
import com.nexus.nexus_api.repository.*;
import com.nexus.nexus_api.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MockExamService {

    private final MockExamRepository mockExamRepository;
    private final MockExamQuestionRepository mockExamQuestionRepository;
    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;

    @Transactional
    public MockExam create(MockExamRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + currentUserId));

        // Só aceita matérias que realmente pertencem (via plano) ao usuário autenticado.
        List<Subject> subjects = subjectRepository.findByIdInAndStudyPlanUserId(request.subjectIds(), currentUserId);
        if (subjects.size() != request.subjectIds().size()) {
            throw new IllegalArgumentException("Uma ou mais matérias informadas não existem ou não pertencem a você.");
        }

        StudyPlan studyPlan = null;
        if (request.studyPlanId() != null) {
            studyPlan = studyPlanRepository.findById(request.studyPlanId())
                    .orElseThrow(() -> new ResourceNotFoundException("Plano de estudo não encontrado com ID: " + request.studyPlanId()));
            SecurityUtils.assertOwnership(studyPlan.getUser().getId());
        }

        List<Long> subjectIds = subjects.stream().map(Subject::getId).toList();
        List<Question> questoesSorteadas = questionRepository.findRandomBySubjectIds(subjectIds, request.quantidadeQuestoes());

        if (questoesSorteadas.isEmpty()) {
            throw new IllegalArgumentException("Nenhuma questão cadastrada nas matérias selecionadas para montar o simulado.");
        }

        MockExam exam = MockExam.builder()
                .titulo(request.titulo())
                .dataRealizacao(LocalDate.now())
                .status(MockExamStatus.CRIADO)
                .duracaoMinutos(request.duracaoMinutos())
                .totalQuestoes(questoesSorteadas.size())
                .studyPlan(studyPlan)
                .subjects(subjects)
                .user(user)
                .build();

        MockExam savedExam = mockExamRepository.save(exam);

        int ordem = 1;
        for (Question question : questoesSorteadas) {
            MockExamQuestion meq = MockExamQuestion.builder()
                    .mockExam(savedExam)
                    .question(question)
                    .ordem(ordem++)
                    .build();
            mockExamQuestionRepository.save(meq);
        }

        return savedExam;
    }

    public MockExam findByIdOwnedByCurrentUser(Long id) {
        MockExam exam = mockExamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Simulado não encontrado com ID: " + id));
        SecurityUtils.assertOwnership(exam.getUser().getId());
        return exam;
    }

    public List<MockExam> listMine() {
        return mockExamRepository.findByUserId(SecurityUtils.getCurrentUserId());
    }

    /** Questões do simulado, na ordem sorteada. */
    public List<Question> listQuestions(Long mockExamId) {
        findByIdOwnedByCurrentUser(mockExamId);
        return mockExamQuestionRepository.findByMockExamIdOrderByOrdemAsc(mockExamId).stream()
                .map(MockExamQuestion::getQuestion)
                .toList();
    }

    public MockExam iniciar(Long id) {
        MockExam exam = findByIdOwnedByCurrentUser(id);

        if (exam.getStatus() != MockExamStatus.CRIADO) {
            throw new IllegalStateException("Este simulado já foi iniciado ou finalizado.");
        }

        exam.setStatus(MockExamStatus.EM_ANDAMENTO);
        exam.setIniciadoEm(LocalDateTime.now());
        return mockExamRepository.save(exam);
    }

    public MockExam finalizar(Long id) {
        MockExam exam = findByIdOwnedByCurrentUser(id);

        if (exam.getStatus() != MockExamStatus.EM_ANDAMENTO) {
            throw new IllegalStateException("O simulado precisa estar em andamento para ser finalizado.");
        }

        long acertos = answerRepository.findByMockExamId(id).stream()
                .filter(Answer::getCorreta)
                .count();

        exam.setAcertos((int) acertos);
        exam.setNotaObtida(BigDecimal.valueOf(acertos));
        exam.setStatus(MockExamStatus.FINALIZADO);
        exam.setFinalizadoEm(LocalDateTime.now());

        return mockExamRepository.save(exam);
    }

    @Transactional
    public void delete(Long id) {
        MockExam exam = findByIdOwnedByCurrentUser(id);

        if (exam.getStatus() != MockExamStatus.CRIADO) {
            throw new IllegalStateException("Só é possível excluir simulados que ainda não foram iniciados.");
        }

        mockExamQuestionRepository.deleteByMockExamId(id);
        mockExamRepository.delete(exam);
    }
}
