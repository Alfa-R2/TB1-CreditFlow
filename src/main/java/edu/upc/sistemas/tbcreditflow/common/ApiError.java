package edu.upc.sistemas.tbcreditflow.common;

import java.time.Instant;

/**
 * Cuerpo uniforme de error devuelto por toda la API (ver §7 de la especificación).
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
