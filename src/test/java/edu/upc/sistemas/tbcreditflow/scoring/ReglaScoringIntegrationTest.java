package edu.upc.sistemas.tbcreditflow.scoring;

import edu.upc.sistemas.tbcreditflow.security.domain.RolNombre;
import edu.upc.sistemas.tbcreditflow.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU07 — CRUD de reglas de scoring (solo ADMIN_CREDITO). */
class ReglaScoringIntegrationTest extends AbstractIntegrationTest {

    private static final String REGLA_VALIDA = """
            {"nombre":"Cuota media","parametro":"ratioCuota","operador":"GT",
            "umbral":0.30,"ponderacion":-10,"activa":true}
            """;

    @Test
    void listar_incluyeReglasSemilla() throws Exception {
        mockMvc.perform(get("/api/reglas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ADMIN_CREDITO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(4)));
    }

    @Test
    void crear_201() throws Exception {
        mockMvc.perform(post("/api/reglas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ADMIN_CREDITO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGLA_VALIDA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre").value("Cuota media"));
    }

    @Test
    void crear_parametroInvalido_400() throws Exception {
        String body = """
                {"nombre":"X","parametro":"otroParametro","operador":"GT",
                "umbral":0.30,"ponderacion":-10,"activa":true}
                """;
        mockMvc.perform(post("/api/reglas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ADMIN_CREDITO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crear_campoFaltante_400() throws Exception {
        String body = """
                {"nombre":"X","parametro":"ratioCuota","operador":"GT","activa":true}
                """; // falta umbral y ponderacion
        mockMvc.perform(post("/api/reglas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ADMIN_CREDITO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizar_200() throws Exception {
        MvcResult creada = mockMvc.perform(post("/api/reglas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ADMIN_CREDITO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGLA_VALIDA))
                .andReturn();
        long id = extraerId(creada);

        String actualizado = """
                {"nombre":"Cuota media (v2)","parametro":"ratioCuota","operador":"GT",
                "umbral":0.35,"ponderacion":-15,"activa":false}
                """;
        mockMvc.perform(put("/api/reglas/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ADMIN_CREDITO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actualizado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Cuota media (v2)"))
                .andExpect(jsonPath("$.ponderacion").value(-15))
                .andExpect(jsonPath("$.activa").value(false));
    }

    @Test
    void actualizar_inexistente_404() throws Exception {
        mockMvc.perform(put("/api/reglas/{id}", 999999)
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ADMIN_CREDITO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGLA_VALIDA))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminar_204() throws Exception {
        MvcResult creada = mockMvc.perform(post("/api/reglas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ADMIN_CREDITO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGLA_VALIDA))
                .andReturn();
        long id = extraerId(creada);

        mockMvc.perform(delete("/api/reglas/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ADMIN_CREDITO)))
                .andExpect(status().isNoContent());
    }

    @Test
    void eliminar_inexistente_404() throws Exception {
        mockMvc.perform(delete("/api/reglas/{id}", 999999)
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ADMIN_CREDITO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void rolNoAutorizado_403() throws Exception {
        mockMvc.perform(get("/api/reglas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ASESOR)))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearRegla_cambiaResultadoDeScoringPosterior() throws Exception {
        // Perfil estándar: con las 4 reglas semilla da score 45 / MEDIO.
        var s = crearSolicitud(new BigDecimal("4200"), new BigDecimal("1750"),
                new BigDecimal("30000"), 24);

        // ADMIN agrega una regla extra que penaliza ratioCuota > 0.40 con -30.
        String reglaExtra = """
                {"nombre":"Penalizacion cuota extra","parametro":"ratioCuota","operador":"GT",
                "umbral":0.40,"ponderacion":-30,"activa":true}
                """;
        mockMvc.perform(post("/api/reglas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ADMIN_CREDITO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reglaExtra))
                .andExpect(status().isCreated());

        // Ahora la evaluación baja a 15 (100-30-25-30) → ALTO.
        mockMvc.perform(post("/api/solicitudes/{id}/evaluacion", s.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ANALISTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(15))
                .andExpect(jsonPath("$.nivelRiesgo").value("ALTO"));
    }
}
