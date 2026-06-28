package edu.upc.sistemas.tbcreditflow.scoring;

import edu.upc.sistemas.tbcreditflow.scoring.domain.NivelRiesgo;
import edu.upc.sistemas.tbcreditflow.scoring.domain.OperadorRegla;
import edu.upc.sistemas.tbcreditflow.scoring.domain.entity.ReglaScoring;
import edu.upc.sistemas.tbcreditflow.scoring.domain.dto.ResultadoEvaluacion;
import edu.upc.sistemas.tbcreditflow.scoring.service.ScoringEngine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias del motor de scoring (HU05/HU06).
 * CP03 — cálculo de capacidad de pago. CP04 — nivel de riesgo alto.
 */
class ScoringEngineTest {

    private final ScoringEngine engine = new ScoringEngine();

    /** Reglas semilla del §4.4. */
    private List<ReglaScoring> reglasSemilla() {
        return List.of(
                new ReglaScoring("Endeudamiento alto", ScoringEngine.RATIO_ENDEUDAMIENTO,
                        OperadorRegla.GT, new BigDecimal("0.40"), -30, true),
                new ReglaScoring("Cuota alta sobre capacidad", ScoringEngine.RATIO_CUOTA,
                        OperadorRegla.GT, new BigDecimal("0.40"), -25, true),
                new ReglaScoring("Bajo endeudamiento", ScoringEngine.RATIO_ENDEUDAMIENTO,
                        OperadorRegla.LT, new BigDecimal("0.20"), 10, true),
                new ReglaScoring("Cuota muy alta sobre capacidad", ScoringEngine.RATIO_CUOTA,
                        OperadorRegla.GT, new BigDecimal("0.60"), -20, true));
    }

    @Test
    void cp03_calculaCapacidadDePago() {
        ResultadoEvaluacion r = engine.calcular(
                new BigDecimal("4200"), new BigDecimal("1750"),
                new BigDecimal("30000"), 24, List.of());

        // capacidadPago = ingreso - deudas = 4200 - 1750 = 2450
        assertThat(r.capacidadPago()).isEqualByComparingTo("2450.00");
    }

    @Test
    void verificacion_caso45Medio() {
        // §4.4: ratioEnd=0.42, ratioCuota=0.51 ⇒ 100−30−25 = 45 → MEDIO
        ResultadoEvaluacion r = engine.calcular(
                new BigDecimal("4200"), new BigDecimal("1750"),
                new BigDecimal("30000"), 24, reglasSemilla());

        assertThat(r.score()).isEqualTo(45);
        assertThat(r.nivelRiesgo()).isEqualTo(NivelRiesgo.MEDIO);
        assertThat(r.justificacion()).contains("Endeudamiento alto", "Cuota alta sobre capacidad");
    }

    @Test
    void cp04_perfilExtremoNivelAlto() {
        // Perfil extremo: ratioEnd=0.42, ratioCuota=0.65 ⇒ 100−30−25−20 = 25 → ALTO
        ResultadoEvaluacion r = engine.calcular(
                new BigDecimal("5000"), new BigDecimal("2100"),
                new BigDecimal("22620"), 12, reglasSemilla());

        assertThat(r.score()).isLessThan(40);
        assertThat(r.score()).isEqualTo(25);
        assertThat(r.nivelRiesgo()).isEqualTo(NivelRiesgo.ALTO);
    }

    @Test
    void capacidadNoPositiva_riesgoAltoScoreCero() {
        // §4.2: deudas >= ingreso ⇒ capacidad <= 0 ⇒ ALTO, score 0
        ResultadoEvaluacion r = engine.calcular(
                new BigDecimal("2000"), new BigDecimal("2500"),
                new BigDecimal("10000"), 12, reglasSemilla());

        assertThat(r.score()).isZero();
        assertThat(r.nivelRiesgo()).isEqualTo(NivelRiesgo.ALTO);
        assertThat(r.capacidadPago()).isEqualByComparingTo("-500.00");
    }

    @Test
    void perfilSano_nivelBajo() {
        // Bajo endeudamiento (ratioEnd < 0.20) ⇒ +10 ⇒ score 100 (cap) → BAJO
        ResultadoEvaluacion r = engine.calcular(
                new BigDecimal("8000"), new BigDecimal("800"),
                new BigDecimal("6000"), 48, reglasSemilla());

        assertThat(r.nivelRiesgo()).isEqualTo(NivelRiesgo.BAJO);
        assertThat(r.score()).isGreaterThanOrEqualTo(70);
    }
}
