package com.nexus.nexus_api.service;

import com.nexus.nexus_api.dto.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GeminiChatService {

    private static final String SYSTEM_PROMPT =
            "Você é um assistente de estudos, ajudando o usuário a se preparar para os planos de estudo, " +
                    "questões e revisões cadastrados no Nexus. Seja direto e didático.";

    private final GeminiClient geminiClient;

    public String ask(String userMessage, List<ChatMessageDto> history) {
        List<Map<String, Object>> contents = new ArrayList<>();

        if (history != null) {
            for (ChatMessageDto msg : history) {
                contents.add(toContent(msg.role(), msg.content()));
            }
        }
        contents.add(toContent("user", userMessage));

        // Chat livre não usa responseSchema — deixa o Gemini responder em texto normal.
        return geminiClient.generateContent(SYSTEM_PROMPT, contents, null, 1024);
    }

    /**
     * O Gemini usa os papéis "user" e "model" (não "assistant" como a Anthropic).
     * O histórico salvo no frontend já usa "user"/"assistant" (formato antigo da
     * Anthropic), então convertemos aqui na borda.
     */
    private Map<String, Object> toContent(String role, String text) {
        String geminiRole = "user".equals(role) ? "user" : "model";
        return Map.of("role", geminiRole, "parts", List.of(Map.of("text", text)));
    }
}
