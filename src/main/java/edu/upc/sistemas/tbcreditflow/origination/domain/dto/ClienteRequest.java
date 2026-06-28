package edu.upc.sistemas.tbcreditflow.origination.domain.dto;

import edu.upc.sistemas.tbcreditflow.origination.domain.TipoDoc;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Datos del cliente embebidos en la creación de una solicitud (§5). */
public record ClienteRequest(
        @NotNull TipoDoc tipoDoc,
        @NotBlank String numDoc,
        @NotBlank String nombres,
        @NotBlank String apellidos,
        @NotNull @DecimalMin(value = "0.0", message = "ingresoMensual debe ser >= 0") BigDecimal ingresoMensual,
        @NotNull @DecimalMin(value = "0.0", message = "deudasActuales debe ser >= 0") BigDecimal deudasActuales
) {
}
