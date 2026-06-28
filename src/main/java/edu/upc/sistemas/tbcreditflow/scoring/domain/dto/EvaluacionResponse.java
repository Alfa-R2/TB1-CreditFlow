package edu.upc.sistemas.tbcreditflow.scoring.domain.dto;

import edu.upc.sistemas.tbcreditflow.scoring.domain.entity.EvaluacionRiesgo;
import edu.upc.sistemas.tbcreditflow.scoring.domain.NivelRiesgo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EvaluacionResponse(
        Long id,
        Long solicitudId,
        BigDecimal capacidadPago,
        Integer score,
        NivelRiesgo nivelRiesgo,
        String justificacion,
        LocalDateTime fecha
) {
    public static EvaluacionResponse from(EvaluacionRiesgo e) {
        return new EvaluacionResponse(
                e.getId(),
                e.getSolicitudId(),
                e.getCapacidadPago(),
                e.getScore(),
                e.getNivelRiesgo(),
                e.getJustificacion(),
                e.getFecha()
        );
    }
}
