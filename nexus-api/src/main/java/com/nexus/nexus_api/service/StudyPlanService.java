package com.nexus.nexus_api.service;

import com.nexus.nexus_api.dto.StudyPlanRequest;
import com.nexus.nexus_api.dto.StudyPlanResponse;
import com.nexus.nexus_api.exception.ResourceNotFoundException;
import com.nexus.nexus_api.model.StudyPlan;
import com.nexus.nexus_api.model.StudyPlanStatus;
import com.nexus.nexus_api.model.User;
import com.nexus.nexus_api.repository.AnswerRepository;
import com.nexus.nexus_api.repository.QuestionRepository;
import com.nexus.nexus_api.repository.StudyPlanRepository;
import com.nexus.nexus_api.repository.SubjectRepository;
import com.nexus.nexus_api.repository.TopicRepository;
import com.nexus.nexus_api.repository.UserRepository;
import com.nexus.nexus_api.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyPlanService {

    private final StudyPlanRepository studyPlanRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;

    public StudyPlan create(StudyPlanRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + currentUserId));

        StudyPlan plan = StudyPlan.builder()
                .titulo(request.nome())
                .objetivo(request.objetivo())
                .descricao(request.descricao())
                .dataInicio(request.dataInicio())
                .dataFim(request.dataAlvo())
                .horasDisponiveis(request.horasDisponiveis())
                .status(request.status() != null ? request.status() : StudyPlanStatus.PLANEJADO)
                .ativo(true)
                .user(user)
                .build();

        return studyPlanRepository.save(plan);
    }

    public StudyPlan findByIdOwnedByCurrentUser(Long id) {
        StudyPlan plan = studyPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plano de estudo não encontrado com ID: " + id));
        SecurityUtils.assertOwnership(plan.getUser().getId());
        return plan;
    }

    public List<StudyPlan> listMine() {
        return studyPlanRepository.findByUserId(SecurityUtils.getCurrentUserId());
    }

    public StudyPlan update(Long id, StudyPlanRequest request) {
        StudyPlan plan = findByIdOwnedByCurrentUser(id);

        plan.setTitulo(request.nome());
        plan.setObjetivo(request.objetivo());
        plan.setDescricao(request.descricao());
        plan.setDataInicio(request.dataInicio());
        plan.setDataFim(request.dataAlvo());
        plan.setHorasDisponiveis(request.horasDisponiveis());
        if (request.status() != null) {
            plan.setStatus(request.status());
        }

        return studyPlanRepository.save(plan);
    }

    public void delete(Long id) {
        StudyPlan plan = findByIdOwnedByCurrentUser(id);
        studyPlanRepository.delete(plan);
    }

    /** Monta o DTO de resposta com as contagens e o progresso calculado sob demanda (nunca persistido). */
    public StudyPlanResponse toResponse(StudyPlan plan) {
        long totalMaterias = subjectRepository.countByStudyPlanId(plan.getId());
        long totalAssuntos = topicRepository.countBySubjectStudyPlanId(plan.getId());
        long totalQuestoes = questionRepository.countByTopicSubjectStudyPlanId(plan.getId());
        long acertosDistintos = answerRepository.countDistinctCorrectQuestionsByPlan(plan.getUser().getId(), plan.getId());

        double progresso = totalQuestoes == 0 ? 0.0 : (acertosDistintos * 100.0) / totalQuestoes;

        return StudyPlanResponse.from(plan, (int) totalMaterias, (int) totalAssuntos, Math.round(progresso * 100.0) / 100.0);
    }
}
