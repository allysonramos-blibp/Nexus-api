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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Serviço responsável pela extração de questões de provas em PDF utilizando Gemini.
 *
 * Estratégia:
 *
 * 1. Extrai o texto do PDF usando PDFBox.
 * 2. Detecta os marcadores das questões.
 * 3. Divide o documento em blocos de aproximadamente 8 questões.
 * 4. Envia cada bloco separadamente ao Gemini.
 * 5. O Gemini retorna JSON estruturado.
 * 6. Junta todos os resultados.
 * 7. Remove duplicidades pelo número da questão.
 * 8. Informa questões ausentes e chunks que falharam.
 *
 * Essa abordagem evita enviar blocos gigantes ao Gemini e reduz o risco de:
 *
 * - JSON truncado;
 * - limite de output;
 * - perda de questões no final do chunk;
 * - falha específica em blocos grandes de Conhecimentos Específicos;
 * - problemas com questões contendo código, XML, JSON ou tabelas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfQuestionExtractionService {

    /*
     * Limite de segurança para o texto total do PDF.
     */
    private static final int MAX_TOTAL_INPUT_CHARS = 600_000;

    /*
     * Número aproximado de questões por chamada ao Gemini.
     *
     * Para provas como Dataprev:
     *
     * 1–8
     * 9–16
     * 17–24
     * ...
     *
     * A divisão real respeita os marcadores encontrados no PDF.
     */
    private static final int QUESTIONS_PER_CHUNK = 6;

    /*
     * Quantidade máxima de caracteres permitida em um chunk.
     *
     * Essa proteção existe para casos em que uma única questão seja
     * extremamente longa.
     */
    private static final int MAX_CHUNK_CHARS = 10_000;

    /*
     * Tentativas por trecho antes de considerar o trecho perdido.
     */
    private static final int MAX_ATTEMPTS_PER_CHUNK = 3;

    /*
     * Profundidade máxima de subdivisão de um trecho problemático.
     */
    private static final int MAX_SPLIT_DEPTH = 2;

    /*
     * Abaixo disso não vale a pena continuar dividindo.
     */
    private static final int MIN_SPLIT_CHARS = 1_200;

    /*
     * Questões por chamada na segunda passada (recuperação).
     */
    private static final int RECOVERY_QUESTIONS_PER_CHUNK = 3;

    /*
     * Sobreposição entre chunks.
     *
     * A divisão é feita por questão, então a sobreposição é pequena e serve
     * apenas como segurança para documentos com estrutura irregular.
     */
    private static final int CHUNK_OVERLAP_CHARS = 1_500;

    /*
     * Limite de saída por chamada ao Gemini.
     *
     * Como agora cada chamada contém poucas questões, 16k tokens é suficiente
     * na grande maioria dos casos e reduz o risco de resposta truncada.
     */
    private static final int MAX_OUTPUT_TOKENS = 16_384;

    /*
     * Detecta questões no formato:
     *
     * 1.
     * 2)
     * 3-
     *
     * ou:
     *
     * Questão 1
     * Questão 2.
     *
     * Também aceita o número sozinho em uma linha:
     *
     * 41
     * Assinale...
     */
    private static final Pattern QUESTION_MARKER = Pattern.compile(
            "(?im)^[ \\t\\u00a0]*(?:quest[aã]o|quest\\.)?[ \\t\\u00a0]*0*([1-9]\\d{0,2})[ \\t\\u00a0]*(?:[.)\\-–—:]+[ \\t\\u00a0]*|$)"
    );

    private static final String SYSTEM_PROMPT_BASE = """
            Você é um extrator especializado de questões de concursos públicos.

            Você receberá um TRECHO de uma prova em texto bruto extraído de PDF.

            Sua tarefa é transformar TODAS as questões completas presentes nesse trecho
            em objetos JSON seguindo exatamente o schema fornecido.

            REGRAS OBRIGATÓRIAS:

            1. Extraia TODAS as questões completas presentes no trecho.
            2. NÃO pule questões.
            3. NÃO resuma questões.
            4. NÃO combine duas questões.
            5. Cada questão deve virar exatamente um objeto.
            6. Preserve o texto original das alternativas.
            7. Remova apenas o prefixo da alternativa, como A), B), C), D) ou E).
            8. O campo "numero" deve conter o número original da questão.
            9. Não invente questões.
            10. Não invente alternativas.
            11. Não invente gabaritos.
            12. Se não houver gabarito identificável, use "".
            13. Se uma questão estiver claramente cortada no início ou no final do trecho,
                não a inclua.
            14. Não inclua cabeçalhos, rodapés ou números de página como questões.

            CLASSIFICAÇÃO:

            - disciplinaSugerida:
              Identifique a disciplina/matéria da questão com base no conteúdo.

            - assuntoSugerido:
              Identifique o assunto específico dentro da disciplina.

            IMPORTANTE:
            O documento pode possuir várias disciplinas.
            Nunca assuma que todas as questões pertencem à mesma disciplina.

            Exemplos de disciplinas:
            - Língua Portuguesa
            - Inglês
            - Raciocínio Lógico
            - Atualidades
            - Legislação
            - Engenharia de Software
            - Banco de Dados
            - Segurança da Informação
            - Arquitetura de Software
            - Desenvolvimento de Software

            Para Conhecimentos Específicos, seja específico conforme o conteúdo.
            Por exemplo:
            - Spring
            - REST
            - Scrum
            - Testes de Software
            - Redes
            - Segurança
            - Banco de Dados
            - NoSQL
            - ETL
            - BI
            - Arquitetura de Software

            DIFICULDADE:

            Use:
            - FACIL
            - MEDIA
            - DIFICIL

            Se não houver informação suficiente para determinar a dificuldade,
            use null.

            GABARITO:

            O campo "gabarito" deve conter o TEXTO EXATO da alternativa correta.

            NUNCA coloque somente:
            A
            B
            C
            D
            E

            Se o documento apresentar um gabarito separado, tente relacionar o número
            da questão com a letra correspondente e depois copie o texto completo
            da alternativa.

            Se não for possível determinar o gabarito com segurança:
            use "".

            EXPLICAÇÃO:

            Se o PDF trouxer explicação, preserve-a de forma objetiva.

            Caso não exista explicação no PDF, produza uma explicação curta e objetiva
            baseada na questão.

            A explicação deve ter no máximo 2 frases.

            PEGADINHA:

            Informe uma frase curta sobre a possível armadilha da questão.

            Caso não exista uma pegadinha identificável:
            use null.

            BANCA:

            Identifique a banca quando estiver disponível.

            ANO:

            Identifique o ano quando estiver disponível.

            IMPORTANTE SOBRE A RESPOSTA:

            Retorne SOMENTE o array JSON.

            Não utilize:
            - Markdown
            - ```json
            - comentários
            - texto antes do JSON
            - texto depois do JSON

            O resultado deve ser um JSON válido.
            """;

    private static final String CHUNKED_SUFFIX = """

            ATENÇÃO:

            Este texto é apenas um trecho de uma prova maior.

            Analise somente as questões completas presentes neste trecho.

            Se a primeira questão estiver incompleta porque começou antes do trecho,
            ignore essa questão.

            Se a última questão estiver incompleta porque continua no próximo trecho,
            ignore essa questão.

            Não tente reconstruir uma questão cortada usando suposições.

            Extraia todas as outras questões completas.
            """;

    private final GeminiClient geminiClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Resultado detalhado da extração.
     */
    public record ExtractionResult(
            List<QuestionRequest> questoes,
            int possivelTotalNoPdf,
            List<Integer> numerosAusentes,
            List<Integer> numerosDuplicados,
            int chunksProcessados,
            int chunksComFalha
    ) {
    }

    /**
     * Contrato antigo.
     *
     * Mantido para compatibilidade com:
     *
     * POST /api/questions/extract-pdf
     */
    public PdfExtractionResponse extract(MultipartFile file) {

        ExtractionResult result = extractDetailed(file);

        return PdfExtractionResponse.of(
                result.questoes(),
                result.possivelTotalNoPdf(),
                result.chunksProcessados(),
                result.chunksComFalha()
        );
    }

    /**
     * Extração detalhada utilizada pelo importador no nível do plano.
     */
    public ExtractionResult extractDetailed(MultipartFile file) {

        String text = extractText(file);

        if (text == null || text.isBlank()) {

            throw new IllegalStateException(
                    "Não consegui extrair texto desse PDF. " +
                    "Se o arquivo for um PDF escaneado como imagem, " +
                    "será necessário utilizar OCR ou importar outro arquivo."
            );
        }

        if (text.length() > MAX_TOTAL_INPUT_CHARS) {

            throw new IllegalStateException(
                    "Esse PDF possui " +
                    text.length() +
                    " caracteres, acima do limite atual de " +
                    MAX_TOTAL_INPUT_CHARS +
                    ". Divida o PDF em partes menores."
            );
        }

        /*
         * Detecta os números das questões presentes no documento.
         */
        Set<Integer> numerosDetectados = estimateQuestionNumbers(text);

        /*
         * Divide o PDF por questões.
         */
        List<String> chunks = splitIntoQuestionChunks(text);

        log.info(
                "[PDF] Texto extraído: {} caracteres | questões detectadas: {} | chunks: {}",
                text.length(),
                numerosDetectados.size(),
                chunks.size()
        );

        List<QuestionRequest> extractedRaw = new ArrayList<>();

        int chunksFailed = 0;

        for (int i = 0; i < chunks.size(); i++) {

            String chunk = chunks.get(i);

            try {

                log.info(
                        "[PDF] Processando chunk {}/{} | {} caracteres",
                        i + 1,
                        chunks.size(),
                        chunk.length()
                );

                List<QuestionRequest> fromChunk =
                        callGeminiResilient(chunk, chunks.size() > 1, 0);

                log.info(
                        "[PDF] Chunk {}/{} concluído: {} questão(ões)",
                        i + 1,
                        chunks.size(),
                        fromChunk.size()
                );

                extractedRaw.addAll(fromChunk);

            } catch (AiServiceException e) {

                chunksFailed++;

                log.error(
                        "[PDF] Chunk {}/{} falhou: {}",
                        i + 1,
                        chunks.size(),
                        e.getMessage(),
                        e
                );

            } catch (Exception e) {

                chunksFailed++;

                log.error(
                        "[PDF] Erro inesperado no chunk {}/{}",
                        i + 1,
                        chunks.size(),
                        e
                );
            }
        }

        /*
         * Segunda passada: tenta recuperar as questões detectadas no PDF
         * que não vieram na primeira rodada (inclusive as de chunks que falharam).
         */
        try {

            List<QuestionRequest> recuperadas =
                    recoverMissingQuestions(text, numerosDetectados, extractedRaw);

            if (!recuperadas.isEmpty()) {

                log.info(
                        "[PDF] Segunda passada recuperou {} questão(ões)",
                        recuperadas.size()
                );

                extractedRaw.addAll(recuperadas);
            }

        } catch (Exception e) {

            log.error("[PDF] Falha na segunda passada de recuperação", e);
        }

        /*
         * Detecta duplicações antes do dedupe.
         */
        List<Integer> duplicados =
                findDuplicateNumeros(extractedRaw);

        /*
         * Remove duplicações.
         */
        List<QuestionRequest> deduped =
                dedupeByNumero(extractedRaw);

        /*
         * Identifica os números realmente extraídos.
         */
        Set<Integer> numerosExtraidos =
                new HashSet<>();

        for (QuestionRequest question : deduped) {

            if (question.numero() != null) {
                numerosExtraidos.add(question.numero());
            }
        }

        /*
         * Descobre quais números detectados no PDF não apareceram
         * no resultado do Gemini.
         */
        List<Integer> ausentes =
                numerosDetectados.stream()
                        .filter(numero -> !numerosExtraidos.contains(numero))
                        .sorted()
                        .toList();

        /*
         * Log final.
         */
        log.info(
                "[PDF] Extração concluída | brutas={} | finais={} | detectadas={} | ausentes={} | duplicadas={} | falhas={}/{}",
                extractedRaw.size(),
                deduped.size(),
                numerosDetectados.size(),
                ausentes.size(),
                duplicados.size(),
                chunksFailed,
                chunks.size()
        );

        /*
         * Se TODOS os chunks falharam, não silencie o problema.
         *
         * Antes isso acabava aparecendo no frontend como:
         *
         * "Não encontrei questões nesse PDF."
         *
         * quando, na verdade, o Gemini tinha falhado.
         */
        if (deduped.isEmpty() && !chunks.isEmpty() && chunksFailed == chunks.size()) {

            throw new AiServiceException(
                    HttpStatus.BAD_GATEWAY,
                    "Não foi possível extrair as questões do PDF. " +
                    "Todos os " +
                    chunks.size() +
                    " trecho(s) enviados para a IA falharam. " +
                    "Verifique os logs do Gemini."
            );
        }

        return new ExtractionResult(
                deduped,
                numerosDetectados.size(),
                ausentes,
                duplicados,
                chunks.size(),
                chunksFailed
        );
    }

    /**
     * Extrai o texto bruto usando PDFBox.
     */
    private String extractText(MultipartFile file) {

        if (file == null || file.isEmpty()) {

            throw new IllegalArgumentException(
                    "Nenhum arquivo PDF foi enviado."
            );
        }

        try (PDDocument document =
                     Loader.loadPDF(file.getBytes())) {

            PDFTextStripper stripper =
                    new PDFTextStripper();

            /*
             * Mantemos a ordem das páginas.
             */
            stripper.setSortByPosition(true);

            return stripper.getText(document);

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Não consegui ler esse arquivo como PDF: " +
                    e.getMessage(),
                    e
            );
        }
    }

    /**
     * Detecta os números das questões existentes no PDF.
     */
    private Set<Integer> estimateQuestionNumbers(String text) {

        Set<Integer> numbers =
                new TreeSet<>();

        Matcher matcher =
                QUESTION_MARKER.matcher(text);

        while (matcher.find()) {

            try {

                int number =
                        Integer.parseInt(matcher.group(1));

                /*
                 * Evita considerar números absurdos como questões.
                 *
                 * Provas normalmente ficam abaixo de 1000.
                 */
                if (number > 0 && number <= 999) {
                    numbers.add(number);
                }

            } catch (NumberFormatException ignored) {
                // Ignora marcador inválido.
            }
        }

        return numbers;
    }

    /**
     * Divide o documento por marcadores de questão.
     *
     * Exemplo:
     *
     * 1
     * ...
     * 2
     * ...
     * 3
     * ...
     *
     * vira aproximadamente:
     *
     * Chunk 1 -> questões 1–8
     * Chunk 2 -> questões 9–16
     * Chunk 3 -> questões 17–24
     * ...
     *
     * Isso é mais seguro que simplesmente cortar a cada X caracteres.
     */
    private List<String> splitIntoQuestionChunks(String text) {

        List<QuestionMarkerPosition> markers =
                findQuestionMarkers(text);

        /*
         * Se não encontramos marcadores suficientes,
         * utilizamos fallback por caracteres.
         */
        if (markers.isEmpty()) {

            log.warn(
                    "[PDF] Nenhum marcador de questão encontrado. " +
                    "Usando fallback por caracteres."
            );

            return splitByCharacterLimit(text);
        }

        List<QuestionBlock> questionBlocks =
                buildQuestionBlocks(text, markers);

        if (questionBlocks.isEmpty()) {

            return splitByCharacterLimit(text);
        }

        List<String> chunks =
                new ArrayList<>();

        StringBuilder currentChunk =
                new StringBuilder();

        int questionsInCurrentChunk = 0;

        for (QuestionBlock block : questionBlocks) {

            String blockText =
                    block.text();

            /*
             * Questão individual extremamente grande.
             *
             * Nesse caso não temos como dividi-la por outra questão.
             */
            if (blockText.length() > MAX_CHUNK_CHARS) {

                if (currentChunk.length() > 0) {

                    chunks.add(
                            currentChunk.toString()
                    );

                    currentChunk.setLength(0);
                    questionsInCurrentChunk = 0;
                }

                /*
                 * Divide a questão grande em pedaços apenas como último recurso.
                 */
                List<String> oversized =
                        splitLargeQuestion(blockText);

                chunks.addAll(oversized);

                continue;
            }

            boolean atingiuQuantidade =
                    questionsInCurrentChunk >= QUESTIONS_PER_CHUNK;

            boolean ultrapassaTamanho =
                    currentChunk.length() > 0 &&
                    currentChunk.length() + blockText.length()
                            > MAX_CHUNK_CHARS;

            if (atingiuQuantidade || ultrapassaTamanho) {

                chunks.add(
                        currentChunk.toString()
                );

                /*
                 * Pequena sobreposição textual como segurança.
                 */
                String overlap =
                        getTail(
                                currentChunk.toString(),
                                CHUNK_OVERLAP_CHARS
                        );

                currentChunk.setLength(0);

                if (!overlap.isBlank()) {

                    currentChunk
                            .append(overlap)
                            .append("\n\n");
                }

                questionsInCurrentChunk = 0;
            }

            currentChunk
                    .append(blockText)
                    .append("\n\n");

            questionsInCurrentChunk++;
        }

        if (currentChunk.length() > 0) {

            chunks.add(
                    currentChunk.toString()
            );
        }

        return chunks;
    }

    /**
     * Encontra a posição de cada marcador de questão.
     */
    private List<QuestionMarkerPosition> findQuestionMarkers(
            String text
    ) {

        List<QuestionMarkerPosition> markers =
                new ArrayList<>();

        Matcher matcher =
                QUESTION_MARKER.matcher(text);

        while (matcher.find()) {

            try {

                int number =
                        Integer.parseInt(matcher.group(1));

                if (number > 0 && number <= 999) {

                    markers.add(
                            new QuestionMarkerPosition(
                                    number,
                                    matcher.start()
                            )
                    );
                }

            } catch (NumberFormatException ignored) {
                // Ignora.
            }
        }

        /*
         * Remove posições duplicadas.
         *
         * Isso pode acontecer em alguns PDFs quando o PDFBox
         * produz linhas repetidas.
         */
        Map<Integer, QuestionMarkerPosition> unique =
                new LinkedHashMap<>();

        for (QuestionMarkerPosition marker : markers) {

            unique.putIfAbsent(
                    marker.position(),
                    marker
            );
        }

        return new ArrayList<>(
                unique.values()
        );
    }

    /**
     * Fallback quando o PDF não possui marcadores detectáveis.
     */
    private List<String> splitByCharacterLimit(
            String text
    ) {

        List<String> chunks =
                new ArrayList<>();

        int length =
                text.length();

        int position = 0;

        while (position < length) {

            int end =
                    Math.min(
                            position + MAX_CHUNK_CHARS,
                            length
                    );

            if (end < length) {

                int lineBreak =
                        text.lastIndexOf(
                                "\n\n",
                                end
                        );

                if (lineBreak > position + 5_000) {

                    end = lineBreak;
                }
            }

            if (end <= position) {
                break;
            }

            String chunk =
                    text.substring(
                            position,
                            end
                    ).trim();

            if (!chunk.isBlank()) {

                chunks.add(chunk);
            }

            position = end;
        }

        return chunks;
    }

    /**
     * Divide uma única questão excepcionalmente grande.
     */
    private List<String> splitLargeQuestion(
            String text
    ) {

        List<String> chunks =
                new ArrayList<>();

        int position = 0;

        while (position < text.length()) {

            int end =
                    Math.min(
                            position + MAX_CHUNK_CHARS,
                            text.length()
                    );

            if (end < text.length()) {

                int lineBreak =
                        text.lastIndexOf(
                                "\n\n",
                                end
                        );

                if (lineBreak > position + 5_000) {

                    end = lineBreak;
                }
            }

            if (end <= position) {
                break;
            }

            chunks.add(
                    text.substring(
                            position,
                            end
                    ).trim()
            );

            position = end;
        }

        return chunks;
    }

    /**
     * Obtém os últimos caracteres de um texto.
     */
    private String getTail(
            String text,
            int amount
    ) {

        if (text == null || text.isBlank()) {
            return "";
        }

        if (text.length() <= amount) {
            return text;
        }

        return text.substring(
                text.length() - amount
        );
    }

    /**
     * Chamada individual ao Gemini.
     */
    private List<QuestionRequest> callGemini(
            String chunkText,
            boolean isChunked
    ) {

        if (chunkText == null ||
                chunkText.isBlank()) {

            return List.of();
        }

        List<Map<String, Object>> contents =
                List.of(
                        Map.of(
                                "role",
                                "user",
                                "parts",
                                List.of(
                                        Map.of(
                                                "text",
                                                chunkText
                                        )
                                )
                        )
                );

        String systemPrompt =
                isChunked
                        ? SYSTEM_PROMPT_BASE + CHUNKED_SUFFIX
                        : SYSTEM_PROMPT_BASE;

        String rawText =
                geminiClient.generateContent(
                        systemPrompt,
                        contents,
                        questionArraySchema(),
                        MAX_OUTPUT_TOKENS
                );

        return parseQuestions(rawText);
    }

    /**
     * Schema JSON enviado ao Gemini.
     *
     * IMPORTANTE:
     *
     * Não usamos "nullable": true.
     *
     * Campos opcionais simplesmente não precisam aparecer na resposta.
     *
     * Isso evita incompatibilidades com structured output do Gemini.
     */
    private Map<String, Object> questionArraySchema() {

        Map<String, Object> itemSchema =
                Map.of(
                        "type",
                        "OBJECT",

                        "properties",
                        Map.ofEntries(

                                Map.entry(
                                        "numero",
                                        Map.of(
                                                "type",
                                                "INTEGER"
                                        )
                                ),

                                Map.entry(
                                        "enunciado",
                                        Map.of(
                                                "type",
                                                "STRING"
                                        )
                                ),

                                Map.entry(
                                        "alternativas",
                                        Map.of(
                                                "type",
                                                "ARRAY",
                                                "items",
                                                Map.of(
                                                        "type",
                                                        "STRING"
                                                )
                                        )
                                ),

                                Map.entry(
                                        "disciplinaSugerida",
                                        Map.of(
                                                "type",
                                                "STRING"
                                        )
                                ),

                                Map.entry(
                                        "assuntoSugerido",
                                        Map.of(
                                                "type",
                                                "STRING"
                                        )
                                ),

                                Map.entry(
                                        "dificuldade",
                                        Map.of(
                                                "type",
                                                "STRING",
                                                "enum",
                                                List.of(
                                                        "FACIL",
                                                        "MEDIA",
                                                        "DIFICIL"
                                                )
                                        )
                                ),

                                Map.entry(
                                        "gabarito",
                                        Map.of(
                                                "type",
                                                "STRING"
                                        )
                                ),

                                Map.entry(
                                        "explicacao",
                                        Map.of(
                                                "type",
                                                "STRING"
                                        )
                                ),

                                Map.entry(
                                        "pegadinha",
                                        Map.of(
                                                "type",
                                                "STRING"
                                        )
                                ),

                                Map.entry(
                                        "banca",
                                        Map.of(
                                                "type",
                                                "STRING"
                                        )
                                ),

                                Map.entry(
                                        "ano",
                                        Map.of(
                                                "type",
                                                "INTEGER"
                                        )
                                )
                        ),

                        "required",
                        List.of(
                                "enunciado",
                                "alternativas",
                                "gabarito"
                        )
                );

        return Map.of(
                "type",
                "ARRAY",
                "items",
                itemSchema
        );
    }

    /**
     * Converte o JSON retornado pelo Gemini para QuestionRequest.
     */
    private List<QuestionRequest> parseQuestions(
            String rawText
    ) {

        if (rawText == null ||
                rawText.isBlank()) {

            throw new AiServiceException(
                    HttpStatus.BAD_GATEWAY,
                    "O Gemini retornou uma resposta vazia durante a extração."
            );
        }

        String cleaned =
                rawText.strip();

        /*
         * Remove markdown caso o modelo devolva
         * ```json apesar do responseMimeType.
         */
        if (cleaned.startsWith("```")) {

            cleaned =
                    cleaned.replaceFirst(
                            "^```(?:json)?",
                            ""
                    ).trim();

            if (cleaned.endsWith("```")) {

                cleaned =
                        cleaned.substring(
                                0,
                                cleaned.length() - 3
                        ).trim();
            }
        }

        JsonNode array;

        try {

            array =
                    objectMapper.readTree(
                            cleaned
                    );

        } catch (Exception e) {

            log.error(
                    "[PDF] Gemini retornou JSON inválido: {}",
                    truncate(cleaned, 2_000),
                    e
            );

            throw new AiServiceException(
                    HttpStatus.BAD_GATEWAY,
                    "Um dos trechos da extração não retornou um JSON válido."
            );
        }

        if (array == null ||
                !array.isArray()) {

            throw new AiServiceException(
                    HttpStatus.BAD_GATEWAY,
                    "O Gemini não retornou uma lista de questões válida."
            );
        }

        List<QuestionRequest> result =
                new ArrayList<>();

        for (JsonNode node : array) {

            if (node == null ||
                    !node.isObject()) {

                continue;
            }

            String enunciado =
                    node.path(
                            "enunciado"
                    ).asText("");

            List<String> alternativas =
                    new ArrayList<>();

            JsonNode alternativesNode =
                    node.path(
                            "alternativas"
                    );

            if (alternativesNode.isArray()) {

                alternativesNode.forEach(
                        alt ->
                                alternativas.add(
                                        alt.asText("")
                                )
                );
            }

            /*
             * Não adiciona objetos completamente vazios.
             */
            if (enunciado.isBlank() &&
                    alternativas.isEmpty()) {

                continue;
            }

            result.add(
                    new QuestionRequest(

                            node.hasNonNull(
                                    "numero"
                            )
                                    ? node.get(
                                            "numero"
                                    ).asInt()
                                    : null,

                            enunciado,

                            alternativas,

                            parseDificuldade(
                                    node.hasNonNull(
                                            "dificuldade"
                                    )
                                            ? node.get(
                                                    "dificuldade"
                                            ).asText()
                                            : null
                            ),

                            node.path(
                                    "gabarito"
                            ).asText(""),

                            node.hasNonNull(
                                    "explicacao"
                            )
                                    ? node.get(
                                            "explicacao"
                                    ).asText()
                                    : null,

                            node.hasNonNull(
                                    "pegadinha"
                            )
                                    ? node.get(
                                            "pegadinha"
                                    ).asText()
                                    : null,

                            node.hasNonNull(
                                    "disciplinaSugerida"
                            )
                                    ? node.get(
                                            "disciplinaSugerida"
                                    ).asText()
                                    : null,

                            node.hasNonNull(
                                    "assuntoSugerido"
                            )
                                    ? node.get(
                                            "assuntoSugerido"
                                    ).asText()
                                    : null,

                            node.hasNonNull(
                                    "banca"
                            )
                                    ? node.get(
                                            "banca"
                                    ).asText()
                                    : null,

                            node.hasNonNull(
                                    "ano"
                            )
                                    ? node.get(
                                            "ano"
                                    ).asInt()
                                    : null
                    )
            );
        }

        return result;
    }

    /**
     * Converte dificuldade para o enum da aplicação.
     */
    private com.nexus.nexus_api.model.QuestionDifficulty parseDificuldade(
            String raw
    ) {

        if (raw == null ||
                raw.isBlank()) {

            return null;
        }

        try {

            return com.nexus.nexus_api.model.QuestionDifficulty
                    .valueOf(
                            raw.trim()
                                    .toUpperCase()
                    );

        } catch (IllegalArgumentException e) {

            return null;
        }
    }

    /**
     * Identifica números duplicados antes do dedupe.
     */
    private List<Integer> findDuplicateNumeros(
            List<QuestionRequest> items
    ) {

        Set<Integer> seen =
                new HashSet<>();

        Set<Integer> duplicated =
                new TreeSet<>();

        for (QuestionRequest q : items) {

            if (q.numero() != null) {

                if (!seen.add(q.numero())) {

                    duplicated.add(
                            q.numero()
                    );
                }
            }
        }

        return new ArrayList<>(
                duplicated
        );
    }

    /**
     * Remove duplicidades por número.
     *
     * Questões sem número não são deduplicadas.
     */
    private List<QuestionRequest> dedupeByNumero(
            List<QuestionRequest> items
    ) {

        List<QuestionRequest> result =
                new ArrayList<>();

        Set<Integer> seen =
                new HashSet<>();

        for (QuestionRequest q : items) {

            if (q.numero() != null) {

                if (!seen.add(q.numero())) {

                    continue;
                }
            }

            result.add(q);
        }

        return result;
    }

    /**
     * Limita texto de log para evitar logs gigantes.
     */
    private String truncate(
            String text,
            int max
    ) {

        if (text == null) {
            return "";
        }

        if (text.length() <= max) {
            return text;
        }

        return text.substring(
                0,
                max
        ) + "...";
    }

    /**
     * Monta os blocos de texto de cada questão a partir dos marcadores.
     */
    private List<QuestionBlock> buildQuestionBlocks(
            String text,
            List<QuestionMarkerPosition> markers
    ) {

        List<QuestionBlock> questionBlocks =
                new ArrayList<>();

        for (int i = 0; i < markers.size(); i++) {

            QuestionMarkerPosition current =
                    markers.get(i);

            int start =
                    current.position();

            int end =
                    i + 1 < markers.size()
                            ? markers.get(i + 1).position()
                            : text.length();

            if (start >= end) {
                continue;
            }

            String block =
                    text.substring(start, end).trim();

            if (!block.isBlank()) {

                questionBlocks.add(
                        new QuestionBlock(
                                current.number(),
                                block
                        )
                );
            }
        }

        return questionBlocks;
    }

    /**
     * Chama o Gemini com novas tentativas e, em último caso,
     * divide o trecho ao meio para contornar respostas truncadas.
     */
    private List<QuestionRequest> callGeminiResilient(
            String chunkText,
            boolean isChunked,
            int depth
    ) {

        RuntimeException last = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS_PER_CHUNK; attempt++) {

            try {

                return callGemini(chunkText, isChunked);

            } catch (RuntimeException e) {

                last = e;

                log.warn(
                        "[PDF] Tentativa {}/{} do trecho falhou: {}",
                        attempt,
                        MAX_ATTEMPTS_PER_CHUNK,
                        e.getMessage()
                );

                if (attempt < MAX_ATTEMPTS_PER_CHUNK) {
                    sleepQuietly(attempt * 1_500L);
                }
            }
        }

        if (depth < MAX_SPLIT_DEPTH &&
                chunkText.length() > MIN_SPLIT_CHARS) {

            List<String> halves =
                    splitInHalf(chunkText);

            if (halves.size() == 2) {

                List<QuestionRequest> partial =
                        new ArrayList<>();

                boolean algumaMetadeOk = false;

                for (String half : halves) {

                    try {

                        partial.addAll(
                                callGeminiResilient(half, true, depth + 1)
                        );

                        algumaMetadeOk = true;

                    } catch (RuntimeException e) {

                        log.error(
                                "[PDF] Metade do trecho falhou: {}",
                                e.getMessage()
                        );
                    }
                }

                if (algumaMetadeOk) {
                    return partial;
                }
            }
        }

        throw last;
    }

    /**
     * Divide um trecho em duas partes, preferindo o marcador de questão
     * mais próximo do meio.
     */
    private List<String> splitInHalf(String text) {

        int middle = text.length() / 2;

        int cut = -1;

        Matcher matcher = QUESTION_MARKER.matcher(text);

        while (matcher.find()) {

            int start = matcher.start();

            if (start <= 0 || start >= text.length()) {
                continue;
            }

            if (cut < 0 ||
                    Math.abs(start - middle) < Math.abs(cut - middle)) {

                cut = start;
            }
        }

        if (cut <= 0) {
            cut = text.lastIndexOf("\n\n", middle);
        }

        if (cut <= 0 || cut >= text.length() - 1) {
            cut = middle;
        }

        String first = text.substring(0, cut).trim();
        String second = text.substring(cut).trim();

        if (first.isBlank() || second.isBlank()) {
            return List.of(text);
        }

        return List.of(first, second);
    }

    /**
     * Segunda passada: reenvia apenas os blocos das questões que ficaram faltando.
     */
    private List<QuestionRequest> recoverMissingQuestions(
            String text,
            Set<Integer> numerosDetectados,
            List<QuestionRequest> jaExtraidas
    ) {

        Set<Integer> presentes = new HashSet<>();

        for (QuestionRequest q : jaExtraidas) {

            if (q.numero() != null) {
                presentes.add(q.numero());
            }
        }

        List<Integer> faltantes =
                numerosDetectados.stream()
                        .filter(numero -> !presentes.contains(numero))
                        .sorted()
                        .toList();

        if (faltantes.isEmpty()) {
            return List.of();
        }

        List<QuestionMarkerPosition> markers =
                findQuestionMarkers(text);

        if (markers.isEmpty()) {
            return List.of();
        }

        Map<Integer, String> porNumero = new LinkedHashMap<>();

        for (QuestionBlock block : buildQuestionBlocks(text, markers)) {
            porNumero.putIfAbsent(block.number(), block.text());
        }

        log.info(
                "[PDF] Tentando recuperar {} questão(ões) ausentes: {}",
                faltantes.size(),
                faltantes
        );

        List<QuestionRequest> recuperadas = new ArrayList<>();

        StringBuilder buffer = new StringBuilder();

        int noBuffer = 0;

        for (Integer numero : faltantes) {

            String bloco = porNumero.get(numero);

            if (bloco == null || bloco.isBlank()) {
                continue;
            }

            boolean cheio =
                    noBuffer >= RECOVERY_QUESTIONS_PER_CHUNK ||
                    (noBuffer > 0 &&
                            buffer.length() + bloco.length() > MAX_CHUNK_CHARS);

            if (cheio) {

                recuperadas.addAll(
                        callGeminiQuietly(buffer.toString())
                );

                buffer.setLength(0);
                noBuffer = 0;
            }

            buffer.append(bloco).append("\n\n");
            noBuffer++;
        }

        if (noBuffer > 0) {

            recuperadas.addAll(
                    callGeminiQuietly(buffer.toString())
            );
        }

        return recuperadas;
    }

    /**
     * Chamada que nunca propaga erro: usada na recuperação.
     */
    private List<QuestionRequest> callGeminiQuietly(String chunkText) {

        try {

            return callGeminiResilient(chunkText, true, 0);

        } catch (RuntimeException e) {

            log.error(
                    "[PDF] Recuperação falhou para um trecho: {}",
                    e.getMessage()
            );

            return List.of();
        }
    }

    private void sleepQuietly(long millis) {

        try {

            Thread.sleep(millis);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
        }
    }

    /**
     * Representa um marcador encontrado no texto.
     */
    private record QuestionMarkerPosition(
            int number,
            int position
    ) {
    }

    /**
     * Representa uma questão individual antes de formar chunks.
     */
    private record QuestionBlock(
            int number,
            String text
    ) {
    }
}