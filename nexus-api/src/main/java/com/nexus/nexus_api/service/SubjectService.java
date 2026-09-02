package com.nexus.nexus_api.service;

import com.nexus.nexus_api.dto.SubjectRequest;
import com.nexus.nexus_api.exception.ResourceNotFoundException;
import com.nexus.nexus_api.model.StudyPlan;
import com.nexus.nexus_api.model.Subject;
import com.nexus.nexus_api.repository.SubjectRepository;
import com.nexus.nexus_api.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final StudyPlanService studyPlanService;

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

    public void delete(Long id) {
        Subject subject = findByIdOwnedByCurrentUser(id);
        subjectRepository.delete(subject);
    }
}
