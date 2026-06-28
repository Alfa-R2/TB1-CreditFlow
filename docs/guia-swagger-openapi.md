# Guia de Swagger / OpenAPI

Esta guia responde como se agrego Swagger al proyecto y que piezas intervienen
para que la documentacion de la API este disponible.

## 1. Idea central

El proyecto usa `springdoc-openapi`.

Swagger UI no se programo endpoint por endpoint. Se agrego una dependencia y
Springdoc escanea automaticamente los controllers de Spring MVC para generar el
documento OpenAPI.

Flujo general:

```text
pom.xml
  -> agrega springdoc-openapi

Spring Boot
  -> detecta springdoc en el classpath
  -> escanea @RestController y mappings HTTP
  -> genera /v3/api-docs
  -> sirve Swagger UI en /swagger-ui.html
```

## 2. Dependencia en Maven

Archivo:

```text
pom.xml
```

Se agrego la version como propiedad:

```xml
<springdoc.version>3.0.3</springdoc.version>
```

Y esta dependencia:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>${springdoc.version}</version>
</dependency>
```

El nombre `webmvc-ui` importa porque este backend usa Spring MVC:

```xml
<artifactId>spring-boot-starter-webmvc</artifactId>
```

No es WebFlux.

## 3. Configuracion propia de OpenAPI

Archivo:

```text
src/main/java/edu/upc/sistemas/tbcreditflow/config/OpenApiConfig.java
```

La clase esta marcada como configuracion:

```java
@Configuration
public class OpenApiConfig {
```

Y declara un bean:

```java
@Bean
public OpenAPI crediFlowOpenAPI() {
    ...
}
```

Ese bean personaliza el documento OpenAPI generado por Springdoc.

En este proyecto configura:

- Titulo: `CrediFlow API`.
- Descripcion de la API.
- Version: `v1`.
- Esquema de seguridad Bearer JWT.

El esquema JWT se define asi:

```java
new SecurityScheme()
        .name(ESQUEMA_JWT)
        .type(SecurityScheme.Type.HTTP)
        .scheme("bearer")
        .bearerFormat("JWT")
```

Esto permite que Swagger UI muestre el boton `Authorize` y acepte un token JWT
para probar endpoints protegidos.

## 4. Propiedades en application.yaml

Archivo:

```text
src/main/resources/application.yaml
```

Configuracion relacionada:

```yaml
springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    enabled: true
```

Esto deja habilitados:

```text
/v3/api-docs
/swagger-ui.html
```

Si se quisiera desactivar documentacion en algun ambiente, se podrian cambiar
esas propiedades:

```yaml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

## 5. Seguridad: Swagger es publico

Archivo:

```text
src/main/java/edu/upc/sistemas/tbcreditflow/config/SecurityConfig.java
```

Como el backend tiene seguridad JWT, si no se permitieran las rutas de Swagger,
el navegador recibiria `401 Unauthorized` al abrir la documentacion.

Por eso `SecurityConfig` permite estas rutas:

```java
.requestMatchers("/swagger-ui.html", "/swagger-ui/**",
        "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
```

Eso significa:

```text
Swagger UI           -> publico
OpenAPI JSON/YAML    -> publico
Endpoints reales API -> siguen protegidos por sus reglas
```

## 6. URLs disponibles

Con la aplicacion levantada en el puerto por defecto:

```text
http://localhost:8080/swagger-ui.html
http://localhost:8080/v3/api-docs
```

La primera URL abre la interfaz visual.

La segunda devuelve el JSON OpenAPI.

## 7. Como Swagger descubre los endpoints

Springdoc lee los controllers registrados por Spring:

```java
@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    @PostMapping
    ...

    @GetMapping("/{id}")
    ...
}
```

Con eso genera rutas como:

```text
POST /api/solicitudes
GET  /api/solicitudes/{id}
```

Tambien interpreta tipos de entrada/salida, por ejemplo:

```java
@RequestBody CrearSolicitudRequest request
ResponseEntity<SolicitudResponse>
```

Por eso los records `Request` y `Response` aparecen como schemas en OpenAPI.

## 8. Flujo completo

```text
Arranque de Spring Boot
  -> Spring descubre controllers
  -> Springdoc descubre mappings HTTP
  -> OpenApiConfig aporta metadatos y esquema JWT
  -> SecurityConfig permite rutas de documentacion
  -> /v3/api-docs queda disponible
  -> /swagger-ui.html consume /v3/api-docs y muestra la UI
```

## 9. Test de verificacion

Archivo:

```text
src/test/java/edu/upc/sistemas/tbcreditflow/config/OpenApiDocsTest.java
```

El test valida que `/v3/api-docs` sea publico y que el documento generado tenga
lo esperado:

```java
mockMvc.perform(get("/v3/api-docs")) // sin Authorization
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.openapi").exists())
        .andExpect(jsonPath("$.info.title").value("CrediFlow API"))
        .andExpect(jsonPath("$.components.securitySchemes.bearer-jwt.scheme").value("bearer"))
        .andExpect(content().string(containsString("/api/auth/login")));
```

Ese test cubre dos cosas importantes:

- Springdoc esta generando el documento OpenAPI.
- `SecurityConfig` permite consultar la documentacion sin token.

## 10. Resumen mental

```text
Dependencia springdoc      -> agrega Swagger/OpenAPI al proyecto
OpenApiConfig              -> personaliza titulo, descripcion y JWT
application.yaml           -> habilita api-docs y swagger-ui
SecurityConfig             -> deja publicas las rutas de documentacion
OpenApiDocsTest            -> valida que /v3/api-docs funcione sin token
Controllers                -> son la fuente de las rutas documentadas
```

