package edu.upc.sistemas.tbcreditflow.config;

import edu.upc.sistemas.tbcreditflow.scoring.domain.OperadorRegla;
import edu.upc.sistemas.tbcreditflow.scoring.domain.ReglaScoring;
import edu.upc.sistemas.tbcreditflow.scoring.repository.ReglaScoringRepository;
import edu.upc.sistemas.tbcreditflow.scoring.service.ScoringEngine;
import edu.upc.sistemas.tbcreditflow.security.domain.EstadoUsuario;
import edu.upc.sistemas.tbcreditflow.security.domain.Rol;
import edu.upc.sistemas.tbcreditflow.security.domain.RolNombre;
import edu.upc.sistemas.tbcreditflow.security.domain.Usuario;
import edu.upc.sistemas.tbcreditflow.security.repository.RolRepository;
import edu.upc.sistemas.tbcreditflow.security.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Bootstrap de datos iniciales (§4.8). Idempotente: solo inserta lo que falta.
 * - 7 roles (RolNombre)
 * - 1 usuario admin (rol ADMIN_CREDITO, contraseña BCrypt) si no existe
 * - 4 reglas de scoring semilla (§4.4) si la tabla está vacía
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin";

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final ReglaScoringRepository reglaScoringRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(RolRepository rolRepository,
                      UsuarioRepository usuarioRepository,
                      ReglaScoringRepository reglaScoringRepository,
                      PasswordEncoder passwordEncoder) {
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.reglaScoringRepository = reglaScoringRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedRoles();
        seedAdmin();
        seedReglas();
    }

    private void seedRoles() {
        for (RolNombre nombre : RolNombre.values()) {
            if (rolRepository.findByNombre(nombre).isEmpty()) {
                rolRepository.save(new Rol(nombre, descripcionDe(nombre)));
            }
        }
    }

    private void seedAdmin() {
        if (usuarioRepository.existsByUsername(ADMIN_USERNAME)) {
            return;
        }
        Rol rolAdmin = rolRepository.findByNombre(RolNombre.ADMIN_CREDITO)
                .orElseThrow(() -> new IllegalStateException("Rol ADMIN_CREDITO no encontrado"));
        usuarioRepository.save(new Usuario(
                ADMIN_USERNAME,
                passwordEncoder.encode(ADMIN_PASSWORD),
                rolAdmin,
                EstadoUsuario.ACTIVO));
    }

    private void seedReglas() {
        if (reglaScoringRepository.count() > 0) {
            return;
        }
        reglaScoringRepository.save(new ReglaScoring(
                "Endeudamiento alto", ScoringEngine.RATIO_ENDEUDAMIENTO,
                OperadorRegla.GT, new BigDecimal("0.40"), -30, true));
        reglaScoringRepository.save(new ReglaScoring(
                "Cuota alta sobre capacidad", ScoringEngine.RATIO_CUOTA,
                OperadorRegla.GT, new BigDecimal("0.40"), -25, true));
        reglaScoringRepository.save(new ReglaScoring(
                "Bajo endeudamiento", ScoringEngine.RATIO_ENDEUDAMIENTO,
                OperadorRegla.LT, new BigDecimal("0.20"), 10, true));
        reglaScoringRepository.save(new ReglaScoring(
                "Cuota muy alta sobre capacidad", ScoringEngine.RATIO_CUOTA,
                OperadorRegla.GT, new BigDecimal("0.60"), -20, true));
    }

    private String descripcionDe(RolNombre nombre) {
        return switch (nombre) {
            case ASESOR -> "Registra solicitudes y adjunta documentos";
            case ANALISTA -> "Ejecuta la evaluación de riesgo";
            case ADMIN_CREDITO -> "Administra la parametrización de reglas";
            case COMITE -> "Aprueba o rechaza solicitudes";
            case CUMPLIMIENTO -> "Consulta la auditoría por cumplimiento";
            case AUDITOR -> "Consulta el historial de auditoría";
            case GERENTE -> "Consulta indicadores y reportes";
        };
    }
}
