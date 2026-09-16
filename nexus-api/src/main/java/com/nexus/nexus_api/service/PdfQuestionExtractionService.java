package com.nexus.nexus_api.service;

import com.nexus.nexus_api.dto.PdfExtractionResponse;
import com.nexus.nexus_api.dto.QuestionRequest;
import com.nexus.nexus_api.service.pdf.ParsedQuestion;
import com.nexus.nexus_api.service.pdf.PdfParseResult;
import com.nexus.nexus_api.service.pdf.PdfQuestionParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.*;

/**
 * Pipeline híbrido de extração de questões de PDF:
 *
 * 1. PDFs digitais com texto vetorial (FGV, Cespe, FCC, provas de concurso geradas digitalmente):
 *    Processado deterministicamente via PDFBox + parser de padrões regex.
 *    Instantâneo (< 100ms), 100% gratuito (sem chamadas de IA/tokens).
 *
 * 2. PDFs escaneados / imagens de celular (ex: CamScanner, Xerox, impressos sem camada OCR):
 *    Processado via Gemini Vision Multimodal (leitura visual de páginas),
 *    extraindo todas as questões, alternativas (a,b,c,d,e) e matérias de cabeçalho.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfQuestionExtractionService {

    private static final int MAX_TOTAL_INPUT_CHARS = 1_500_000;
    private final PdfQuestionParserService parserService;
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public record ExtractionResult(
            List<QuestionRequest> questoes,
            int possivelTotalNoPdf,
            List<Integer> numerosAusentes,
            List<Integer> numerosDuplicados,
            int chunksProcessados,
            int chunksComFalha
    ) {}

    /** Mantém o endpoint antigo /api/questions/extract-pdf. */
    public PdfExtractionResponse extract(MultipartFile file) {
        ExtractionResult result = extractDetailed(file);
        return PdfExtractionResponse.of(
                result.questoes(),
                result.possivelTotalNoPdf(),
                result.chunksProcessados(),
                result.chunksComFalha()
        );
    }

    /** Usado pelo fluxo de importação por plano de estudos. */
    public ExtractionResult extractDetailed(MultipartFile file) {
        validateFile(file);

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível ler os bytes do arquivo enviado: " + e.getMessage(), e);
        }

        try (PDDocument document = Loader.loadPDF(fileBytes)) {
            int pageCount = document.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(false);
            stripper.setPageStart("\n[[NEXUS_PAGE_BREAK]]\n");
            String text = stripper.getText(document);

            boolean hasSignificantText = text != null && text.trim().length() >= 50;
            PdfParseResult parsed = null;

            if (hasSignificantText) {
                if (text.length() > MAX_TOTAL_INPUT_CHARS) {
                    throw new IllegalStateException(
                            "O PDF é grande demais para a importação automática nesta versão (" + text.length() + " caracteres)."
                    );
                }
                parsed = parserService.parse(text, pageCount);
            }

            // Se a extração determinística encontrou questões, usamos o resultado determinístico!
            if (parsed != null && !parsed.questoes().isEmpty()) {
                List<QuestionRequest> questions = parsed.questoes().stream()
                        .map(this::toQuestionRequest)
                        .toList();

                log.info("[PDF] Extração determinística concluída: páginas={}, questões={}, números={}, ausentes={}, duplicados={}, inválidas={}",
                        parsed.paginasProcessadas(), questions.size(), parsed.numerosEncontrados(),
                        parsed.numerosAusentes(), parsed.numerosDuplicados(), parsed.questoesInvalidas());

                return new ExtractionResult(
                        questions,
                        parsed.numerosEncontrados().size(),
                        parsed.numerosAusentes(),
                        parsed.numerosDuplicados(),
                        1,
                        0
                );
            }

            // Se não encontrou questões via texto digital (caso típico de PDF escaneado/imagem/CamScanner),
            // ativamos a leitura visual por visão computacional (Gemini Vision)
            log.info("[PDF] PDF sem camada de texto legível ou sem questões via texto puro ({} págs). Acionando Gemini Vision Multimodal...", pageCount);

            if (geminiClient.isConfigured()) {
                return extractWithGeminiVision(fileBytes, pageCount);
            } else {
                throw new IllegalStateException(
                        "O PDF enviado é uma imagem escaneada (como CamScanner) e requer a chave GEMINI_API_KEY configurada para leitura visual."
                );
            }

        } catch (IOException e) {
            log.error("[PDF] Erro ao ler documento PDF", e);
            throw new IllegalStateException("Não foi possível processar o arquivo PDF: " + e.getMessage(), e);
        }
    }

    private ExtractionResult extractWithGeminiVision(byte[] fileBytes, int pageCount) {
        try {
            String base64Pdf = Base64.getEncoder().encodeToString(fileBytes);

            Map<String, Object> inlineData = Map.of(
                    "mimeType", "application/pdf",
                    "data", base64Pdf
            );

            String prompt = """
                    Você é um especialista em transcrição e extração de provas e exames acadêmicos e de concursos (impressos, escaneados via CamScanner ou digitais).
                    Examine visualmente cada página deste documento e extraia com máxima precisão TODAS as questões presentes (de múltipla escolha e dissertativas).

                    Instruções:
                    1. Identifique o número de cada questão (ex: 1, 2, 3...) e a disciplina/matéria caso indicada no cabeçalho ou antes das questões (ex: 'FISIOLOGIA III', 'FARMACOLOGIA I', 'PATOLOGIA I', 'NEUROANATOMIA', 'Português', etc.).
                    2. Extraia o enunciado completo, incluindo textos de suporte, tabelas e itens (I, II, III).
                    3. Para cada alternativa (A, B, C, D, E ou a, b, c, d), extraia o texto correspondente.
                    4. Se houver questão dissertativa/discursiva, extraia o enunciado e adicione a opção "[Questão Dissertativa]" como única alternativa.
                    5. Se houver gabarito anotado ou evidente no documento, indique a letra em 'gabarito' (ex: "A", "B", "C", "D"), senão deixe vazio "".

                    Retorne estritamente um JSON no formato:
                    {
                      "questoes": [
                        {
                          "numero": 1,
                          "disciplina": "Fisiologia III",
                          "enunciado": "...",
                          "alternativas": ["a) ...", "b) ...", "c) ...", "d) ..."],
                          "gabarito": "D"
                        }
                      ]
                    }
                    """;

            List<Map<String, Object>> contents = List.of(
                    Map.of(
                            "role", "user",
                            "parts", List.of(
                                    Map.of("inlineData", inlineData),
                                    Map.of("text", prompt)
                            )
                    )
            );

            Map<String, Object> schema = buildJsonSchema();

            String responseText = geminiClient.generateContent(
                    "Você é um assistente de extração precisa de exames, provas e concursos.",
                    contents,
                    schema,
                    8192
            );

            return parseGeminiResponse(responseText, pageCount);
        } catch (Exception e) {
            log.error("[PDF] Erro ao extrair com Gemini Vision", e);
            throw new IllegalStateException(
                    "Não foi possível extrair questões da prova escaneada: " + e.getMessage(),
                    e
            );
        }
    }

    private Map<String, Object> buildJsonSchema() {
        Map<String, Object> itemProps = new LinkedHashMap<>();
        itemProps.put("numero", Map.of("type", "INTEGER"));
        itemProps.put("disciplina", Map.of("type", "STRING"));
        itemProps.put("enunciado", Map.of("type", "STRING"));
        itemProps.put("alternativas", Map.of(
                "type", "ARRAY",
                "items", Map.of("type", "STRING")
        ));
        itemProps.put("gabarito", Map.of("type", "STRING"));

        Map<String, Object> itemSchema = new LinkedHashMap<>();
        itemSchema.put("type", "OBJECT");
        itemSchema.put("properties", itemProps);
        itemSchema.put("required", List.of("numero", "enunciado", "alternativas"));

        Map<String, Object> rootProps = new LinkedHashMap<>();
        rootProps.put("questoes", Map.of(
                "type", "ARRAY",
                "items", itemSchema
        ));

        Map<String, Object> rootSchema = new LinkedHashMap<>();
        rootSchema.put("type", "OBJECT");
        rootSchema.put("properties", rootProps);
        rootSchema.put("required", List.of("questoes"));

        return rootSchema;
    }

    private ExtractionResult parseGeminiResponse(String responseText, int pageCount) {
        try {
            JsonNode root = objectMapper.readTree(responseText);
            JsonNode questoesNode = root.path("questoes");
            if (!questoesNode.isArray() || questoesNode.isEmpty()) {
                throw new IllegalStateException("Nenhuma questão foi identificada na imagem da prova.");
            }

            List<QuestionRequest> questions = new ArrayList<>();
            List<Integer> numerosEncontrados = new ArrayList<>();

            for (JsonNode qNode : questoesNode) {
                int numero = qNode.path("numero").asInt(questions.size() + 1);
                String enunciado = qNode.path("enunciado").asText("").trim();
                String disciplina = qNode.path("disciplina").asText("Geral").trim();
                String gabarito = qNode.path("gabarito").asText("").trim();

                List<String> alternativas = new ArrayList<>();
                JsonNode altNode = qNode.path("alternativas");
                if (altNode.isArray()) {
                    for (JsonNode alt : altNode) {
                        String txt = alt.asText("").trim();
                        if (!txt.isBlank()) alternativas.add(txt);
                    }
                }

                if (alternativas.isEmpty()) {
                    alternativas.add("[Questão Dissertativa]");
                }

                if (!enunciado.isBlank()) {
                    numerosEncontrados.add(numero);
                    questions.add(new QuestionRequest(
                            numero,
                            enunciado,
                            alternativas,
                            null,
                            gabarito,
                            null,
                            null,
                            disciplina,
                            "Geral",
                            null,
                            null
                    ));
                }
            }

            log.info("[PDF] Gemini Vision extraiu {} questões da prova escaneada ({} páginas).",
                    questions.size(), pageCount);

            return new ExtractionResult(
                    questions,
                    questions.size(),
                    List.of(),
                    List.of(),
                    1,
                    0
            );
        } catch (Exception e) {
            log.error("[PDF] Erro ao interpretar resposta JSON do Gemini: {}", responseText, e);
            throw new IllegalStateException("Erro ao processar as questões extraídas pela IA: " + e.getMessage(), e);
        }
    }

    private QuestionRequest toQuestionRequest(ParsedQuestion question) {
        return new QuestionRequest(
                question.numero(),
                question.enunciado(),
                question.alternativas(),
                null,
                "",
                null,
                null,
                question.disciplinaSugerida(),
                null,
                null,
                null
        );
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Envie um arquivo PDF para importar.");
        }
        String name = file.getOriginalFilename();
        if (name != null && !name.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("O arquivo enviado precisa ser um PDF.");
        }
    }
}
