package com.nexus.nexus_api.service.pdf;

import java.util.List;

public record PdfParseResult(
        List<ParsedQuestion> questoes,
        List<Integer> numerosEncontrados,
        List<Integer> numerosAusentes,
        List<Integer> numerosDuplicados,
        int paginasProcessadas,
        int questoesInvalidas,
        boolean possuiTexto
) {
}
