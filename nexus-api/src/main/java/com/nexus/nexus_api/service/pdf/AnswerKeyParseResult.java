package com.nexus.nexus_api.service.pdf;

import java.util.List;
import java.util.Map;

public record AnswerKeyParseResult(
        Map<Integer, String> respostasPorNumero,
        List<Integer> anuladas,
        int totalEncontrado,
        int totalAnuladas,
        List<Integer> numerosDuplicados
) {}
