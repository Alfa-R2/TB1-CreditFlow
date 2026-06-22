package edu.upc.sistemas.tbcreditflow.origination.domain;

/**
 * Ciclo de vida de la Solicitud (§4.1):
 * REGISTRADA → EVALUADA → APROBADA | RECHAZADA (estados terminales).
 */
public enum EstadoSolicitud {
    REGISTRADA,
    EVALUADA,
    APROBADA,
    RECHAZADA
}
