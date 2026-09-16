package com.nexus.nexus_api.service.pdf;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser determinístico e robusto de questões de múltipla escolha para provas de concursos.
 *
 * Suporta formatos de bancos e bancas brasileiras (FGV, Cespe/Cebraspe, FCC, Vunesp, etc.):
 * - Numeração: "1", "1.", "1)", "1 -", "Questão 1", "QUESTÃO 01".
 * - Alternativas: "(A)", "A)", "A.", "A -", multilinhas e quebras de página.
 * - Limpa cabeçalhos e rodapés comuns que possam confundir a numeração de páginas com questões.
 * - Garante que questões com enunciados e pelo menos 2 alternativas sejam extraídas fielmente.
 */
@Slf4j
@Service
public class PdfQuestionParserService {

    private static final String PAGE_BREAK = "[[NEXUS_PAGE_BREAK]]";

    // Padrão de início de questão: linha contendo apenas o número (1 a 3 dígitos) ou "Questão X"
    private static final Pattern QUESTION_START = Pattern.compile(
            "(?im)^\\s*(?:quest(?:ão|ao)\\s*)?(\\d{1,3})\\s*(?:[\\.\\)\\-:]\\s*)?$"
    );

    // Padrão de alternativa: (A), A), A., A - ou A:
    private static final Pattern ALTERNATIVE_START = Pattern.compile(
            "(?i)^\\s*(?:\\(([A-E])\\)|([A-E])[\\)\\.\\-:])\\s*(.*)$"
    );

    private static final Pattern ALTERNATIVE_WITHOUT_PUNCTUATION = Pattern.compile(
            "(?i)^\\s*([A-E])\\s{2,}(.+)$"
    );

    public PdfParseResult parse(String text, int pageCount) {
        if (text == null || text.isBlank()) {
            return new PdfParseResult(List.of(), List.of(), List.of(), List.of(), pageCount, 0, false);
        }

        String cleanedText = preprocessAndCleanHeaders(text);
        List<Marker> markers = findQuestionMarkers(cleanedText);

        log.info("[PDF-PARSER] Marcadores brutos encontrados: {}", markers.size());

        List<ParsedQuestion> parsed = new ArrayList<>();
        Set<Integer> parsedNumbers = new HashSet<>();
        List<Integer> duplicates = new ArrayList<>();
        int invalid = 0;

        for (int i = 0; i < markers.size(); i++) {
            Marker current = markers.get(i);
            int end = (i + 1 < markers.size()) ? markers.get(i + 1).start() : cleanedText.length();

            if (end <= current.start()) {
                invalid++;
                continue;
            }

            String rawBlock = cleanedText.substring(current.start(), end);
            ParsedQuestion question = parseBlock(current.number(), rawBlock);

            if (question != null) {
                if (parsedNumbers.contains(question.numero())) {
                    duplicates.add(question.numero());
                } else {
                    parsed.add(question);
                    parsedNumbers.add(question.numero());
                }
            } else {
                invalid++;
            }
        }

        parsed.sort(Comparator.comparing(ParsedQuestion::numero, Comparator.nullsLast(Integer::compareTo)));

        List<Integer> found = parsed.stream()
                .map(ParsedQuestion::numero)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();

        List<Integer> missing = findMissingNumbers(found);

        log.info("[PDF-PARSER] Resultado: {} questões extraídas, {} inválidas, {} ausentes, {} duplicadas",
                parsed.size(), invalid, missing, duplicates);

        return new PdfParseResult(parsed, found, missing, duplicates, pageCount, invalid, !cleanedText.isBlank());
    }

    private String preprocessAndCleanHeaders(String text) {
        String normalized = text.replace('\u0000', ' ')
                .replace('\u00A0', ' ')
                .replace("\r\n", "\n")
                .replace('\r', '\n');

        String[] lines = normalized.split("\\n", -1);
        StringBuilder sb = new StringBuilder(normalized.length());

        for (String line : lines) {
            String trimmed = line.trim();

            // Ignora linhas de rodapé com paginação explícita para evitar confundir número de página com questão
            // Ex: "PÁGINA 3", "Página 14 de 20", "TIPO AMARELA – PÁGINA 3"
            if (trimmed.matches("(?i).*P[AÁ]GINA\\s+\\d+.*") || trimmed.matches("(?i).*PAGE\\s+\\d+.*")) {
                continue;
            }

            // Remove repetições de cabeçalhos de provas formais
            if (trimmed.matches("(?i)^EMPRESA DE TECNOLOGIA.*FGV CONHECIMENTO.*$") ||
                trimmed.matches("(?i)^FGV CONHECIMENTO.*$") ||
                trimmed.matches("(?i)^CONCURSO P[UÚ]BLICO.*$")) {
                continue;
            }

            sb.append(trimmed).append("\n");
        }

        return sb.toString();
    }

