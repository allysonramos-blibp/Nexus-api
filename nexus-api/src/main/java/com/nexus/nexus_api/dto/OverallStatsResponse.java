package com.nexus.nexus_api.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record OverallStatsResponse(
        long questoesRespondidas,
        long acertos,
        long erros,
        BigDecimal percentualAcerto
) {
    public static OverallStatsResponse of(long respondidas, long acertos) {
        long erros = respondidas - acertos;
        BigDecimal percentual = respondidas == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(acertos)
                    .divide(BigDecimal.valueOf(respondidas), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        return new OverallStatsResponse(respondidas, acertos, erros, percentual);
    }
}
