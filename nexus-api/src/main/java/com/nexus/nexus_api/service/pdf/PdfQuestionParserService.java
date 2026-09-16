package com.nexus.nexus_api.service.pdf;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser determinístico de questões a partir de texto de PDF.
 *
 * Suporta:
 * - Provas de concursos padrão (FGV, Cespe, FCC, Vunesp).
 * - Provas acadêmicas/universitárias divididas por blocos de matérias/disciplinas (ex: UniRV, Medicina).
 * - Questões com 4 alternativas (a, b, c, d) ou 5 alternativas (a, b, c, d, e), maiúsculas ou minúsculas.
 * - Questões dissertativas/discursivas identificadas no corpo ou título.
 */
@Slf4j
@Service
public class PdfQuestionParserService {

    private static final String PAGE_BREAK = "[[NEXUS_PAGE_BREAK]]";

    // Padrão de início de questão: "1", "01", "1.", "1)", "1 -", "Questão 1", "QUESTÃO 01", "QUESTÃO 05 - Dissertativa"
    private static final Pattern QUESTION_START = Pattern.compile(
            "(?im)^\\s*(?:quest(?:ão|ao)\\s*)?(\\d{1,3})\\s*(?:[\\.\\)\\-:]\\s*|\\s*-\\s*(?:dissertativa|discursiva)\\s*)?$"
    );

    // Alternativas com pontuação: "(A)", "(a)", "A)", "a)", "A.", "a.", "A -", "a -"
    private static final Pattern ALTERNATIVE_START = Pattern.compile(
            "(?i)^\\s*(?:\\(([A-E])\\)|([A-E])[\\)\\.\\-:])\\s*(.*)$"
    );

    // Alternativas separadas por múltiplos espaços (sem pontuação explícita)
    private static final Pattern ALTERNATIVE_WITHOUT_PUNCTUATION = Pattern.compile(
            "(?i)^\\s*([A-E])\\s{2,}(.*)$"
    );

    public PdfParseResult parse(String fullText, int totalPages) {
        if (fullText == null || fullText.isBlank()) {
            return new PdfParseResult(
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    0,
                    0,
                    false
            );
        }

        String cleanedText = preprocessAndCleanHeaders(fullText);
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
            ParsedQuestion question = parseBlock(current.number(), rawBlock, current.subject());

            if (question != null) {
                parsed.add(question);
                if (!parsedNumbers.add(question.numero())) {
                    duplicates.add(question.numero());
                }
            } else {
                invalid++;
            }
        }

        // Ordena mantendo a sequência de aparição ou número
        List<Integer> foundNumbers = parsed.stream()
                .map(ParsedQuestion::numero)
                .distinct()
                .sorted()
                .toList();

        List<Integer> missingNumbers = findMissingNumbers(foundNumbers);

        log.info("[PDF-PARSER] Resultado: {} questões extraídas, {} inválidas, {} ausentes, {} duplicadas",
                parsed.size(), invalid, missingNumbers, duplicates);

        return new PdfParseResult(
                List.copyOf(parsed),
                foundNumbers,
                missingNumbers,
                duplicates,
                totalPages,
                invalid,
                true
        );
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

            // Ignora marcas d'água de scanners de celular (CamScanner, etc)
            if (trimmed.matches("(?i).*digitalizado com camscanner.*") ||
                trimmed.matches("(?i).*scanned with camscanner.*")) {
                continue;
            }

            // Ignora linhas de rodapé com paginação explícita para evitar confundir com questão
            if (trimmed.matches("(?i).*P[AÁ]GINA\\s+\\d+.*") || trimmed.matches("(?i).*PAGE\\s+\\d+.*")) {
                continue;
            }

            // Remove repetições de cabeçalhos formais de provas
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
        String[] lines = text.split("\\n", -1);

        String currentSubject = null;
        int currentOffset = 0;

