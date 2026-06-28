package edu.upc.sistemas.tbcreditflow.reporting;

import edu.upc.sistemas.tbcreditflow.audit.domain.AccionAuditoria;
import edu.upc.sistemas.tbcreditflow.audit.service.AuditService;
import edu.upc.sistemas.tbcreditflow.origination.domain.entity.Solicitud;
import edu.upc.sistemas.tbcreditflow.security.domain.RolNombre;
import edu.upc.sistemas.tbcreditflow.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU10 / CP07 — consulta del historial de auditoría filtrada por cliente y por fechas. */
class AuditoriaConsultaIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AuditService auditService;

    private Solicitud solicitudConDecision(AccionAuditoria accion, String usuario) {
        Solicitud s = crearSolicitud(new BigDecimal("4200"), new BigDecimal("1750"),
                new BigDecimal("30000"), 24);
        auditService.registrar(s.getId(), accion, usuario);
        return s;
    }

    @Test
    void sinFiltros_listaTodos() throws Exception {
        solicitudConDecision(AccionAuditoria.APROBADA, "u1");
        solicitudConDecision(AccionAuditoria.RECHAZADA, "u2");

        mockMvc.perform(get("/api/auditoria")
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.AUDITOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void cp07_filtraPorClienteYFechas() throws Exception {
        Solicitud s1 = solicitudConDecision(AccionAuditoria.APROBADA, "u1");
        solicitudConDecision(AccionAuditoria.RECHAZADA, "u2");

        // Solo el cliente de s1, dentro de un rango de fechas que incluye hoy
        mockMvc.perform(get("/api/auditoria")
                        .param("clienteId", String.valueOf(s1.getCliente().getId()))
                        .param("desde", "2000-01-01")
                        .param("hasta", "2999-12-31")
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.CUMPLIMIENTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].solicitudId").value(s1.getId().intValue()))
                .andExpect(jsonPath("$[0].accion").value("APROBADA"));
    }

    @Test
    void filtroFechaFutura_listaVacia() throws Exception {
        solicitudConDecision(AccionAuditoria.APROBADA, "u1");

        mockMvc.perform(get("/api/auditoria")
                        .param("desde", "2999-01-01")
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.AUDITOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void rolNoAutorizado_403() throws Exception {
        mockMvc.perform(get("/api/auditoria")
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ASESOR)))
                .andExpect(status().isForbidden());
    }
}
