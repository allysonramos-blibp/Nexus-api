package com.nexus.nexus_api.service;

import com.nexus.nexus_api.dto.AnswerRequest;
import com.nexus.nexus_api.exception.ResourceNotFoundException;
import com.nexus.nexus_api.model.Answer;
import com.nexus.nexus_api.model.MockExam;
import com.nexus.nexus_api.model.MockExamStatus;
import com.nexus.nexus_api.model.Question;
import com.nexus.nexus_api.model.User;
import com.nexus.nexus_api.repository.AnswerRepository;
import com.nexus.nexus_api.repository.MockExamQuestionRepository;
import com.nexus.nexus_api.repository.MockExamRepository;
import com.nexus.nexus_api.repository.UserRepository;
import com.nexus.nexus_api.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnswerService {

    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final QuestionService questionService;
    private final MockExamRepository mockExamRepository;
    private final MockExamQuestionRepository mockExamQuestionRepository;

    public Answer register(AnswerRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        // Garante que a questão pertence (via cadeia Topic->Subject->StudyPlan) ao usuário autenticado.
        Question question = questionService.findByIdOwnedByCurrentUser(request.questionId());

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + currentUserId));

        MockExam mockExam = null;
        if (request.mockExamId() != null) {
            mockExam = mockExamRepository.findById(request.mockExamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Simulado não encontrado com ID: " + request.mockExamId()));

            SecurityUtils.assertOwnership(mockExam.getUser().getId());

            if (mockExam.getStatus() != MockExamStatus.EM_ANDAMENTO) {
                throw new IllegalStateException("O simulado precisa estar em andamento para receber respostas.");
            }

            boolean pertenceAoSimulado = mockExamQuestionRepository.findByMockExamIdOrderByOrdemAsc(mockExam.getId())
                    .stream()
                    .anyMatch(meq -> meq.getQuestion().getId().equals(question.getId()));

            if (!pertenceAoSimulado) {
                throw new IllegalArgumentException("Esta questão não faz parte do simulado informado.");
            }
        }

        boolean correta = question.getGabarito() != null
                && question.getGabarito().trim().equalsIgnoreCase(request.respostaEscolhida().trim());

        long tentativasAnteriores = answerRepository.countByUserIdAndQuestionId(currentUserId, question.getId());

        Answer answer = Answer.builder()
                .respostaEscolhida(request.respostaEscolhida())
                .correta(correta)
                .tempoSegundos(request.tempoSegundos())
                .numeroTentativa((int) tentativasAnteriores + 1)
                .respondidoEm(LocalDateTime.now())
                .user(user)
                .question(question)
                .mockExam(mockExam)
                .build();

        return answerRepository.save(answer);
    }

    public List<Answer> listMine() {
        return answerRepository.findByUserId(SecurityUtils.getCurrentUserId());
    }

    public Answer findByIdOwnedByCurrentUser(Long id) {
        Answer answer = answerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resposta não encontrada com ID: " + id));
        SecurityUtils.assertOwnership(answer.getUser().getId());
        return answer;
    }
}
