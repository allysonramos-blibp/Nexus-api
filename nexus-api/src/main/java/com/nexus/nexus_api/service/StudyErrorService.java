package com.nexus.nexus_api.service;

import com.nexus.nexus_api.dto.StudyErrorRequest;
import com.nexus.nexus_api.exception.ResourceNotFoundException;
import com.nexus.nexus_api.model.Answer;
import com.nexus.nexus_api.model.Question;
import com.nexus.nexus_api.model.StudyError;
import com.nexus.nexus_api.model.User;
import com.nexus.nexus_api.repository.AnswerRepository;
import com.nexus.nexus_api.repository.StudyErrorRepository;
import com.nexus.nexus_api.repository.UserRepository;
import com.nexus.nexus_api.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyErrorService {

    /** Intervalo padrão até a próxima revisão quando o cliente não informa uma data específica. */
    private static final int INTERVALO_PADRAO_DIAS = 3;

    private final StudyErrorRepository studyErrorRepository;
    private final UserRepository userRepository;
    private final QuestionService questionService;
    private final AnswerRepository answerRepository;

    /**
     * Cria ou atualiza a entrada do caderno de erros para a questão (respeita a constraint única
     * user+question — errar a mesma questão de novo atualiza a entrada existente em vez de duplicar).
     */
    public StudyError registerOrUpdate(StudyErrorRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Question question = questionService.findByIdOwnedByCurrentUser(request.questionId());

        Answer answer = null;
        if (request.answerId() != null) {
            answer = answerRepository.findById(request.answerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Resposta não encontrada com ID: " + request.answerId()));
            SecurityUtils.assertOwnership(answer.getUser().getId());
        }

        LocalDate proximaRevisao = request.proximaRevisao() != null
                ? request.proximaRevisao()
                : LocalDate.now().plusDays(INTERVALO_PADRAO_DIAS);

        StudyError studyError = studyErrorRepository.findByUserIdAndQuestionId(currentUserId, question.getId())
                .orElseGet(() -> {
                    User user = userRepository.findById(currentUserId)
                            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + currentUserId));
                    return StudyError.builder()
                            .user(user)
                            .question(question)
                            .criadoEm(LocalDateTime.now())
                            .build();
                });

        studyError.setMotivo(request.motivo());
        studyError.setAnotacao(request.observacao());
        studyError.setProximaRevisao(proximaRevisao);
        studyError.setResolvido(false);
        studyError.setAnswer(answer);

        return studyErrorRepository.save(studyError);
    }

    public List<StudyError> listMine() {
        return studyErrorRepository.findByUserId(SecurityUtils.getCurrentUserId());
    }

    public List<StudyError> listPendentesRevisao() {
        return studyErrorRepository.findByUserIdAndResolvidoFalseAndProximaRevisaoLessThanEqual(
                SecurityUtils.getCurrentUserId(), LocalDate.now());
    }

    public StudyError findByIdOwnedByCurrentUser(Long id) {
        StudyError studyError = studyErrorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de erro não encontrado com ID: " + id));
        SecurityUtils.assertOwnership(studyError.getUser().getId());
        return studyError;
    }

    public StudyError markResolved(Long id) {
        StudyError studyError = findByIdOwnedByCurrentUser(id);
        studyError.setResolvido(true);
        return studyErrorRepository.save(studyError);
    }

    public void delete(Long id) {
        StudyError studyError = findByIdOwnedByCurrentUser(id);
        studyErrorRepository.delete(studyError);
    }
}
