package com.nexus.nexus_api.service.pdf;

import java.util.List;

/**
 * Representação intermediária de uma questão extraída do PDF (determinística ou multimodal).
 * Não depende de banco de dados e não é persistida diretamente.
 */
public record ParsedQuestion(
        Integer numero,
        String enunciado,
        List<String> alternativas,
        String rawText,
        int paginaInicial,
        int paginaFinal,
        String disciplinaSugerida
) {
    public ParsedQuestion(Integer numero, String enunciado, List<String> alternativas, String rawText, int paginaInicial, int paginaFinal) {
        this(numero, enunciado, alternativas, rawText, paginaInicial, paginaFinal, null);
    }
}
