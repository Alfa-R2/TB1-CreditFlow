package edu.upc.sistemas.tbcreditflow.origination;

import edu.upc.sistemas.tbcreditflow.origination.domain.Solicitud;
import edu.upc.sistemas.tbcreditflow.security.domain.RolNombre;
import edu.upc.sistemas.tbcreditflow.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU02 — adjuntar documentos (PDF/JPG, hash SHA-256; formato inválido ⇒ 400). */
class DocumentoIntegrationTest extends AbstractIntegrationTest {

    @Test
    void subirPdf_201ConHash() throws Exception {
        Solicitud s = crearSolicitud(new BigDecimal("4200"), new BigDecimal("1750"),
                new BigDecimal("30000"), 24);
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "boleta.pdf", "application/pdf", "contenido-pdf".getBytes());

        mockMvc.perform(multipart("/api/solicitudes/{id}/documentos", s.getId())
                        .file(archivo)
                        .param("tipo", "BOLETA_PAGO")
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ASESOR)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("BOLETA_PAGO"))
                .andExpect(jsonPath("$.hash").isNotEmpty());
    }

    @Test
    void subirFormatoInvalido_400() throws Exception {
        Solicitud s = crearSolicitud(new BigDecimal("4200"), new BigDecimal("1750"),
                new BigDecimal("30000"), 24);
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "nota.txt", "text/plain", "texto".getBytes());

        mockMvc.perform(multipart("/api/solicitudes/{id}/documentos", s.getId())
                        .file(archivo)
                        .param("tipo", "BOLETA_PAGO")
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ASESOR)))
                .andExpect(status().isBadRequest());
    }
}
