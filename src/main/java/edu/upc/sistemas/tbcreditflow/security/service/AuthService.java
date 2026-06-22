package edu.upc.sistemas.tbcreditflow.security.service;

import edu.upc.sistemas.tbcreditflow.security.domain.EstadoUsuario;
import edu.upc.sistemas.tbcreditflow.security.domain.LoginRequest;
import edu.upc.sistemas.tbcreditflow.security.domain.TokenResponse;
import edu.upc.sistemas.tbcreditflow.security.domain.Usuario;
import edu.upc.sistemas.tbcreditflow.security.repository.UsuarioRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Autenticación por usuario/contraseña (BCrypt) y emisión del JWT — HU12.
 * Credenciales inválidas ⇒ {@link BadCredentialsException} ⇒ HTTP 401.
 */
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public TokenResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

        if (usuario.getEstado() != EstadoUsuario.ACTIVO
                || !passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        String rol = usuario.getRol().getNombre().name();
        String token = jwtService.generateToken(usuario.getUsername(), rol);
        return new TokenResponse(token, rol);
    }
}