        for (String line : lines) {
            String trimmed = line.trim();

            // Verifica se a linha é um cabeçalho de matéria/disciplina
            if (isSubjectHeader(trimmed)) {
                currentSubject = trimmed;
            }

            Matcher matcher = QUESTION_START.matcher(line);
            if (matcher.matches()) {
                int number;
                try {
                    number = Integer.parseInt(matcher.group(1));
                } catch (NumberFormatException e) {
                    currentOffset += line.length() + 1;
                    continue;
                }

                if (number >= 1 && number <= 300) {
                    int markerStart = currentOffset + matcher.start(1);
                    int markerEnd = currentOffset + line.length();

                    String after = (markerEnd < text.length()) ? text.substring(markerEnd) : "";
                    if (hasPlausibleQuestionContent(after)) {
                        result.add(new Marker(number, currentOffset, markerEnd, currentSubject));
                    }
                }
            }

            currentOffset += line.length() + 1;
        }

        return result;
    }

    private boolean isSubjectHeader(String trimmed) {
        if (trimmed.length() < 3 || trimmed.length() > 50) return false;

        // Não é cabeçalho se for termo administrativo da prova
        if (trimmed.matches("(?i)^(?:quest(?:ão|ao)|p[aá]gina|page|nome|data|instruç|avaliaç|campus|universidade|faculdade|gabarito|caderno).*")) {
            return false;
        }

        // Deve conter letras e não terminar com pontuação de frase
        boolean hasLetters = trimmed.chars().anyMatch(Character::isLetter);
        boolean endsWithPunct = trimmed.endsWith(".") || trimmed.endsWith("?") || trimmed.endsWith(":") || trimmed.endsWith(",");

        if (!hasLetters || endsWithPunct) return false;

        // Padrão típico de cabeçalho: "FISIOLOGIA III", "FARMACOLOGIA I", "DIREITO PENAL", etc.
        return trimmed.matches("(?i)^[A-ZÁÉÍÓÚÂÊÔÃÕÇ\\s\\-\\dIVXLCDM]+$");
    }

    private boolean hasPlausibleQuestionContent(String after) {
        String sample = after.replace(PAGE_BREAK, " ").strip();
        if (sample.length() < 15) return false;

        int checkLimit = Math.min(sample.length(), 4000);
        String window = sample.substring(0, checkLimit);

        // Verifica se há alternativas (A-E ou a-e) ou indicação de dissertativa
        if (window.matches("(?is).*\\b(?:\\(?[A-Ea-e][\\)\\.\\-:]|[A-Ea-e]\\s{2,}).*")) {
            return true;
        }

        return window.matches("(?is).*\\b(?:dissertativa|discursiva|responder em linhas).*");
    }

    private ParsedQuestion parseBlock(int number, String rawBlock, String subject) {
        String body = rawBlock;

        Matcher questionPrefix = Pattern.compile("(?is)^\\s*(?:quest(?:ão|ao)\\s*)?" + number + "(?:\\s*[\\.\\)\\-:]\\s*|\\s*-\\s*(?:dissertativa|discursiva)\\s*|\\s*)").matcher(body);
        if (questionPrefix.find()) {
            body = body.substring(questionPrefix.end());
        }

        body = cleanBlock(body);
        if (body.isBlank()) return null;

        List<AlternativeMarker> alternatives = findAlternatives(body);

        // Se encontrou menos de 2 alternativas, verifica se é uma questão dissertativa
        if (alternatives.size() < 2) {
            String lower = rawBlock.toLowerCase();
            if (lower.contains("dissertativa") || lower.contains("discursiva") || lower.contains("responder em")) {
                String enunciadoDissertativa = cleanContent(body);
                if (enunciadoDissertativa.length() >= 5) {
                    return new ParsedQuestion(
                            number,
                            enunciadoDissertativa,
                            List.of("[Questão Dissertativa]"),
                            rawBlock.strip(),
                            -1,
                            -1,
                            subject
                    );
                }
            }
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
                -1,
                subject
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

    private record Marker(int number, int start, int end, String subject) {}
    private record AlternativeMarker(String label, int start, int contentStart) {}
}
