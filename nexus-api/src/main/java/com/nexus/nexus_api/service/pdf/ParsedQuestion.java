package com.nexus.nexus_api.service.pdf;

import java.util.List;

/**
 * Representação intermediária de uma questão extraída deterministicamente do PDF.
 * Não depende de IA e não é persistida diretamente.
 */
public record ParsedQuestion(
        Integer numero,
        String enunciado,
        List<String> alternativas,
        String rawText,
        int paginaInicial,
        int paginaFinal
) {
}
