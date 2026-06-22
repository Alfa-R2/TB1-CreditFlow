package edu.upc.sistemas.tbcreditflow.origination.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** Cuerpo de {@code POST /api/solicitudes} (§5). */
public record CrearSolicitudRequest(
        @NotNull @Valid ClienteRequest cliente,
        @NotNull @Positive(message = "monto debe ser > 0") BigDecimal monto,
        @NotNull @Positive(message = "plazoMeses debe ser > 0") Integer plazoMeses
) {
}
