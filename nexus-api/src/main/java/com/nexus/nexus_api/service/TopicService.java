package com.nexus.nexus_api.service;

import com.nexus.nexus_api.dto.TopicRequest;
import com.nexus.nexus_api.exception.ResourceNotFoundException;
import com.nexus.nexus_api.model.Subject;
import com.nexus.nexus_api.model.Topic;
import com.nexus.nexus_api.repository.TopicRepository;
import com.nexus.nexus_api.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;
    private final SubjectService subjectService;

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

    public void delete(Long id) {
        Topic topic = findByIdOwnedByCurrentUser(id);
        topicRepository.delete(topic);
    }
}
