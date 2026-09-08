package com.nexus.nexus_api.service;

import com.nexus.nexus_api.dto.QuestionRequest;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PdfQuestionExtractionService {

    private static final String ANTHROPIC_URL = "https://api.anthropic.com/v1/messages";
    // Texto muito grande em uma única chamada arrisca estourar o limite de saída do
    // modelo (a resposta vem truncada e não parseia como JSON) — acima disso, melhor
    // o usuário dividir o PDF em partes menores.
    private static final int MAX_INPUT_CHARS = 60_000;

    @Value("${anthropic.api.key}")
    private String apiKey;

    @Value("${anthropic.model}")
    private String model;

    @Value("${anthropic.workspace.id:}")
    private String workspaceId;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<QuestionRequest> extract(MultipartFile file) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "ANTHROPIC_API_KEY não está configurada no servidor (variável de ambiente ausente ou vazia).");
        }

        String text = extractText(file);

        if (text.isBlank()) {
            throw new IllegalStateException(
                    "Não consegui extrair texto desse PDF — se ele for uma imagem escaneada (sem texto selecionável), essa importação automática não funciona, só digitando manualmente.");
        }

        if (text.length() > MAX_INPUT_CHARS) {
            throw new IllegalStateException(
                    "Esse PDF tem texto demais pra processar de uma vez (" + text.length() +
                            " caracteres). Divida em partes menores (ex.: 30-40 questões por arquivo) e importe cada uma separadamente.");
        }

        return callAnthropic(text);
    }

    private String extractText(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            throw new IllegalStateException("Não consegui ler esse arquivo como PDF: " + e.getMessage(), e);
        }
    }

    private List<QuestionRequest> callAnthropic(String pdfText) {
        String systemPrompt = """
                Você extrai questões de múltipla escolha de provas/simulados a partir do texto bruto de um PDF.

                Responda APENAS com um array JSON válido, sem markdown, sem comentário, sem texto antes ou depois.
                Cada item do array deve ter exatamente estes campos:
                - "numero": número da questão no PDF (inteiro), ou null se não identificar
                - "enunciado": o enunciado completo da questão, sem o número
                - "alternativas": array de strings com o texto de cada alternativa, sem o prefixo "A)", "B)" etc.
                - "dificuldade": "FACIL", "MEDIA" ou "DIFICIL" — só se o PDF indicar isso explicitamente, senão null
                - "gabarito": o TEXTO EXATO (idêntico, caractere a caractere) de uma das strings em "alternativas" —
                  NUNCA a letra sozinha. Se o gabarito estiver numa lista separada (ex.: uma seção "GABARITO" no fim
                  do documento com algo como "1-A 2-C 3-D..."), cruze o número da questão com essa lista e resolva
                  qual alternativa aquela letra representa, copiando o texto dela.
                - "explicacao": comentário/justificativa da resposta, se o PDF trouxer; senão null
                - "banca": banca organizadora, se identificável; senão null
                - "ano": ano da prova, se identificável; senão null

                Se não conseguir identificar o gabarito de uma questão com confiança, ainda inclua a questão no
                array, mas deixe "gabarito" como uma string vazia "" — não invente uma resposta.
                Ignore cabeçalhos, rodapés, numeração de página e qualquer coisa que não seja questão ou gabarito.
                """;

        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", 8192,
                "system", systemPrompt,
                "messages", List.of(Map.of("role", "user", "content", pdfText))
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
            String stopReason = root.path("stop_reason").asText("");
            String rawText = root.get("content").get(0).get("text").asText();

            if ("max_tokens".equals(stopReason)) {
                throw new IllegalStateException(
                        "O PDF tem questões demais pra extrair de uma vez só — a resposta foi cortada. Divida o arquivo em partes menores e tente de novo.");
            }

            return parseQuestions(rawText);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Falha ao extrair questões do PDF: " + e.getMessage(), e);
        }
    }

    private List<QuestionRequest> parseQuestions(String rawText) {
        // Claude às vezes embrulha em ```json apesar da instrução — tira antes de parsear.
        String cleaned = rawText.strip();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```(json)?", "").trim();
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
            }
        }

        JsonNode array;
        try {
            array = objectMapper.readTree(cleaned);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "A extração não retornou um JSON válido — tente novamente ou com um PDF menor.");
        }

        List<QuestionRequest> result = new ArrayList<>();
        for (JsonNode node : array) {
            List<String> alternativas = new ArrayList<>();
            node.path("alternativas").forEach(alt -> alternativas.add(alt.asText("")));

            result.add(new QuestionRequest(
                    node.path("numero").isNull() || !node.hasNonNull("numero") ? null : node.get("numero").asInt(),
                    node.path("enunciado").asText(""),
                    alternativas,
                    parseDificuldade(node.path("dificuldade").asText(null)),
                    node.path("gabarito").asText(""),
                    node.hasNonNull("explicacao") ? node.get("explicacao").asText() : null,
                    node.hasNonNull("banca") ? node.get("banca").asText() : null,
                    node.hasNonNull("ano") ? node.get("ano").asInt() : null
            ));
        }
        return result;
    }

    private com.nexus.nexus_api.model.QuestionDifficulty parseDificuldade(String raw) {
        if (raw == null) return null;
        try {
            return com.nexus.nexus_api.model.QuestionDifficulty.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
