package edu.upc.sistemas.tbcreditflow.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utilidad de hashing SHA-256 sin dependencias externas (java.security.MessageDigest).
 * Se usa para el hash de contenido de documentos (§4.7) y el sellado encadenado de auditoría (§4.6).
 */
public final class HashUtil {

    private HashUtil() {
    }

    /** SHA-256 de una cadena de texto (UTF-8) devuelto como hex de 64 caracteres. */
    public static String sha256(String input) {
        return sha256(input.getBytes(StandardCharsets.UTF_8));
    }

    /** SHA-256 del contenido binario devuelto como hex de 64 caracteres. */
    public static String sha256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 es parte del JDK; este caso no debería ocurrir nunca.
            throw new IllegalStateException("Algoritmo SHA-256 no disponible", e);
        }
    }
}
