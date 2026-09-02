package com.nexus.nexus_api.dto;

import com.nexus.nexus_api.model.Topic;

public record TopicResponse(
        Long id,
        String nome,
        Integer ordem,
        Long subjectId
) {
    public static TopicResponse from(Topic topic) {
        return new TopicResponse(
                topic.getId(),
                topic.getNome(),
                topic.getOrdem(),
                topic.getSubject().getId()
        );
    }
}
