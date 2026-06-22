package edu.upc.sistemas.tbcreditflow.origination.domain;

import jakarta.validation.constraints.NotNull;

/** Cuerpo de {@code POST /api/solicitudes/{id}/decision} (§5). */
public record DecisionRequest(
        @NotNull AccionDecision accion
) {
}
