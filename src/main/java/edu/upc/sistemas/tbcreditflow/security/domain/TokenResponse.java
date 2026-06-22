package edu.upc.sistemas.tbcreditflow.security.domain;

/**
 * Respuesta del login: {@code { "token": "...", "rol": "ASESOR" }}.
 */
public record TokenResponse(
        String token,
        String rol
) {
}
