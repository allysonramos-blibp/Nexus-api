package com.nexus.nexus_api.service;

import com.nexus.nexus_api.dto.ChatMessageDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AnthropicChatService {

    private static final String ANTHROPIC_URL = "https://api.anthropic.com/v1/messages";

    @Value("${anthropic.api.key}")
    private String apiKey;

    @Value("${anthropic.model}")
    private String model;

    /**
     * Só é obrigatório quando a chave usada é do tipo "identity-linked" (vinculada à
     * conta/organização, não a um workspace específico) — a própria Anthropic retorna
     * 400 pedindo esse header nesse caso. Chaves do tipo antigo (workspace-scoped)
     * não precisam disso, por isso fica opcional e só é enviado se configurado.
     */
    @Value("${anthropic.workspace.id:}")
    private String workspaceId;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String ask(String userMessage, List<ChatMessageDto> history) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "ANTHROPIC_API_KEY não está configurada no servidor (variável de ambiente ausente ou vazia).");
        }

        List<Map<String, String>> messages = new ArrayList<>();

        if (history != null) {
            for (ChatMessageDto msg : history) {
                messages.add(Map.of("role", msg.role(), "content", msg.content()));
            }
        }
        messages.add(Map.of("role", "user", "content", userMessage));

        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", 1024,
                "system", "Você é um assistente de estudos, ajudando o usuário a se preparar para os planos de estudo, questões e revisões cadastrados no Nexus. Seja direto e didático.",
                "messages", messages
        );

        try {
            String jsonBody = objectMapper.writeValueAsString(body);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(ANTHROPIC_URL))
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("content-type", "application/json");

            if (workspaceId != null && !workspaceId.isBlank()) {
                requestBuilder.header("anthropic-workspace-id", workspaceId);
            }

            HttpRequest request = requestBuilder
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 300) {
                throw new RuntimeException("Anthropic retornou status " + response.statusCode() + ": " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            return root.get("content").get(0).get("text").asText();
        } catch (Exception e) {
            throw new RuntimeException("Falha ao consultar a IA: " + e.getMessage(), e);
        }
    }
}