package com.nexus.nexus_api.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record TopicPerformanceDto(
        Long topicId,
        String topicNome,
        Long respondidas,
        Long acertos
) {
    public BigDecimal percentual() {
        if (respondidas == null || respondidas == 0) return BigDecimal.ZERO;
        long ac = acertos == null ? 0 : acertos;
        return BigDecimal.valueOf(ac)
                .divide(BigDecimal.valueOf(respondidas), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
