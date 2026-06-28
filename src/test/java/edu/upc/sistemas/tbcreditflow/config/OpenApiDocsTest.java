package edu.upc.sistemas.tbcreditflow.config;

import edu.upc.sistemas.tbcreditflow.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Verifica que springdoc genera el OpenAPI y que la seguridad lo expone sin token. */
class OpenApiDocsTest extends AbstractIntegrationTest {

    @Test
    void apiDocs_esPublicoYDescribeLaApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs")) // sin Authorization
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("CrediFlow API"))
                .andExpect(jsonPath("$.components.securitySchemes.bearer-jwt.scheme").value("bearer"))
                .andExpect(content().string(containsString("/api/auth/login")));
    }
}
