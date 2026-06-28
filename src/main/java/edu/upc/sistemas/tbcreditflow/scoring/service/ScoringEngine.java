package edu.upc.sistemas.tbcreditflow.scoring.service;

import edu.upc.sistemas.tbcreditflow.scoring.domain.NivelRiesgo;
import edu.upc.sistemas.tbcreditflow.scoring.domain.OperadorRegla;
import edu.upc.sistemas.tbcreditflow.scoring.domain.entity.ReglaScoring;
import edu.upc.sistemas.tbcreditflow.scoring.domain.dto.ResultadoEvaluacion;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Motor de scoring puro (sin dependencias de persistencia), unitariamente testeable.
 * Implementa el cálculo de capacidad de pago (§4.2) y el motor de reglas (§4.3).
 */
@Component
public class ScoringEngine {

    public static final String RATIO_ENDEUDAMIENTO = "ratioEndeudamiento";
    public static final String RATIO_CUOTA = "ratioCuota";

    private static final int RATIO_SCALE = 4;
    private static final int MONEY_SCALE = 2;

    public ResultadoEvaluacion calcular(BigDecimal ingresoMensual, BigDecimal deudasActuales,
                                        BigDecimal monto, int plazoMeses, List<ReglaScoring> reglas) {
        BigDecimal capacidadPago = ingresoMensual.subtract(deudasActuales).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        // §4.2: capacidad <= 0 ⇒ nivel ALTO, score 0; no se calcula ratioCuota.
        if (capacidadPago.compareTo(BigDecimal.ZERO) <= 0) {
            return new ResultadoEvaluacion(capacidadPago, 0, NivelRiesgo.ALTO,
                    "Capacidad de pago <= 0 (deudas >= ingreso): nivel ALTO, score 0");
        }

        BigDecimal cuotaEstimada = monto.divide(BigDecimal.valueOf(plazoMeses), MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal ratioEndeudamiento = deudasActuales.divide(ingresoMensual, RATIO_SCALE, RoundingMode.HALF_UP);
        BigDecimal ratioCuota = cuotaEstimada.divide(capacidadPago, RATIO_SCALE, RoundingMode.HALF_UP);

        int score = 100;
        StringBuilder justificacion = new StringBuilder();
        for (ReglaScoring regla : reglas) {
            if (!Boolean.TRUE.equals(regla.getActiva())) {
                continue;
            }
            BigDecimal valor = resolver(regla.getParametro(), ratioEndeudamiento, ratioCuota);
            if (valor == null) {
                continue; // parámetro desconocido: se ignora la regla
            }
            if (comparar(valor, regla.getOperador(), regla.getUmbral())) {
                score += regla.getPonderacion();
                String signo = regla.getPonderacion() >= 0 ? "+" : "";
                justificacion.append(regla.getNombre())
                        .append(" (").append(signo).append(regla.getPonderacion()).append("); ");
            }
        }
        score = Math.max(0, Math.min(100, score));

        String texto = justificacion.isEmpty() ? "Sin reglas aplicadas" : justificacion.toString().trim();
        return new ResultadoEvaluacion(capacidadPago, score, nivelDe(score), texto);
    }

    private BigDecimal resolver(String parametro, BigDecimal ratioEndeudamiento, BigDecimal ratioCuota) {
        if (RATIO_ENDEUDAMIENTO.equals(parametro)) {
            return ratioEndeudamiento;
        }
        if (RATIO_CUOTA.equals(parametro)) {
            return ratioCuota;
        }
        return null;
    }

    private boolean comparar(BigDecimal valor, OperadorRegla operador, BigDecimal umbral) {
        int cmp = valor.compareTo(umbral);
        return switch (operador) {
            case GT -> cmp > 0;
            case LT -> cmp < 0;
            case EQ -> cmp == 0;
        };
    }

    private NivelRiesgo nivelDe(int score) {
        if (score >= 70) {
            return NivelRiesgo.BAJO;
        }
        if (score >= 40) {
            return NivelRiesgo.MEDIO;
        }
        return NivelRiesgo.ALTO;
    }
}
