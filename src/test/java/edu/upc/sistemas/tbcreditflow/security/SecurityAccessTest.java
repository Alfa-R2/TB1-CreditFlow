package edu.upc.sistemas.tbcreditflow.security;

import edu.upc.sistemas.tbcreditflow.origination.domain.Solicitud;
import edu.upc.sistemas.tbcreditflow.security.domain.RolNombre;
import edu.upc.sistemas.tbcreditflow.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU12 / §6 — matriz de acceso por rol. CP06: acceso no autorizado ⇒ 403; sin token ⇒ 401. */
class SecurityAccessTest extends AbstractIntegrationTest {

    private static final String SOLICITUD_VALIDA = """
            {"cliente":{"tipoDoc":"DNI","numDoc":"55555555","nombres":"Luis","apellidos":"Diaz",
            "ingresoMensual":4200,"deudasActuales":1750},"monto":30000,"plazoMeses":24}
            """;

    @Test
    void cp06_rolNoAutorizado_403() throws Exception {
        // POST /api/solicitudes requiere ASESOR; un ANALISTA debe recibir 403
        mockMvc.perform(post("/api/solicitudes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ANALISTA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SOLICITUD_VALIDA))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinToken_401() throws Exception {
        mockMvc.perform(post("/api/solicitudes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SOLICITUD_VALIDA))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInvalido_401() throws Exception {
        mockMvc.perform(post("/api/solicitudes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-falso-invalido")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SOLICITUD_VALIDA))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void evaluacionRequiereAnalista_asesor403() throws Exception {
        Solicitud s = crearSolicitud(new BigDecimal("4200"), new BigDecimal("1750"),
                new BigDecimal("30000"), 24);

        mockMvc.perform(post("/api/solicitudes/{id}/evaluacion", s.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ASESOR)))
                .andExpect(status().isForbidden());
    }

    @Test
    void decisionRequiereComite_analista403() throws Exception {
        Solicitud s = crearSolicitud(new BigDecimal("4200"), new BigDecimal("1750"),
                new BigDecimal("30000"), 24);

        mockMvc.perform(post("/api/solicitudes/{id}/decision", s.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ANALISTA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accion":"APROBAR"}
                                """))
                .andExpect(status().isForbidden());
    }
}
