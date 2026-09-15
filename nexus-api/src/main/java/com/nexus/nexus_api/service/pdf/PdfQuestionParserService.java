package com.nexus.nexus_api.service.pdf;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser determinístico de questões de múltipla escolha.
 *
 * A regra principal é simples: primeiro identifica blocos de questões no texto do PDF;
 * depois, dentro de cada bloco, identifica as alternativas. Nenhuma chamada externa/IA é feita.
 */
@Slf4j
@Service
public class PdfQuestionParserService {

    private static final String PAGE_BREAK = "[[NEXUS_PAGE_BREAK]]";

    /**
     * Aceita:
     * 1.
     * 1)
     * 1-
     * 1:
     * Questão 1
     * Questao 1
     * Questão 1.
     * e números sozinhos em linha.
     */
    private static final Pattern QUESTION_START = Pattern.compile(
            "(?im)^\\s*(?:quest(?:ão|ao)\\s*)?(\\d{1,3})(?:\\s*[\\.\\)\\-:]\\s*|\\s*)$"
    );

    /** Alternativas tradicionais A) ... E), A. ... E., A- ... ou A: ... */
    private static final Pattern ALTERNATIVE_START = Pattern.compile(
            "(?i)^\\s*(?:\\(([A-F])\\)|([A-F])[\\)\\.\\-:])\\s*(.*)$"
    );

    /** Algumas provas usam apenas "A texto" no começo da linha. */
    private static final Pattern ALTERNATIVE_WITHOUT_PUNCTUATION = Pattern.compile(
            "(?i)^\\s*([A-F])\\s{2,}(.+)$"
    );


    public PdfParseResult parse(String text, int pageCount) {
        if (text == null || text.isBlank()) {
            return new PdfParseResult(List.of(), List.of(), List.of(), List.of(), pageCount, 0, false);
        }

        String normalized = normalizeText(text);
        List<Marker> markers = findQuestionMarkers(normalized);

        // Se houver muitos falsos positivos, usa a sequência mais consistente de números.
        markers = selectBestMarkerSequence(markers);

        List<ParsedQuestion> parsed = new ArrayList<>();
        int invalid = 0;

        for (int i = 0; i < markers.size(); i++) {
            Marker current = markers.get(i);
            int end = i + 1 < markers.size() ? markers.get(i + 1).start() : normalized.length();
            if (end <= current.start()) {
                invalid++;
                continue;
            }

            String rawBlock = normalized.substring(current.start(), end);
            ParsedQuestion question = parseBlock(current.number(), rawBlock);
            if (question == null) {
                invalid++;
                continue;
            }
            parsed.add(question);
        }

        parsed.sort(Comparator.comparing(ParsedQuestion::numero, Comparator.nullsLast(Integer::compareTo)));

        List<Integer> found = parsed.stream()
                .map(ParsedQuestion::numero)
                .filter(n -> n != null)
                .distinct()
                .sorted()
                .toList();

        List<Integer> duplicates = findDuplicates(markers);
        List<Integer> missing = findMissingNumbers(found);

        log.info("[PDF-PARSER] {} marcador(es), {} questão(ões) válidas, {} inválida(s), números={}, ausentes={}, duplicados={}",
                markers.size(), parsed.size(), invalid, found, missing, duplicates);

        return new PdfParseResult(parsed, found, missing, duplicates, pageCount, invalid, true);
    }

