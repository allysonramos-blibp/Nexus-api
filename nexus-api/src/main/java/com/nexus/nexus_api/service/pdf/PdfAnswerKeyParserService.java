package com.nexus.nexus_api.service.pdf;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser determinístico de gabaritos oficiais em PDF.
 *
 * Reconhece tabelas, listas e grades com formatos comuns de bancas:
 * 1 A  |  1. A  |  1) A  |  01 - A  |  1: A
 * e também questões anuladas:
 * 17 *  |  17 X  |  17 #  |  17 ANULADA
 *
 * O gabarito é associado SEMPRE pelo número da questão, nunca pelo índice da lista.
 */
@Slf4j
@Service
public class PdfAnswerKeyParserService {

    // Padrão 1: linha com "1 A" ou "1 - A" ou "1. A" ou "Questão 1: A" ou "17 *"
    private static final Pattern PAIR_PATTERN = Pattern.compile(
            "(?i)(?:quest[aã]o\\s*)?(\\d{1,3})\\s*(?:[\\.\\)\\-:]\\s*)?\\s*([A-E]|\\*|X|#|ANULADA)",
            Pattern.CASE_INSENSITIVE
    );

    // Padrão 2: linha de cabeçalho de números seguida de linha de letras (comum em gabaritos FGV / Cespe)
    // Ex: 1 2 3 4 5 6 7 8 9 10
    //     A B C D E A B C D E

    public AnswerKeyParseResult parse(String text) {
        if (text == null || text.isBlank()) {
            return new AnswerKeyParseResult(Map.of(), List.of(), 0, 0, List.of());
        }

        Map<Integer, String> respostas = new TreeMap<>();
        Set<Integer> anuladas = new TreeSet<>();
        Set<Integer> duplicados = new TreeSet<>();

        // 1. Tentar detectar tabelas compactas estilo FGV (linha de números seguida de linha de letras)
        boolean detectedTable = parseTableGrid(text, respostas, anuladas, duplicados);

        // 2. Se encontrou poucos resultados ou nenhum por grid, roda varredura por pares
        if (respostas.size() < 10) {
            parsePairs(text, respostas, anuladas, duplicados);
        }

        return new AnswerKeyParseResult(
                respostas,
                new ArrayList<>(anuladas),
                respostas.size() + anuladas.size(),
                anuladas.size(),
                new ArrayList<>(duplicados)
        );
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
                    i++; // Pula a linha de resposta consumida
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
