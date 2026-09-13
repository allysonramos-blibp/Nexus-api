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

    public void delete(Long id) {
        Subject subject = findByIdOwnedByCurrentUser(id);
        subjectRepository.delete(subject);
    }
}
