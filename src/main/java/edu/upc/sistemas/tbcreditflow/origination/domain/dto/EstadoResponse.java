package edu.upc.sistemas.tbcreditflow.origination.domain.dto;

import edu.upc.sistemas.tbcreditflow.origination.domain.EstadoSolicitud;

/** Respuesta {@code { "estado": "..." }} de la decisión y de la consulta de estado. */
public record EstadoResponse(
        EstadoSolicitud estado
) {
}
