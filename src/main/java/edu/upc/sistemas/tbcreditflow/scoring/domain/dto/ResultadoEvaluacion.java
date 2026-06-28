package edu.upc.sistemas.tbcreditflow.scoring.domain.dto;

import edu.upc.sistemas.tbcreditflow.scoring.domain.NivelRiesgo;
import edu.upc.sistemas.tbcreditflow.scoring.domain.entity.EvaluacionRiesgo;

import java.math.BigDecimal;

/** Resultado puro del motor de scoring, antes de persistirse como {@link EvaluacionRiesgo}. */
public record ResultadoEvaluacion(
        BigDecimal capacidadPago,
        int score,
        NivelRiesgo nivelRiesgo,
        String justificacion
) {
}
