package com.nexus.nexus_api.dto;

import java.util.List;

public record ChatRequest(
        String message,
        List<ChatMessageDto> history
) {}
