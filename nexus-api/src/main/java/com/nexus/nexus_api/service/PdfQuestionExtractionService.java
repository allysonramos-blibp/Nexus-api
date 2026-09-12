package com.nexus.nexus_api.service;

import com.nexus.nexus_api.dto.PdfExtractionResponse;
import com.nexus.nexus_api.dto.QuestionRequest;
import com.nexus.nexus_api.exception.AiServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extrai questões de um PDF via Gemini.
 *
 * IMPORTANTE (histórico do bug "70 questões viram 7"): a versão anterior mandava o PDF
 * inteiro numa ÚNICA chamada ao Gemini, esperando de volta um ÚNICO array JSON com todas
 * as questões. Isso falha de duas formas para provas longas: (1) o JSON de saída de uma
 * prova de 70 questões facilmente passa de 30-50 mil tokens, estourando o teto de saída
 * do modelo; e (2) mesmo sem estourar o teto, é um comportamento conhecido de LLMs "ficarem
 * preguiçosos" em documentos longos e devolverem um JSON válido, mas com só uma fração das
 * questões, sem sinalizar erro nenhum.
 *
 * A solução: dividir o texto do PDF em pedaços (chunks) de tamanho controlado, com uma
 * pequena sobreposição entre eles (pra não cortar uma questão bem no meio), chamar o Gemini
 * uma vez por chunk, e juntar+deduplicar o resultado. Isso limita o tamanho da resposta por
 * chamada a um valor sempre seguro, e isola a falha de um chunk (não derruba a importação
 * inteira se só um pedaço falhar).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfQuestionExtractionService {

    // Teto de segurança pro texto total do PDF — acima disso o número de chunks (e o custo/
    // tempo da importação) fica exagerado; provas normais (até ~150-200 páginas) ficam bem
    // abaixo disso.
    private static final int MAX_TOTAL_INPUT_CHARS = 600_000;

    // Tamanho-alvo de cada chunk mandado ao Gemini. Calibrado com folga generosa: mesmo no
    // pior caso (questão com enunciado longo + explicação + pegadinha, ~700-900 tokens de
    // saída por questão), um chunk de 30k caracteres de entrada não deve gerar mais que uns
    // 15-20 mil tokens de saída — bem abaixo do teto de MAX_OUTPUT_TOKENS.
    private static final int CHUNK_TARGET_CHARS = 30_000;

    // Sobreposição entre chunks consecutivos: se uma questão for cortada bem no fim de um
    // chunk, ela reaparece completa no início do próximo (o prompt instrui o model a ignorar
    // questões parciais/incompletas na borda, então a versão completa vinda do overlap é a
    // que efetivamente entra no resultado).
    private static final int CHUNK_OVERLAP_CHARS = 600;

    // Teto real de saída do gemini-3.6-flash é 65.536 tokens; usamos metade disso por chunk
    // pra deixar folga (inclusive pra "thinking tokens" do model, que consomem do mesmo
    // orçamento de saída).
    private static final int MAX_OUTPUT_TOKENS = 32_768;

    // Heurística SÓ para estimar quantas questões o PDF provavelmente tem, usada apenas para
    // alertar o usuário se a extração real ficou muito abaixo disso — não é uma contagem
    // confiável (pode ter falso positivo em referências tipo "Art. 5." dentro de texto de lei),
    // por isso nunca é tratada como valor exato em lugar nenhum do fluxo.
    private static final Pattern QUESTION_MARKER = Pattern.compile(
            "(?im)^\\s*(?:quest[aã]o\\s+)?0*([1-9]\\d{0,2})\\s*[.)\\-]\\s"
    );

    private static final String SYSTEM_PROMPT_BASE = """
            Você extrai questões de múltipla escolha de provas/simulados a partir do texto bruto de um PDF,
            devolvendo cada questão já classificada e com uma dica pedagógica de pegadinha.

            Preencha cada campo do schema assim:
            - numero: número da questão no PDF, ou null se não identificar.
            - enunciado: o enunciado completo da questão, sem o número.
            - alternativas: uma string por alternativa, sem o prefixo "A)", "B)" etc.
            - disciplinaSugerida: a disciplina/matéria a que a questão pertence (ex.: "Direito Constitucional"),
              sua melhor estimativa mesmo que o PDF não rotule explicitamente; null só se for realmente impossível inferir.
              Um mesmo PDF pode ter VÁRIAS disciplinas diferentes (ex.: Português, depois Inglês, depois Raciocínio
              Lógico) — identifique a disciplina questão a questão, nunca assuma que todo o documento é uma matéria só.
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

            EXTRAIA TODAS AS QUESTÕES COMPLETAS DO TRECHO, sem pular nenhuma, mesmo que sejam muitas. Nunca
            resuma, combine ou "escolha algumas representativas" — cada questão do texto vira um item do array.
            """;

    private static final String CHUNKED_SUFFIX = """

            ATENÇÃO: o texto abaixo é UM TRECHO de um documento maior, não a prova inteira — ele pode começar ou
            terminar no meio de uma questão (o trecho seguinte/anterior continua o documento, com alguma
            sobreposição de texto entre eles). Se a primeira ou a última questão do trecho estiver incompleta
            (faltando enunciado ou alternativas porque foi cortada na borda do trecho), NÃO a inclua no resultado —
            ela vai aparecer completa no trecho vizinho. Só inclua questões que estão inteiras dentro deste trecho.
            """;

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PdfExtractionResponse extract(MultipartFile file) {
        String text = extractText(file);

        if (text.isBlank()) {
            throw new IllegalStateException(
                    "Não consegui extrair texto desse PDF — se ele for uma imagem escaneada (sem texto selecionável), essa importação automática não funciona, só digitando manualmente.");
        }

        if (text.length() > MAX_TOTAL_INPUT_CHARS) {
            throw new IllegalStateException(
                    "Esse PDF tem texto demais pra processar (" + text.length() +
                            " caracteres, teto atual é " + MAX_TOTAL_INPUT_CHARS + "). Divida o arquivo em partes menores.");
        }

        int estimatedTotal = estimateQuestionCount(text);
        List<String> chunks = splitIntoChunks(text);
        log.info("Extração de PDF iniciada: {} caracteres, {} chunk(s), ~{} questões detectadas por heurística",
                text.length(), chunks.size(), estimatedTotal);

        List<QuestionRequest> extracted = new ArrayList<>();
        int chunksFailed = 0;
        boolean isChunked = chunks.size() > 1;

        for (int i = 0; i < chunks.size(); i++) {
            try {
                List<QuestionRequest> fromChunk = callGemini(chunks.get(i), isChunked);
                log.info("Chunk {}/{}: {} questões extraídas", i + 1, chunks.size(), fromChunk.size());
                extracted.addAll(fromChunk);
            } catch (AiServiceException e) {
                chunksFailed++;
                log.warn("Chunk {}/{} falhou na extração (seguindo para os próximos): {}",
                        i + 1, chunks.size(), e.getMessage());
            }
        }

        List<QuestionRequest> deduped = dedupeByNumero(extracted);
        log.info("Extração de PDF concluída: {} questões brutas, {} após dedupe (estimativa era ~{}), {}/{} chunk(s) falharam",
                extracted.size(), deduped.size(), estimatedTotal, chunksFailed, chunks.size());

        return PdfExtractionResponse.of(deduped, estimatedTotal, chunks.size(), chunksFailed);
    }

    private String extractText(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            throw new IllegalStateException("Não consegui ler esse arquivo como PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Estimativa heurística (regex) de quantos números de questão aparecem no texto — só
     * para alertar se a extração real ficou muito abaixo disso. Não é usada para nada além
     * de reporte/alerta (ver ressalvas no Javadoc da classe).
     */
    private int estimateQuestionCount(String text) {
        Set<Integer> numbers = new TreeSet<>();
        Matcher matcher = QUESTION_MARKER.matcher(text);
        while (matcher.find()) {
            try {
                numbers.add(Integer.parseInt(matcher.group(1)));
            } catch (NumberFormatException ignored) {
                // regex não deveria capturar algo não-numérico, mas por segurança ignoramos
            }
        }
        return numbers.size();
    }

    /**
     * Divide o texto em pedaços de ~CHUNK_TARGET_CHARS, preferindo cortar em uma quebra de
     * parágrafo (linha em branco) próxima do alvo pra reduzir a chance de partir uma questão
     * ao meio. Cada chunk (exceto o primeiro) inclui os últimos CHUNK_OVERLAP_CHARS caracteres
     * do chunk anterior, como rede de segurança adicional.
     */
    private List<String> splitIntoChunks(String text) {
        List<String> chunks = new ArrayList<>();
        int len = text.length();

        if (len <= CHUNK_TARGET_CHARS) {
            chunks.add(text);
            return chunks;
        }

        int pos = 0;
        while (pos < len) {
            int target = Math.min(pos + CHUNK_TARGET_CHARS, len);
            int end = target;
            if (target < len) {
                int lastBreak = text.lastIndexOf("\n\n", target);
                if (lastBreak > pos + (CHUNK_TARGET_CHARS / 2)) {
                    end = lastBreak;
                }
            }
            int overlapStart = Math.max(pos - CHUNK_OVERLAP_CHARS, 0);
            chunks.add(text.substring(overlapStart, end));
            pos = end;
        }
        return chunks;
    }

    /**
     * Remove duplicatas por número de questão (esperado por causa do overlap entre chunks —
     * a mesma questão pode ser extraída duas vezes, uma incompleta e ignorada pelo model, e
     * uma completa). Mantém a primeira ocorrência de cada número; questões sem número
     * identificado (numero == null) nunca são deduplicadas entre si, já que não há como saber
     * com segurança se são a mesma questão.
     */
    private List<QuestionRequest> dedupeByNumero(List<QuestionRequest> items) {
        List<QuestionRequest> result = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for (QuestionRequest q : items) {
            if (q.numero() != null && !seen.add(q.numero())) {
                continue;
            }
            result.add(q);
        }
        return result;
    }

    private List<QuestionRequest> callGemini(String chunkText, boolean isChunked) {
        List<Map<String, Object>> contents = List.of(
                Map.of("role", "user", "parts", List.of(Map.of("text", chunkText)))
        );

        String systemPrompt = isChunked ? SYSTEM_PROMPT_BASE + CHUNKED_SUFFIX : SYSTEM_PROMPT_BASE;
        String rawText = geminiClient.generateContent(systemPrompt, contents, questionArraySchema(), MAX_OUTPUT_TOKENS);
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
                    "Um dos trechos da extração não retornou um JSON válido.");
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