    private String normalizeText(String text) {
        String normalized = text.replace('\u0000', ' ')
                .replace('\u00A0', ' ')
                .replace("\r\n", "\n")
                .replace('\r', '\n');

        // Mantém o marcador de página criado pelo PDFTextStripper, mas limpa espaços laterais.
        String[] lines = normalized.split("\\n", -1);
        StringBuilder out = new StringBuilder(normalized.length());
        for (String line : lines) {
            String cleaned = line.replaceAll("[ \\t]+$", "").trim();
            out.append(cleaned).append('\n');
        }
        return out.toString();
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
            if (number < 1 || number > 999) continue;

            // Um número sozinho precisa ter conteúdo suficiente depois dele; isso reduz
            // falsos positivos de números de página isolados.
            String after = text.substring(matcher.end());
            if (matcher.group(0).trim().matches("\\d{1,3}") && !hasPlausibleQuestionContent(after)) {
                continue;
            }

            result.add(new Marker(number, matcher.start(), matcher.end()));
        }
        return result;
    }

    private boolean hasPlausibleQuestionContent(String after) {
        String sample = after.replace(PAGE_BREAK, " ").strip();
        if (sample.length() < 20) return false;
        String first = sample.split("\\n", 2)[0].trim();
        return first.length() >= 5;
    }

    /**
     * Remove candidatos muito improváveis. Em PDFs reais, a sequência de questões é a melhor
     * evidência para distinguir um número de questão de número de página/artigo/código.
     */
    private List<Marker> selectBestMarkerSequence(List<Marker> markers) {
        if (markers.size() <= 1) return markers;

        List<Marker> ordered = new ArrayList<>(markers);
        List<Marker> best = new ArrayList<>();

        // Primeiro tenta encontrar a maior sequência crescente de +1.
        List<Marker> current = new ArrayList<>();
        for (Marker marker : ordered) {
            if (current.isEmpty()) {
                current.add(marker);
                continue;
            }
            int previous = current.get(current.size() - 1).number();
            if (marker.number() == previous + 1) {
                current.add(marker);
            } else if (marker.number() > previous) {
                if (current.size() > best.size()) best = new ArrayList<>(current);
                current = new ArrayList<>();
                current.add(marker);
            }
        }
        if (current.size() > best.size()) best = current;

        // Se a sequência ficou curta, mantém todos os marcadores: provas podem ter blocos,
        // questões anuladas ou numeração não consecutiva.
        if (best.size() < Math.min(3, markers.size())) return markers;

        // Reconstroi a lista preservando marcadores repetidos que pertençam à mesma região.
        int minStart = best.get(0).start();
        int maxStart = best.get(best.size() - 1).start();
        List<Marker> filtered = new ArrayList<>();
        for (Marker marker : ordered) {
            if (marker.start() >= minStart && marker.start() <= maxStart) {
                filtered.add(marker);
            }
        }
        return filtered;
    }

    private ParsedQuestion parseBlock(int number, String rawBlock) {
        String body = rawBlock;
        Matcher questionPrefix = Pattern.compile("(?is)^\\s*(?:quest(?:ão|ao)\\s*)?\\d{1,3}(?:\\s*[\\.\\)\\-:]\\s*|\\s*)").matcher(body);
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
        if (enunciado.length() < 5) return null;

        List<String> optionTexts = new ArrayList<>();
        for (int i = 0; i < alternatives.size(); i++) {
            AlternativeMarker current = alternatives.get(i);
            int end = i + 1 < alternatives.size() ? alternatives.get(i + 1).start() : body.length();
            String option = cleanContent(body.substring(current.contentStart(), end));
            if (!option.isBlank()) optionTexts.add(option);
        }

        if (optionTexts.size() < 2) return null;

        int pageStart = -1;
        int pageEnd = -1;

        return new ParsedQuestion(
                number,
                enunciado,
                List.copyOf(optionTexts),
                rawBlock.strip(),
                pageStart,
                pageEnd
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
        return value.replace(PAGE_BREAK, "\n")
                .replaceAll("(?m)^\\s*Página\\s+\\d+\\s*$", "")
                .replaceAll("(?m)^\\s*Page\\s+\\d+\\s*$", "")
                .strip();
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

    private List<Integer> findDuplicates(List<Marker> markers) {
        Set<Integer> seen = new HashSet<>();
        Set<Integer> duplicates = new TreeSet<>();
        for (Marker marker : markers) {
            if (!seen.add(marker.number())) duplicates.add(marker.number());
        }
        return List.copyOf(duplicates);
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
