package edu.upc.sistemas.tbcreditflow.common;

/**
 * Recurso no encontrado ⇒ HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
