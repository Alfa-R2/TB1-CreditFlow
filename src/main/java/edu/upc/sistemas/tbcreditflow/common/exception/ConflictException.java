package edu.upc.sistemas.tbcreditflow.common.exception;

/**
 * Transición de estado inválida / intento de modificar un registro inmutable ⇒ HTTP 409.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
