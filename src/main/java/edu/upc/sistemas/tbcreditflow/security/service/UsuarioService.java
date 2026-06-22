package edu.upc.sistemas.tbcreditflow.security.service;

import edu.upc.sistemas.tbcreditflow.common.ResourceNotFoundException;
import edu.upc.sistemas.tbcreditflow.security.domain.Usuario;
import edu.upc.sistemas.tbcreditflow.security.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Acceso al usuario autenticado actual. Permite a los demás módulos resolver "quién hace la acción"
 * (asesor que registra una solicitud, miembro del comité que decide, etc.).
 */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /** Username del usuario autenticado en el contexto de seguridad actual. */
    public String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResourceNotFoundException("No hay usuario autenticado");
        }
        return auth.getName();
    }

    /** Usuario autenticado resuelto contra la base de datos. */
    public Usuario currentUsuario() {
        String username = currentUsername();
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));
    }
}
