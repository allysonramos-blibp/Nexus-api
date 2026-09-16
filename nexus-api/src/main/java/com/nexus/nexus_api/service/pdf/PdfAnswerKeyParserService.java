package com.nexus.nexus_api.service.pdf;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser determinístico de gabaritos oficiais em PDF com suporte a múltiplos cargos/tipos de prova.
 *
 * Suporta:
 * 1. Filtragem inteligente por título/bloco de cargo ou tipo de prova (ex: "ATI - DESENVOLVIMENTO DE SOFTWARE - PROVA TIPO 3", "TIPO 3", "AMARELA").
 * 2. Tabelas compactas estilo FGV (linha de números 1..20 seguida de linha de letras A..E / *).
 * 3. Formato por pares chave-valor: "1 A", "01 - A", "1. A", "17 *".
 * 4. Listagem de seções encontradas no PDF caso o usuário queira escolher na interface.
 */
@Slf4j
@Service
public class PdfAnswerKeyParserService {

    private static final Pattern PAIR_PATTERN = Pattern.compile(
            "(?i)(?:quest[aã]o\\s*)?(\\d{1,3})\\s*(?:[\\.\\)\\-:]\\s*)?\\s*([A-E]|\\*|X|#|ANULADA)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Lista todos os títulos de cargos/cadernos de gabarito encontrados no documento.
     * Ex: "ATI - DESENVOLVIMENTO DE SOFTWARE – PROVA TIPO 3"
     */
    public List<String> listSections(String text) {
        if (text == null || text.isBlank()) return List.of();
        List<String> sections = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (isSectionHeader(trimmed)) {
                sections.add(trimmed);
            }
        }
        return sections;
    }

    public AnswerKeyParseResult parse(String text) {
        return parse(text, null);
    }

    public AnswerKeyParseResult parse(String text, String targetFilter) {
        if (text == null || text.isBlank()) {
            return new AnswerKeyParseResult(Map.of(), List.of(), 0, 0, List.of());
        }

        // Se foi especificado um filtro (ex: "TIPO 3", "DESENVOLVIMENTO DE SOFTWARE"),
        // isolamos apenas a região correspondente a esse bloco no PDF.
        String relevantText = isolateSection(text, targetFilter);

        Map<Integer, String> respostas = new TreeMap<>();
        Set<Integer> anuladas = new TreeSet<>();
        Set<Integer> duplicados = new TreeSet<>();

        // 1. Detecção de grade FGV (linha de números seguida de linha de respostas)
        boolean detectedTable = parseTableGrid(relevantText, respostas, anuladas, duplicados);

        // 2. Se encontrou poucos resultados ou nenhum por grid, varre por pares
        if (respostas.size() < 10) {
            parsePairs(relevantText, respostas, anuladas, duplicados);
        }

        return new AnswerKeyParseResult(
                respostas,
                new ArrayList<>(anuladas),
                respostas.size() + anuladas.size(),
                anuladas.size(),
                new ArrayList<>(duplicados)
        );
    }

    private boolean isSectionHeader(String line) {
        // Títulos no estilo: "ATI - DESENVOLVIMENTO DE SOFTWARE – PROVA TIPO 3" ou "CARGO X – PROVA TIPO 1"
        return line.matches("(?i).*PROVA\\s+TIPO\\s+\\d+.*") ||
               (line.matches("(?i).*(ANALISTA|TÉCNICO|TECNICO|ENGENHEIRO|MEDICO|MÉDICO|ATI|ADVOCA|CONTABIL).*") 
                && line.length() > 10 && line.length() < 120);
    }

    private String isolateSection(String text, String targetFilter) {
        if (targetFilter == null || targetFilter.isBlank()) {
            return text;
        }

        String filterNorm = targetFilter.trim().toLowerCase();
        String[] lines = text.split("\\r?\\n");
        StringBuilder sb = new StringBuilder();
        boolean insideTargetSection = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (isSectionHeader(line)) {
                // Checa se essa linha bate com o filtro desejado
                String lineLower = line.toLowerCase();
                if (matchesFilter(lineLower, filterNorm)) {
                    insideTargetSection = true;
                    sb.append(line).append("\n");
                    continue;
                } else if (insideTargetSection) {
                    // Começou outra seção diferente, encerra o bloco
                    break;
                }
            }

            if (insideTargetSection) {
                sb.append(lines[i]).append("\n");
            }
        }

        // Se o filtro encontrou o bloco, retorna apenas ele; caso contrário, usa o texto todo
        return sb.length() > 0 ? sb.toString() : text;
    }

    private boolean matchesFilter(String lineLower, String filterNorm) {
        // Ex: se filterNorm for "tipo 3", bate se contiver "tipo 3"
        // Se filterNorm tiver múltiplas palavras (ex: "desenvolvimento tipo 3"), checa todas
        String[] tokens = filterNorm.split("\\s+");
        for (String t : tokens) {
            if (!lineLower.contains(t)) {
                return false;
            }
        }
        return true;
    }

    private boolean parseTableGrid(String text, Map<Integer, String> respostas, Set<Integer> anuladas, Set<Integer> duplicados) {
        String[] lines = text.split("\\r?\\n");
        int found = 0;

        for (int i = 0; i < lines.length - 1; i++) {
            String lineNum = lines[i].trim();
            String lineAns = lines[i + 1].trim();

            String[] tokensNum = lineNum.split("\\s+");
            String[] tokensAns = lineAns.split("\\s+");

            if (tokensNum.length >= 5 && tokensAns.length >= 5 && tokensNum.length == tokensAns.length) {
                boolean isNumberRow = true;
                for (String t : tokensNum) {
                    if (!t.matches("\\d{1,3}")) {
                        isNumberRow = false;
                        break;
                    }
                }

                boolean isAnswerRow = true;
                for (String t : tokensAns) {
                    if (!t.matches("(?i)[A-E]|\\*|X|#|ANULADA")) {
                        isAnswerRow = false;
                        break;
                    }
                }

                if (isNumberRow && isAnswerRow) {
                    for (int k = 0; k < tokensNum.length; k++) {
                        int num = Integer.parseInt(tokensNum[k]);
                        String ans = tokensAns[k].toUpperCase();

                        if (respostas.containsKey(num) || anuladas.contains(num)) {
                            duplicados.add(num);
                        }

                        if (ans.equals("*") || ans.equals("X") || ans.equals("#") || ans.equals("ANULADA")) {
                            anuladas.add(num);
                        } else {
                            respostas.put(num, ans);
                        }
                        found++;
                    }
                    i++; // Pula a linha de respostas consumida
                }
            }
        }
        return found > 0;
    }

    private void parsePairs(String text, Map<Integer, String> respostas, Set<Integer> anuladas, Set<Integer> duplicados) {
        Matcher matcher = PAIR_PATTERN.matcher(text);
        while (matcher.find()) {
            try {
                int num = Integer.parseInt(matcher.group(1));
                if (num < 1 || num > 999) continue;

                String ans = matcher.group(2).toUpperCase();

                if (respostas.containsKey(num) || anuladas.contains(num)) {
                    duplicados.add(num);
                }

                if (ans.equals("*") || ans.equals("X") || ans.equals("#") || ans.equals("ANULADA")) {
                    anuladas.add(num);
                } else {
                    respostas.put(num, ans);
                }
            } catch (Exception ignored) {}
        }
    }
}
