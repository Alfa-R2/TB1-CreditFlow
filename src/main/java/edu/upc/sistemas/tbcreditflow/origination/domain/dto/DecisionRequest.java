package edu.upc.sistemas.tbcreditflow.origination.domain.dto;

import edu.upc.sistemas.tbcreditflow.origination.domain.AccionDecision;
import jakarta.validation.constraints.NotNull;

/** Cuerpo de {@code POST /api/solicitudes/{id}/decision} (§5). */
public record DecisionRequest(
        @NotNull AccionDecision accion
) {
}
