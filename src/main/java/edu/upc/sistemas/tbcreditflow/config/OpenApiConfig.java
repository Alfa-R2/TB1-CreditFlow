package edu.upc.sistemas.tbcreditflow.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadatos de la API y esquema de seguridad para Swagger UI (springdoc). Declara un esquema Bearer
 * JWT global, de modo que el botón "Authorize" permita pegar el token obtenido en
 * {@code POST /api/auth/login} y probar los endpoints protegidos.
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_JWT = "bearer-jwt";

    @Bean
    public OpenAPI crediFlowOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CrediFlow API")
                        .description("""
                                API REST de gestión de solicitudes de crédito.
                                Autenticación JWT: obtén un token en POST /api/auth/login y pégalo en
                                el botón Authorize (formato: solo el token, sin el prefijo 'Bearer').""")
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT))
                .components(new Components().addSecuritySchemes(ESQUEMA_JWT,
                        new SecurityScheme()
                                .name(ESQUEMA_JWT)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
