package com.nexus.nexus_api.dto;

import java.util.List;

public record AnswerKeyImportResponse(
        int totalEncontrado,
        int totalAtualizado,
        List<Integer> questoesSemCorrespondencia,
        List<Integer> numerosAusentesNoGabarito,
        List<Integer> numerosAnulados,
        List<Integer> numerosDuplicadosIgnorados
) {}
