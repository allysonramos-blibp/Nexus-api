package com.nexus.nexus_api.service;

import com.nexus.nexus_api.dto.OverallStatsResponse;
import com.nexus.nexus_api.dto.SubjectPerformanceDto;
import com.nexus.nexus_api.dto.TopicPerformanceDto;
import com.nexus.nexus_api.repository.AnswerRepository;
import com.nexus.nexus_api.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final AnswerRepository answerRepository;

    public OverallStatsResponse overall() {
        Long userId = SecurityUtils.getCurrentUserId();
        long respondidas = answerRepository.countByUserId(userId);
        long acertos = answerRepository.countByUserIdAndCorretaTrue(userId);
        return OverallStatsResponse.of(respondidas, acertos);
    }

    public List<SubjectPerformanceDto> porMateria() {
        return answerRepository.findPerformanceBySubject(SecurityUtils.getCurrentUserId());
    }

    public List<TopicPerformanceDto> porAssunto() {
        return answerRepository.findPerformanceByTopic(SecurityUtils.getCurrentUserId());
    }

    public OverallStatsResponse porPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null) {
            throw new IllegalArgumentException("Informe as datas de início e fim do período.");
        }
        if (fim.isBefore(inicio)) {
            throw new IllegalArgumentException("A data de fim não pode ser anterior à data de início.");
        }

        Long userId = SecurityUtils.getCurrentUserId();
        LocalDateTime inicioDateTime = inicio.atStartOfDay();
        LocalDateTime fimDateTime = fim.atTime(LocalTime.MAX);

        long respondidas = answerRepository.countByUserIdAndRespondidoEmBetween(userId, inicioDateTime, fimDateTime);
        long acertos = answerRepository.countByUserIdAndCorretaTrueAndRespondidoEmBetween(userId, inicioDateTime, fimDateTime);

        return OverallStatsResponse.of(respondidas, acertos);
    }
}
