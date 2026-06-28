package edu.upc.sistemas.tbcreditflow.common.exception;

/**
 * Datos de entrada inválidos / faltantes (división por cero, formato de documento, etc.) ⇒ HTTP 400.
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
