package edu.upc.sistemas.tbcreditflow.scoring.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Cuerpo de creación/actualización de una ReglaScoring (HU07). Sin {@code id}. */
public record ReglaScoringRequest(
        @NotBlank String nombre,
        @NotBlank String parametro,
        @NotNull OperadorRegla operador,
        @NotNull BigDecimal umbral,
        @NotNull Integer ponderacion,
        @NotNull Boolean activa
) {
}
