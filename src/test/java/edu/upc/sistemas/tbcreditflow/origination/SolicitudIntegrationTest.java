package edu.upc.sistemas.tbcreditflow.origination;

import edu.upc.sistemas.tbcreditflow.security.domain.RolNombre;
import edu.upc.sistemas.tbcreditflow.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU01/HU03 — registro de solicitudes (create-or-reuse de Cliente y validación). CP01, CP02. */
class SolicitudIntegrationTest extends AbstractIntegrationTest {

    private static final String SOLICITUD_VALIDA = """
            {"cliente":{"tipoDoc":"DNI","numDoc":"12345678","nombres":"Juan","apellidos":"Perez",
            "ingresoMensual":4200,"deudasActuales":1750},"monto":30000,"plazoMeses":24}
            """;

    @Test
    void cp01_registroConDatosValidos_201Registrada() throws Exception {
        mockMvc.perform(post("/api/solicitudes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ASESOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SOLICITUD_VALIDA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.estado").value("REGISTRADA"))
                .andExpect(jsonPath("$.clienteId").isNumber());
    }

    @Test
    void cp02_registroConDatosIncompletos_400() throws Exception {
        String incompleta = """
                {"cliente":{"tipoDoc":"DNI","nombres":"Juan"},"plazoMeses":24}
                """;
        mockMvc.perform(post("/api/solicitudes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ASESOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(incompleta))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void registroReutilizaClientePorDocumento() throws Exception {
        MvcResult primera = mockMvc.perform(post("/api/solicitudes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ASESOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SOLICITUD_VALIDA))
                .andExpect(status().isCreated())
                .andReturn();
        MvcResult segunda = mockMvc.perform(post("/api/solicitudes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ASESOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SOLICITUD_VALIDA))
                .andExpect(status().isCreated())
                .andReturn();

        Number cliente1 = com.jayway.jsonpath.JsonPath.read(primera.getResponse().getContentAsString(), "$.clienteId");
        Number cliente2 = com.jayway.jsonpath.JsonPath.read(segunda.getResponse().getContentAsString(), "$.clienteId");
        assertThat(cliente1.longValue()).isEqualTo(cliente2.longValue());
    }

    @Test
    void obtenerPorId_200() throws Exception {
        MvcResult creada = mockMvc.perform(post("/api/solicitudes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ASESOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SOLICITUD_VALIDA))
                .andReturn();
        long id = extraerId(creada);

        mockMvc.perform(get("/api/solicitudes/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ASESOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) id))
                .andExpect(jsonPath("$.estado").value("REGISTRADA"));
    }

    @Test
    void obtenerInexistente_404() throws Exception {
        mockMvc.perform(get("/api/solicitudes/{id}", 999999)
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ASESOR)))
                .andExpect(status().isNotFound());
    }

    @Test
    void listar_200() throws Exception {
        mockMvc.perform(post("/api/solicitudes")
                .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ASESOR))
                .contentType(MediaType.APPLICATION_JSON)
                .content(SOLICITUD_VALIDA));

        mockMvc.perform(get("/api/solicitudes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ASESOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void hu04_consultarEstado_200() throws Exception {
        var s = crearSolicitud(new BigDecimal("4200"), new BigDecimal("1750"),
                new BigDecimal("30000"), 24);

        mockMvc.perform(get("/api/solicitudes/{id}/estado", s.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ASESOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("REGISTRADA"));
    }

    @Test
    void hu04_consultarEstado_404() throws Exception {
        mockMvc.perform(get("/api/solicitudes/{id}/estado", 999999)
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ASESOR)))
                .andExpect(status().isNotFound());
    }

    @Test
    void hu04_consultarEstado_rolNoAutorizado_403() throws Exception {
        var s = crearSolicitud(new BigDecimal("4200"), new BigDecimal("1750"),
                new BigDecimal("30000"), 24);

        mockMvc.perform(get("/api/solicitudes/{id}/estado", s.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.GERENTE)))
                .andExpect(status().isForbidden());
    }
}
