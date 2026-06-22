package edu.upc.sistemas.tbcreditflow.origination.domain;

/** Respuesta {@code { "estado": "..." }} de la decisión y de la consulta de estado. */
public record EstadoResponse(
        EstadoSolicitud estado
) {
}
