package edu.upc.sistemas.tbcreditflow.support;

import com.jayway.jsonpath.JsonPath;
import edu.upc.sistemas.tbcreditflow.origination.domain.entity.Cliente;
import edu.upc.sistemas.tbcreditflow.origination.domain.entity.Solicitud;
import edu.upc.sistemas.tbcreditflow.origination.domain.TipoDoc;
import edu.upc.sistemas.tbcreditflow.origination.repository.ClienteRepository;
import edu.upc.sistemas.tbcreditflow.origination.repository.SolicitudRepository;
import edu.upc.sistemas.tbcreditflow.security.domain.EstadoUsuario;
import edu.upc.sistemas.tbcreditflow.security.domain.entity.Rol;
import edu.upc.sistemas.tbcreditflow.security.domain.RolNombre;
import edu.upc.sistemas.tbcreditflow.security.domain.entity.Usuario;
import edu.upc.sistemas.tbcreditflow.security.repository.RolRepository;
import edu.upc.sistemas.tbcreditflow.security.repository.UsuarioRepository;
import edu.upc.sistemas.tbcreditflow.security.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base de pruebas de integración: contexto Spring completo sobre H2 (perfil {@code test}),
 * MockMvc con la cadena de seguridad, y utilidades para tokens JWT y datos de prueba.
 * Cada test corre en una transacción que se revierte al terminar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class AbstractIntegrationTest {

    private static final AtomicInteger SECUENCIA = new AtomicInteger(0);

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UsuarioRepository usuarioRepository;

    @Autowired
    protected RolRepository rolRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected JwtService jwtService;

    @Autowired
    protected ClienteRepository clienteRepository;

    @Autowired
    protected SolicitudRepository solicitudRepository;

    /** Crea (si no existe) un usuario para el rol indicado y lo devuelve. */
    protected Usuario usuarioDeRol(RolNombre rolNombre) {
        String username = "user_" + rolNombre.name();
        return usuarioRepository.findByUsername(username).orElseGet(() -> {
            Rol rol = rolRepository.findByNombre(rolNombre).orElseThrow();
            return usuarioRepository.save(
                    new Usuario(username, passwordEncoder.encode("pass"), rol, EstadoUsuario.ACTIVO));
        });
    }

    /** Header Authorization con un JWT válido para el rol indicado. */
    protected String bearer(RolNombre rolNombre) {
        usuarioDeRol(rolNombre);
        return "Bearer " + jwtService.generateToken("user_" + rolNombre.name(), rolNombre.name());
    }

    /** Crea un cliente + solicitud en estado REGISTRADA directamente, para preparar escenarios. */
    protected Solicitud crearSolicitud(BigDecimal ingreso, BigDecimal deudas, BigDecimal monto, int plazoMeses) {
        String numDoc = "DOC" + SECUENCIA.incrementAndGet();
        Cliente cliente = clienteRepository.save(
                new Cliente(TipoDoc.DNI, numDoc, "Nombre", "Apellido", ingreso, deudas));
        Usuario asesor = usuarioDeRol(RolNombre.ASESOR);
        return solicitudRepository.save(
                new Solicitud(cliente, asesor, monto, plazoMeses, LocalDateTime.now()));
    }

    /** Extrae el campo numérico {@code $.id} del cuerpo JSON de una respuesta. */
    protected long extraerId(MvcResult resultado) throws Exception {
        Number id = JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }
}
