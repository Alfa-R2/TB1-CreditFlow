package edu.upc.sistemas.tbcreditflow.security.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Generación y validación de JWT (HS256) — HU12.
 * El token lleva el {@code username} como subject y el rol en el claim {@code rol}.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMillis;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expirationMillis) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationMillis;
    }

    public String generateToken(String username, String rol) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(username)
                .claim("rol", rol)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMillis))
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return parse(token).getSubject();
    }

    public String extractRol(String token) {
        return parse(token).get("rol", String.class);
    }

    /** Devuelve true si el token es válido (firma correcta y no expirado). */
    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
