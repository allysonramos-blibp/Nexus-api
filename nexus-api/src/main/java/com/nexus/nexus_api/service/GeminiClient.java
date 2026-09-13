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

@Component
public class GeminiClient {

    private static final String BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String model;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateContent(
            String systemInstruction,
            List<Map<String, Object>> contents,
            Map<String, Object> jsonSchema,
            int maxOutputTokens
    ) {

        if (apiKey == null || apiKey.isBlank()) {
            throw new AiServiceException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "GEMINI_API_KEY não está configurada no servidor."
            );
        }

        if (model == null || model.isBlank()) {
            throw new AiServiceException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "gemini.model não está configurado no servidor."
            );
        }

        Map<String, Object> generationConfig = new LinkedHashMap<>();

        generationConfig.put(
                "maxOutputTokens",
                maxOutputTokens
        );

        generationConfig.put(
                "temperature",
                0.2
        );

        if (jsonSchema != null) {
            generationConfig.put(
                    "responseMimeType",
                    "application/json"
            );

            generationConfig.put(
                    "responseSchema",
                    jsonSchema
            );
        }

        Map<String, Object> body = new LinkedHashMap<>();

        /*
         * IMPORTANTE:
         * A API REST usa systemInstruction em camelCase.
         */
        if (systemInstruction != null && !systemInstruction.isBlank()) {

            body.put(
                    "systemInstruction",
                    Map.of(
                            "parts",
                            List.of(
                                    Map.of(
                                            "text",
                                            systemInstruction
                                    )
                            )
                    )
            );
        }

        body.put("contents", contents);
        body.put("generationConfig", generationConfig);

        String url =
                BASE_URL
                        + model
                        + ":generateContent?key="
                        + apiKey;

        HttpResponse<String> response;

        try {

            String jsonBody =
                    objectMapper.writeValueAsString(body);

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header(
                                    "Content-Type",
                                    "application/json"
                            )
                            .timeout(
                                    Duration.ofSeconds(120)
                            )
                            .POST(
                                    HttpRequest.BodyPublishers
                                            .ofString(jsonBody)
                            )
                            .build();

            response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new AiServiceException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "A comunicação com o Gemini foi interrompida.",
                    e
            );

        } catch (Exception e) {

            throw new AiServiceException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Falha ao conectar com a IA: "
                            + e.getMessage(),
                    e
            );
        }

        /*
         * LOG SEGURO:
         * nunca mostramos a API key.
         */
        System.out.println(
                "[GEMINI] HTTP "
                        + response.statusCode()
                        + " | model="
                        + model
        );

        if (response.statusCode() == 429) {

            throw new AiServiceException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Limite de uso do Gemini atingido. Aguarde alguns instantes e tente novamente."
            );
        }

        if (response.statusCode() == 401 ||
                response.statusCode() == 403) {

            throw new AiServiceException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "A chave da API do Gemini foi rejeitada."
            );
        }

        if (response.statusCode() >= 300) {

            String errorBody = response.body();

            /*
             * Evita mensagens absurdamente grandes.
             */
            if (errorBody.length() > 3000) {
                errorBody =
                        errorBody.substring(0, 3000);
            }

            System.err.println(
                    "[GEMINI ERROR] HTTP "
                            + response.statusCode()
                            + ": "
                            + errorBody
            );

            throw new AiServiceException(
                    HttpStatus.BAD_GATEWAY,
                    "Gemini retornou HTTP "
                            + response.statusCode()
                            + ". Detalhes: "
                            + errorBody
            );
        }

        JsonNode root;

        try {

            root =
                    objectMapper.readTree(
                            response.body()
                    );

        } catch (Exception e) {

            throw new AiServiceException(
                    HttpStatus.BAD_GATEWAY,
                    "Resposta do Gemini não é um JSON válido.",
                    e
            );
        }

        JsonNode promptFeedback =
                root.path("promptFeedback");

        if (promptFeedback.hasNonNull("blockReason")) {

            throw new AiServiceException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "O Gemini bloqueou a requisição. Motivo: "
                            + promptFeedback
                            .path("blockReason")
                            .asText()
            );
        }

        JsonNode candidates =
                root.path("candidates");

        if (!candidates.isArray()
                || candidates.isEmpty()) {

            throw new AiServiceException(
                    HttpStatus.BAD_GATEWAY,
                    "O Gemini não retornou nenhum candidato de resposta."
            );
        }

        JsonNode firstCandidate =
                candidates.get(0);

        String finishReason =
                firstCandidate
                        .path("finishReason")
                        .asText("");

        if ("MAX_TOKENS".equals(finishReason)) {

            throw new AiServiceException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "A resposta da IA foi cortada por limite de tokens."
            );
        }

        if ("SAFETY".equals(finishReason)
                || "RECITATION".equals(finishReason)) {

            throw new AiServiceException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "O Gemini interrompeu a resposta por política de conteúdo. Motivo: "
                            + finishReason
            );
        }

        JsonNode parts =
                firstCandidate
                        .path("content")
                        .path("parts");

        StringBuilder text =
                new StringBuilder();

        if (parts.isArray()) {

            for (JsonNode part : parts) {

                if (part.hasNonNull("text")) {

                    text.append(
                            part.get("text")
                                    .asText()
                    );
                }
            }
        }

        if (text.isEmpty()) {

            throw new AiServiceException(
                    HttpStatus.BAD_GATEWAY,
                    "O Gemini retornou uma resposta vazia."
            );
        }

        return text.toString();
    }
}