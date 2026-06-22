package edu.upc.sistemas.tbcreditflow.reporting;

import edu.upc.sistemas.tbcreditflow.security.domain.RolNombre;
import edu.upc.sistemas.tbcreditflow.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU13 — indicadores (solo GERENTE). */
class ReporteIntegrationTest extends AbstractIntegrationTest {

    private static final String SOLICITUD_VALIDA = """
            {"cliente":{"tipoDoc":"DNI","numDoc":"33333333","nombres":"Eva","apellidos":"Ruiz",
            "ingresoMensual":4200,"deudasActuales":1750},"monto":30000,"plazoMeses":24}
            """;

    @Test
    void indicadores_reflejaFlujoCompleto() throws Exception {
        // registrar → evaluar → aprobar (una solicitud, perfil MEDIO)
        MvcResult creada = mockMvc.perform(post("/api/solicitudes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ASESOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SOLICITUD_VALIDA))
                .andReturn();
        long id = extraerId(creada);

        mockMvc.perform(post("/api/solicitudes/{id}/evaluacion", id)
                .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ANALISTA)));
        mockMvc.perform(post("/api/solicitudes/{id}/decision", id)
                .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.COMITE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"accion":"APROBAR"}
                        """));

        mockMvc.perform(get("/api/reportes/indicadores")
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.GERENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSolicitudes").value(1))
                .andExpect(jsonPath("$.aprobadas").value(1))
                .andExpect(jsonPath("$.rechazadas").value(0))
                .andExpect(jsonPath("$.porcentajeAprobacion").value(100.00))
                .andExpect(jsonPath("$.distribucionRiesgo.MEDIO").value(1))
                .andExpect(jsonPath("$.distribucionRiesgo.BAJO").value(0))
                .andExpect(jsonPath("$.distribucionRiesgo.ALTO").value(0))
                .andExpect(jsonPath("$.tiempoPromedioDias").exists());
    }

    @Test
    void rolNoAutorizado_403() throws Exception {
        mockMvc.perform(get("/api/reportes/indicadores")
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ASESOR)))
                .andExpect(status().isForbidden());
    }
}
