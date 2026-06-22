package edu.upc.sistemas.tbcreditflow.reporting.domain;

import java.math.BigDecimal;
import java.util.Map;

/** Indicadores del tablero de gerencia (HU13). */
public record ReporteIndicadores(
        long totalSolicitudes,
        long aprobadas,
        long rechazadas,
        BigDecimal porcentajeAprobacion,
        BigDecimal tiempoPromedioDias,
        Map<String, Long> distribucionRiesgo
) {
}