    private List<Marker> findQuestionMarkers(String text) {
        List<Marker> result = new ArrayList<>();
        Matcher matcher = QUESTION_START.matcher(text);

        while (matcher.find()) {
            int number;
            try {
                number = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                continue;
            }

            if (number < 1 || number > 300) continue;

            // Verificar se o conteúdo que segue possui indicativo de questão (enunciado + alternativas)
            String after = text.substring(matcher.end());
            if (hasPlausibleQuestionContent(after)) {
                result.add(new Marker(number, matcher.start(), matcher.end()));
            }
        }

        return result;
    }

    private boolean hasPlausibleQuestionContent(String after) {
        String sample = after.replace(PAGE_BREAK, " ").strip();
        if (sample.length() < 15) return false;

        // Procura se em até 4000 caracteres existe uma alternativa (A) ou A)
        int checkLimit = Math.min(sample.length(), 4000);
        String window = sample.substring(0, checkLimit);

        return window.matches("(?is).*\\b(?:\\(?[A-E][\\)\\.\\-:]|[A-E]\\s{2,}).*");
    }

    private ParsedQuestion parseBlock(int number, String rawBlock) {
        String body = rawBlock;

        // Remove o prefixo do número inicial
        Matcher questionPrefix = Pattern.compile("(?is)^\\s*(?:quest(?:ão|ao)\\s*)?" + number + "(?:\\s*[\\.\\)\\-:]\\s*|\\s*)").matcher(body);
        if (questionPrefix.find()) {
            body = body.substring(questionPrefix.end());
        }

        body = cleanBlock(body);
        if (body.isBlank()) return null;

        List<AlternativeMarker> alternatives = findAlternatives(body);
        if (alternatives.size() < 2) {
            return null;
        }

        int firstAlternativeStart = alternatives.get(0).start();
        String enunciado = cleanContent(body.substring(0, firstAlternativeStart));
        if (enunciado.length() < 3) return null;

        List<String> optionTexts = new ArrayList<>();
        for (int i = 0; i < alternatives.size(); i++) {
            AlternativeMarker current = alternatives.get(i);
            int end = i + 1 < alternatives.size() ? alternatives.get(i + 1).start() : body.length();
            String option = cleanContent(body.substring(current.contentStart(), end));
            if (!option.isBlank()) {
                optionTexts.add(option);
            }
        }

        if (optionTexts.size() < 2) return null;

        return new ParsedQuestion(
                number,
                enunciado,
                List.copyOf(optionTexts),
                rawBlock.strip(),
                -1,
                -1
        );
    }

    private List<AlternativeMarker> findAlternatives(String body) {
        List<AlternativeMarker> result = new ArrayList<>();
        String[] lines = body.split("\\n", -1);
        int offset = 0;

        for (String line : lines) {
            Matcher matcher = ALTERNATIVE_START.matcher(line);
            if (!matcher.matches()) {
                matcher = ALTERNATIVE_WITHOUT_PUNCTUATION.matcher(line);
            }

            if (matcher.matches()) {
                String label;
                int contentGroup;
                if (matcher.groupCount() >= 3 && matcher.group(1) != null) {
                    label = matcher.group(1).toUpperCase();
                    contentGroup = 3;
                } else if (matcher.groupCount() >= 3 && matcher.group(2) != null) {
                    label = matcher.group(2).toUpperCase();
                    contentGroup = 3;
                } else {
                    label = matcher.group(1).toUpperCase();
                    contentGroup = 2;
                }

                int prefixLength = matcher.start(contentGroup);
                int absoluteStart = offset;
                int absoluteContentStart = offset + prefixLength;
                result.add(new AlternativeMarker(label, absoluteStart, absoluteContentStart));
            }
            offset += line.length() + 1;
        }
        return result;
    }

    private String cleanBlock(String value) {
        return value.replace(PAGE_BREAK, "\n").strip();
    }

    private String cleanContent(String value) {
        String[] lines = value.replace(PAGE_BREAK, "\n").split("\n", -1);
        List<String> cleanedLines = new ArrayList<>();
        boolean previousBlank = false;

        for (String line : lines) {
            String cleaned = line.replaceAll("[ \t]+", " ").trim();
            if (cleaned.isBlank()) {
                if (!previousBlank && !cleanedLines.isEmpty()) {
                    cleanedLines.add("");
                }
                previousBlank = true;
                continue;
            }
            cleanedLines.add(cleaned);
            previousBlank = false;
        }

        while (!cleanedLines.isEmpty() && cleanedLines.get(0).isBlank()) cleanedLines.remove(0);
        while (!cleanedLines.isEmpty() && cleanedLines.get(cleanedLines.size() - 1).isBlank()) {
            cleanedLines.remove(cleanedLines.size() - 1);
        }
        return String.join("\n", cleanedLines).trim();
    }

    private List<Integer> findMissingNumbers(List<Integer> found) {
        if (found.size() < 2) return List.of();
        int min = found.get(0);
        int max = found.get(found.size() - 1);
        Set<Integer> set = new HashSet<>(found);
        List<Integer> missing = new ArrayList<>();
        for (int n = min; n <= max; n++) {
            if (!set.contains(n)) missing.add(n);
        }
        return List.copyOf(missing);
    }

    private record Marker(int number, int start, int end) {}
    private record AlternativeMarker(String label, int start, int contentStart) {}
}
