package edu.upc.sistemas.tbcreditflow.scoring;

import edu.upc.sistemas.tbcreditflow.origination.domain.entity.Solicitud;
import edu.upc.sistemas.tbcreditflow.scoring.service.ScoringService;
import edu.upc.sistemas.tbcreditflow.security.domain.RolNombre;
import edu.upc.sistemas.tbcreditflow.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU05/HU06 — evaluación de riesgo vía API. */
class ScoringIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ScoringService scoringService;

    @Test
    void evaluar_casoMedio_200() throws Exception {
        Solicitud s = crearSolicitud(new BigDecimal("4200"), new BigDecimal("1750"),
                new BigDecimal("30000"), 24);

        mockMvc.perform(post("/api/solicitudes/{id}/evaluacion", s.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ANALISTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nivelRiesgo").value("MEDIO"))
                .andExpect(jsonPath("$.score").value(45))
                .andExpect(jsonPath("$.capacidadPago").value(2450.00));
    }

    @Test
    void evaluar_ingresoCero_400() throws Exception {
        Solicitud s = crearSolicitud(new BigDecimal("0"), new BigDecimal("0"),
                new BigDecimal("10000"), 12);

        mockMvc.perform(post("/api/solicitudes/{id}/evaluacion", s.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ANALISTA)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void evaluar_estadoNoRegistrada_409() throws Exception {
        Solicitud s = crearSolicitud(new BigDecimal("4200"), new BigDecimal("1750"),
                new BigDecimal("30000"), 24);
        scoringService.evaluar(s.getId()); // pasa a EVALUADA

        mockMvc.perform(post("/api/solicitudes/{id}/evaluacion", s.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ANALISTA)))
                .andExpect(status().isConflict());
    }

    @Test
    void evaluar_solicitudInexistente_404() throws Exception {
        mockMvc.perform(post("/api/solicitudes/{id}/evaluacion", 999999)
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ANALISTA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void obtenerEvaluacion_200() throws Exception {
        Solicitud s = crearSolicitud(new BigDecimal("4200"), new BigDecimal("1750"),
                new BigDecimal("30000"), 24);
        scoringService.evaluar(s.getId());

        mockMvc.perform(get("/api/solicitudes/{id}/evaluacion", s.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.COMITE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nivelRiesgo").value("MEDIO"));
    }

    @Test
    void obtenerEvaluacion_sinEvaluar_404() throws Exception {
        Solicitud s = crearSolicitud(new BigDecimal("4200"), new BigDecimal("1750"),
                new BigDecimal("30000"), 24);

        mockMvc.perform(get("/api/solicitudes/{id}/evaluacion", s.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ANALISTA)))
                .andExpect(status().isNotFound());
    }
}
