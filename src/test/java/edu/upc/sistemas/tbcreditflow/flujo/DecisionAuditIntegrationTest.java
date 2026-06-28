package edu.upc.sistemas.tbcreditflow.flujo;

import edu.upc.sistemas.tbcreditflow.audit.domain.AccionAuditoria;
import edu.upc.sistemas.tbcreditflow.audit.domain.entity.RegistroAuditoria;
import edu.upc.sistemas.tbcreditflow.audit.repository.RegistroAuditoriaRepository;
import edu.upc.sistemas.tbcreditflow.audit.service.AuditService;
import edu.upc.sistemas.tbcreditflow.origination.domain.entity.Solicitud;
import edu.upc.sistemas.tbcreditflow.scoring.service.ScoringService;
import edu.upc.sistemas.tbcreditflow.security.domain.RolNombre;
import edu.upc.sistemas.tbcreditflow.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU08/HU09/HU11 — decisión del comité, transición terminal y auditoría encadenada. CP05. */
class DecisionAuditIntegrationTest extends AbstractIntegrationTest {

    private static final String SOLICITUD_VALIDA = """
            {"cliente":{"tipoDoc":"DNI","numDoc":"77777777","nombres":"Ana","apellidos":"Lopez",
            "ingresoMensual":4200,"deudasActuales":1750},"monto":30000,"plazoMeses":24}
            """;

    @Autowired
    private ScoringService scoringService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private RegistroAuditoriaRepository registroAuditoriaRepository;

    @Test
    void e2e_registrarEvaluarDecidirAuditar() throws Exception {
        // 1. Registrar (ASESOR)
        MvcResult creada = mockMvc.perform(post("/api/solicitudes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ASESOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SOLICITUD_VALIDA))
                .andExpect(status().isCreated())
                .andReturn();
        long id = extraerId(creada);

        // 2. Evaluar (ANALISTA) → MEDIO
        mockMvc.perform(post("/api/solicitudes/{id}/evaluacion", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ANALISTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nivelRiesgo").value("MEDIO"));

        // 3. Decidir (COMITE) → APROBADA
        mockMvc.perform(post("/api/solicitudes/{id}/decision", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.COMITE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accion":"APROBAR"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADA"));

        // 4. Auditoría: registro presente, con hash y cadena íntegra
        RegistroAuditoria registro = registroAuditoriaRepository.findAll().stream()
                .filter(r -> r.getSolicitudId().equals(id))
                .findFirst()
                .orElseThrow();
        assertThat(registro.getAccion()).isEqualTo(AccionAuditoria.APROBADA);
        assertThat(registro.getUsuario()).isEqualTo("user_COMITE");
        assertThat(registro.getHashIntegridad()).hasSize(64);
        assertThat(auditService.verificarCadena()).isTrue();
    }

    @Test
    void cp05_decisionGeneraRegistroInmutableConHash() throws Exception {
        Solicitud s = crearSolicitud(new BigDecimal("4200"), new BigDecimal("1750"),
                new BigDecimal("30000"), 24);
        scoringService.evaluar(s.getId()); // EVALUADA

        mockMvc.perform(post("/api/solicitudes/{id}/decision", s.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.COMITE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accion":"RECHAZAR"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADA"));

        List<RegistroAuditoria> registros = registroAuditoriaRepository.findAll().stream()
                .filter(r -> r.getSolicitudId().equals(s.getId()))
                .toList();
        assertThat(registros).hasSize(1);
        assertThat(registros.get(0).getAccion()).isEqualTo(AccionAuditoria.RECHAZADA);
        assertThat(registros.get(0).getHashIntegridad()).hasSize(64);
        assertThat(auditService.verificarCadena()).isTrue();
    }

    @Test
    void decision_solicitudNoEvaluada_409() throws Exception {
        Solicitud s = crearSolicitud(new BigDecimal("4200"), new BigDecimal("1750"),
                new BigDecimal("30000"), 24); // sigue REGISTRADA

        mockMvc.perform(post("/api/solicitudes/{id}/decision", s.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.COMITE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accion":"APROBAR"}
                                """))
                .andExpect(status().isConflict());
    }
}
