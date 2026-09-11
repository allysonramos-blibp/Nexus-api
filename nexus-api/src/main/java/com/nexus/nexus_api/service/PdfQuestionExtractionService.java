package com.nexus.nexus_api.service;

import com.nexus.nexus_api.dto.QuestionRequest;
import com.nexus.nexus_api.exception.AiServiceException;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PdfQuestionExtractionService {

    // Texto muito grande em uma única chamada arrisca estourar o limite de saída do
    // modelo (a resposta vem truncada e não parseia como JSON) — acima disso, melhor
    // o usuário dividir o PDF em partes menores. Ajustado para o teto real de saída do
    // gemini-3.6-flash (65.536 tokens) — bem maior que os 8.192 da Claude, que era o
    // que definia esses números antes da migração.
    private static final int MAX_INPUT_CHARS = 200_000;
    private static final int MAX_OUTPUT_TOKENS = 32_768;

    private static final String SYSTEM_PROMPT = """
            Você extrai questões de múltipla escolha de provas/simulados a partir do texto bruto de um PDF,
            devolvendo cada questão já classificada e com uma dica pedagógica de pegadinha.

            Preencha cada campo do schema assim:
            - numero: número da questão no PDF, ou null se não identificar.
            - enunciado: o enunciado completo da questão, sem o número.
            - alternativas: uma string por alternativa, sem o prefixo "A)", "B)" etc.
            - disciplinaSugerida: a disciplina/matéria a que a questão pertence (ex.: "Direito Constitucional"),
              sua melhor estimativa mesmo que o PDF não rotule explicitamente; null só se for realmente impossível inferir.
            - assuntoSugerido: o assunto/tópico específico dentro da disciplina (ex.: "Controle de Constitucionalidade").
            - dificuldade: "FACIL", "MEDIA" ou "DIFICIL" — só se o PDF indicar isso explicitamente, senão null.
            - gabarito: o TEXTO EXATO (idêntico, caractere a caractere) de uma das strings em "alternativas" —
              NUNCA a letra sozinha. Se o gabarito estiver numa lista separada (ex.: uma seção "GABARITO" no fim
              do documento com algo como "1-A 2-C 3-D..."), cruze o número da questão com essa lista e resolva
              qual alternativa aquela letra representa, copiando o texto dela. Se não conseguir identificar o
              gabarito com confiança, deixe como uma string vazia "" — não invente uma resposta.
            - explicacao: comentário/justificativa da resposta, se o PDF trouxer; senão sua própria explicação
              objetiva de por que aquela alternativa é a correta.
            - pegadinha: uma frase curta descrevendo o tipo de armadilha que a banca costuma usar nesse cenário
              (ex.: "a banca troca 'deve' por 'pode' na alternativa errada para confundir o candidato"); null se
              não houver pegadinha identificável.
            - banca: banca organizadora, se identificável; senão null.
            - ano: ano da prova, se identificável; senão null.

            Ignore cabeçalhos, rodapés, numeração de página e qualquer coisa que não seja questão ou gabarito.
            """;

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<QuestionRequest> extract(MultipartFile file) {
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

        return callGemini(text);
    }

    private String extractText(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            throw new IllegalStateException("Não consegui ler esse arquivo como PDF: " + e.getMessage(), e);
        }
    }

    private List<QuestionRequest> callGemini(String pdfText) {
        List<Map<String, Object>> contents = List.of(
                Map.of("role", "user", "parts", List.of(Map.of("text", pdfText)))
        );

        String rawText = geminiClient.generateContent(SYSTEM_PROMPT, contents, questionArraySchema(), MAX_OUTPUT_TOKENS);
        return parseQuestions(rawText);
    }

    /**
     * Schema OpenAPI-reduzido exigido pelo Gemini em generationConfig.responseSchema
     * para forçar a saída em JSON estruturado (evita ter que "pedir educadamente"
     * por JSON e torcer para o model não embrulhar em markdown).
     */
    private Map<String, Object> questionArraySchema() {
        Map<String, Object> itemSchema = Map.of(
                "type", "OBJECT",
                "properties", Map.ofEntries(
                        Map.entry("numero", Map.of("type", "INTEGER", "nullable", true)),
                        Map.entry("enunciado", Map.of("type", "STRING")),
                        Map.entry("alternativas", Map.of("type", "ARRAY", "items", Map.of("type", "STRING"))),
                        Map.entry("disciplinaSugerida", Map.of("type", "STRING", "nullable", true)),
                        Map.entry("assuntoSugerido", Map.of("type", "STRING", "nullable", true)),
                        Map.entry("dificuldade", Map.of(
                                "type", "STRING", "nullable", true,
                                "enum", List.of("FACIL", "MEDIA", "DIFICIL"))),
                        Map.entry("gabarito", Map.of("type", "STRING")),
                        Map.entry("explicacao", Map.of("type", "STRING", "nullable", true)),
                        Map.entry("pegadinha", Map.of("type", "STRING", "nullable", true)),
                        Map.entry("banca", Map.of("type", "STRING", "nullable", true)),
                        Map.entry("ano", Map.of("type", "INTEGER", "nullable", true))
                ),
                "required", List.of("enunciado", "alternativas", "gabarito")
        );

        return Map.of("type", "ARRAY", "items", itemSchema);
    }

    private List<QuestionRequest> parseQuestions(String rawText) {
        // Com responseMimeType=application/json o Gemini normalmente já devolve JSON puro,
        // mas mantemos essa limpeza como rede de segurança contra variações do model.
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
            throw new AiServiceException(
                    HttpStatus.BAD_GATEWAY,
                    "A extração não retornou um JSON válido — tente novamente ou com um PDF menor.");
        }

        List<QuestionRequest> result = new ArrayList<>();
        for (JsonNode node : array) {
            List<String> alternativas = new ArrayList<>();
            node.path("alternativas").forEach(alt -> alternativas.add(alt.asText("")));

            result.add(new QuestionRequest(
                    node.hasNonNull("numero") ? node.get("numero").asInt() : null,
                    node.path("enunciado").asText(""),
                    alternativas,
                    parseDificuldade(node.path("dificuldade").asText(null)),
                    node.path("gabarito").asText(""),
                    node.hasNonNull("explicacao") ? node.get("explicacao").asText() : null,
                    node.hasNonNull("pegadinha") ? node.get("pegadinha").asText() : null,
                    node.hasNonNull("disciplinaSugerida") ? node.get("disciplinaSugerida").asText() : null,
                    node.hasNonNull("assuntoSugerido") ? node.get("assuntoSugerido").asText() : null,
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