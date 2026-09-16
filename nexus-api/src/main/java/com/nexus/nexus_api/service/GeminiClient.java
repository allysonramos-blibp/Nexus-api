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
    private static final String FALLBACK_MODEL = "gemini-3.5-flash-lite";

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model:gemini-3.6-flash}")
    private String model;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

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
        if (!isConfigured()) {
            throw new AiServiceException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "GEMINI_API_KEY não está configurada no servidor (variável de ambiente ausente ou vazia).");
        }

        String primaryModel = (model != null && !model.isBlank()) ? model : "gemini-3.6-flash";

        try {
            return doGenerateContent(primaryModel, systemInstruction, contents, jsonSchema, maxOutputTokens);
        } catch (AiServiceException e) {
            // Se o modelo principal falhar com 429 (rate limit) ou 404 (modelo obsoleto/não encontrado),
            // tenta o fallback leve automático (gemini-3.5-flash-lite)
            if (!primaryModel.equals(FALLBACK_MODEL) &&
                    (e.getStatus() == HttpStatus.TOO_MANY_REQUESTS || e.getStatus() == HttpStatus.NOT_FOUND || e.getStatus() == HttpStatus.BAD_GATEWAY)) {
                try {
                    return doGenerateContent(FALLBACK_MODEL, systemInstruction, contents, jsonSchema, maxOutputTokens);
                } catch (Exception fallbackEx) {
                    throw e;
                }
            }
            throw e;
        }
    }

    private String doGenerateContent(String targetModel,
                                     String systemInstruction,
                                     List<Map<String, Object>> contents,
                                     Map<String, Object> jsonSchema,
                                     int maxOutputTokens) {
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("maxOutputTokens", maxOutputTokens);
        generationConfig.put("temperature", 0.2);

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

        String url = BASE_URL + targetModel + ":generateContent?key=" + apiKey;

        HttpResponse<String> response;
        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("content-type", "application/json")
                    .timeout(Duration.ofSeconds(180))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE, "Falha ao conectar com a IA: " + e.getMessage(), e);
        }

        if (response.statusCode() == 404) {
            throw new AiServiceException(
                    HttpStatus.NOT_FOUND,
                    "Modelo Gemini não encontrado (" + targetModel + "): " + response.body());
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
                    "A resposta da IA foi cortada por exceder o limite de tokens de saída. Tente um arquivo menor ou divida o conteúdo.");
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
