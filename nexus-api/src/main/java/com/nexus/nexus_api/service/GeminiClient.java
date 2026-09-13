package com.nexus.nexus_api.service;

import com.nexus.nexus_api.exception.AiServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente HTTP de baixo nível para a API do Google Gemini (generateContent).
 * Concentra montagem de request, autenticação, timeout e tratamento de erros —
 * {@link GeminiChatService} e {@link PdfQuestionExtractionService} só montam o
 * conteúdo da conversa/prompt e leem o texto de volta.
 */
@Component
public class GeminiClient {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model}")
    private String model;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Chama o Gemini com um system instruction opcional e uma lista de mensagens
     * (role "user"/"model"). Se {@code jsonSchema} não for null, força saída
     * estruturada em JSON validada contra esse schema (formato OpenAPI reduzido
     * que o Gemini espera em generationConfig.responseSchema).
     *
     * @return o texto bruto devolvido pelo model (já garantido não-vazio).
     */
    public String generateContent(String systemInstruction,
                                   List<Map<String, Object>> contents,
                                   Map<String, Object> jsonSchema,
                                   int maxOutputTokens) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiServiceException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "GEMINI_API_KEY não está configurada no servidor (variável de ambiente ausente ou vazia).");
        }
        if (model == null || model.isBlank()) {
            throw new AiServiceException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "gemini.model não está configurado no servidor.");
        }

        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("maxOutputTokens", maxOutputTokens);
        generationConfig.put("temperature", 0.4);
        if (jsonSchema != null) {
            generationConfig.put("responseMimeType", "application/json");
            generationConfig.put("responseSchema", jsonSchema);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        if (systemInstruction != null && !systemInstruction.isBlank()) {
            body.put("system_instruction", Map.of("parts", List.of(Map.of("text", systemInstruction))));
        }
        body.put("contents", contents);
        body.put("generationConfig", generationConfig);

        String url = BASE_URL + model + ":generateContent?key=" + apiKey;

        HttpResponse<String> response;
        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("content-type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE, "Falha ao conectar com a IA: " + e.getMessage(), e);
        }

        if (response.statusCode() == 429) {
            throw new AiServiceException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Limite de uso gratuito do Gemini atingido no momento (rate limit). Aguarde alguns instantes e tente novamente.");
        }
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new AiServiceException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "A chave da API do Gemini foi rejeitada (inválida, expirada ou sem permissão).");
        }
        if (response.statusCode() >= 300) {
            throw new AiServiceException(
                    HttpStatus.BAD_GATEWAY,
                    "Gemini retornou status " + response.statusCode() + ": " + response.body());
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(response.body());
        } catch (Exception e) {
            throw new AiServiceException(HttpStatus.BAD_GATEWAY, "Resposta do Gemini não é um JSON válido.", e);
        }

        JsonNode promptFeedback = root.path("promptFeedback");
        if (promptFeedback.hasNonNull("blockReason")) {
            throw new AiServiceException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "O Gemini bloqueou esta requisição (motivo: " + promptFeedback.path("blockReason").asText() + ").");
        }

        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            throw new AiServiceException(HttpStatus.BAD_GATEWAY, "O Gemini não retornou nenhum candidato de resposta.");
        }

        JsonNode firstCandidate = candidates.get(0);
        String finishReason = firstCandidate.path("finishReason").asText("");
        if ("MAX_TOKENS".equals(finishReason)) {
            throw new AiServiceException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "A resposta da IA foi cortada por exceder o limite de tokens de saída. Tente um texto de entrada menor.");
        }
        if ("SAFETY".equals(finishReason) || "RECITATION".equals(finishReason)) {
            throw new AiServiceException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "O Gemini interrompeu a resposta por motivo de segurança/política de conteúdo (" + finishReason + ").");
        }

        JsonNode parts = firstCandidate.path("content").path("parts");
        StringBuilder text = new StringBuilder();
        if (parts.isArray()) {
            for (JsonNode part : parts) {
                if (part.hasNonNull("text")) {
                    text.append(part.get("text").asText());
                }
            }
        }

        if (text.isEmpty()) {
            throw new AiServiceException(HttpStatus.BAD_GATEWAY, "O Gemini retornou uma resposta vazia.");
        }

        return text.toString();
    }
}
