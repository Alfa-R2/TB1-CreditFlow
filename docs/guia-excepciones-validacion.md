# Guia de GlobalExceptionHandler, excepciones y validacion

Esta guia explica como el backend convierte errores en respuestas HTTP
uniformes, y como se relacionan `@Valid`, las anotaciones de validacion y los
metodos `@ExceptionHandler`.

## 1. Archivo principal

Archivo:

```text
src/main/java/edu/upc/sistemas/tbcreditflow/common/GlobalExceptionHandler.java
```

La clase esta anotada con:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
```

`@RestControllerAdvice` le dice a Spring:

```text
Esta clase maneja excepciones lanzadas desde controllers REST.
Sus respuestas se serializan como JSON.
Aplica de forma global a los controllers del proyecto.
```

No se llama manualmente desde los controllers. Spring la usa automaticamente
cuando una excepcion ocurre durante el procesamiento de una request.

## 2. Formato uniforme de error

Archivo:

```text
src/main/java/edu/upc/sistemas/tbcreditflow/common/ApiError.java
```

Codigo:

```java
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
```

Todas las respuestas de error que pasan por `GlobalExceptionHandler` usan ese
formato:

```json
{
  "timestamp": "2026-06-28T18:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "username: no debe estar vacio",
  "path": "/api/auth/login"
}
```

El metodo privado que arma esa respuesta es:

```java
private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest req) {
    ApiError body = new ApiError(
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            req.getRequestURI()
    );
    return ResponseEntity.status(status).body(body);
}
```

## 3. Como funciona `@ExceptionHandler`

Cada metodo de `GlobalExceptionHandler` declara que excepcion sabe manejar:

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ApiError> handleValidation(...) {
    ...
}
```

Lectura:

```text
Si ocurre MethodArgumentNotValidException,
llama a handleValidation(...)
y devuelve un ApiError con HTTP 400.
```

Otro ejemplo:

```java
@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<ApiError> handleNotFound(...) {
    return build(HttpStatus.NOT_FOUND, ex.getMessage(), req);
}
```

Lectura:

```text
Si un service lanza ResourceNotFoundException,
Spring responde 404 con ApiError.
```

## 4. Relacion entre `@Valid` y el handler

Ejemplo real:

```text
src/main/java/edu/upc/sistemas/tbcreditflow/security/controller/AuthController.java
```

```java
@PostMapping("/login")
public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(authService.login(request));
}
```

El DTO:

```text
src/main/java/edu/upc/sistemas/tbcreditflow/security/domain/LoginRequest.java
```

```java
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password
) {
}
```

Flujo si llega un body invalido:

```text
Cliente
  -> POST /api/auth/login
     body: {"username":""}

Spring MVC
  -> convierte JSON a LoginRequest
  -> ve @Valid
  -> revisa @NotBlank en username y password
  -> detecta errores
  -> lanza MethodArgumentNotValidException

GlobalExceptionHandler
  -> handleValidation(...)
  -> responde 400 con ApiError
```

Punto importante:

```text
@Valid dispara la validacion.
@ExceptionHandler arma la respuesta HTTP.
```

Sin `@Valid`, las anotaciones como `@NotBlank` o `@NotNull` en el DTO no se
aplicarian automaticamente al `@RequestBody`.

## 5. Validacion anidada

Ejemplo real:

```text
src/main/java/edu/upc/sistemas/tbcreditflow/origination/domain/CrearSolicitudRequest.java
```

```java
public record CrearSolicitudRequest(
        @NotNull @Valid ClienteRequest cliente,
        @NotNull @Positive(message = "monto debe ser > 0") BigDecimal monto,
        @NotNull @Positive(message = "plazoMeses debe ser > 0") Integer plazoMeses
) {
}
```

El campo `cliente` tambien tiene `@Valid`.

Eso significa:

```text
Valida CrearSolicitudRequest.
Ademas entra a validar ClienteRequest.
```

DTO anidado:

```text
src/main/java/edu/upc/sistemas/tbcreditflow/origination/domain/ClienteRequest.java
```

```java
public record ClienteRequest(
        @NotNull TipoDoc tipoDoc,
        @NotBlank String numDoc,
        @NotBlank String nombres,
        @NotBlank String apellidos,
        @NotNull @DecimalMin(value = "0.0", message = "ingresoMensual debe ser >= 0") BigDecimal ingresoMensual,
        @NotNull @DecimalMin(value = "0.0", message = "deudasActuales debe ser >= 0") BigDecimal deudasActuales
) {
}
```

Si `cliente` no tuviera `@Valid`, Spring validaria que `cliente` no sea null,
pero no necesariamente validaria los campos internos de `ClienteRequest`.

## 6. Validaciones usadas en el proyecto

Ejemplos de anotaciones:

| Anotacion | Uso |
| --- | --- |
| `@NotBlank` | Strings obligatorios y no vacios. |
| `@NotNull` | Campo obligatorio. |
| `@Positive` | Numero mayor que cero. |
| `@DecimalMin("0.0")` | Numero decimal minimo. |
| `@Valid` | Activa validacion del objeto o validacion anidada. |

Ejemplos reales:

```java
@NotBlank String username
@NotNull @Positive(message = "monto debe ser > 0") BigDecimal monto
@NotNull @DecimalMin(value = "0.0", message = "ingresoMensual debe ser >= 0") BigDecimal ingresoMensual
```

Estas anotaciones vienen de Jakarta Validation y funcionan porque el proyecto
tiene la dependencia:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

## 7. Tabla de excepciones manejadas

`GlobalExceptionHandler` traduce estas excepciones:

| Excepcion | Cuando ocurre | HTTP |
| --- | --- | --- |
| `MethodArgumentNotValidException` | Falla `@Valid` sobre `@RequestBody`. | `400` |
| `ConstraintViolationException` | Falla validacion sobre parametros, normalmente con `@Validated`. | `400` |
| `HttpMessageNotReadableException` | JSON malformado, body ilegible o enum invalido. | `400` |
| `MethodArgumentTypeMismatchException` | Parametro con tipo incorrecto, por ejemplo `id` no numerico. | `400` |
| `BadRequestException` | Regla de negocio considera invalida la entrada. | `400` |
| `MaxUploadSizeExceededException` | Archivo supera el maximo permitido. | `400` |
| `BadCredentialsException` | Login con credenciales invalidas. | `401` |
| `AccessDeniedException` | Acceso denegado a nivel controller/metodo. | `403` |
| `ResourceNotFoundException` | Recurso inexistente. | `404` |
| `ConflictException` | Conflicto de estado o accion invalida. | `409` |
| `Exception` | Cualquier error no contemplado. | `500` |

## 8. Excepciones propias del proyecto

Hay tres excepciones simples en `common/`:

```text
BadRequestException
ConflictException
ResourceNotFoundException
```

Cada una representa una intencion HTTP:

| Clase | Significado | HTTP |
| --- | --- | --- |
| `BadRequestException` | Datos invalidos o regla de negocio incumplida. | `400` |
| `ResourceNotFoundException` | No existe el recurso solicitado. | `404` |
| `ConflictException` | La operacion no es compatible con el estado actual. | `409` |

Ejemplo mental:

```java
throw new ResourceNotFoundException("Solicitud no encontrada");
```

No hace falta capturarla en el controller. Si sube hasta Spring MVC,
`GlobalExceptionHandler` la captura y responde:

```text
HTTP 404
ApiError.message = "Solicitud no encontrada"
```

## 9. Diferencia entre validacion y regla de negocio

Validacion con anotaciones:

```java
@NotBlank String username
@Positive BigDecimal monto
```

Sirve para reglas estructurales simples:

```text
campo requerido
string no vacio
numero positivo
formato basico
```

Regla de negocio en service:

```java
throw new BadRequestException("...");
throw new ConflictException("...");
throw new ResourceNotFoundException("...");
```

Sirve para reglas que requieren consultar estado o aplicar logica:

```text
la solicitud existe?
esta en el estado correcto?
el cliente ya existe?
hay datos suficientes para scoring?
la transicion de estado es valida?
```

## 10. Flujo completo de request invalida

Ejemplo:

```text
POST /api/solicitudes
```

Body incompleto:

```json
{
  "cliente": {
    "tipoDoc": "DNI",
    "nombres": "Juan"
  },
  "plazoMeses": 24
}
```

Flujo:

```text
SecurityConfig
  -> valida que el usuario tenga rol ASESOR

Spring MVC
  -> parsea JSON
  -> crea CrearSolicitudRequest
  -> ejecuta @Valid
  -> detecta errores en monto y campos de cliente
  -> lanza MethodArgumentNotValidException

GlobalExceptionHandler
  -> handleValidation(...)
  -> junta errores de campos
  -> build(HttpStatus.BAD_REQUEST, message, request)

Respuesta
  -> HTTP 400
  -> ApiError
```

El controller no necesita `try/catch`:

```java
@PostMapping
public ResponseEntity<SolicitudResponse> crear(@Valid @RequestBody CrearSolicitudRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(solicitudService.crear(request));
}
```

Si la validacion falla, el metodo `crear(...)` ni siquiera llega a ejecutar su
cuerpo.

## 11. Seguridad: dos lugares pueden generar 401/403

Hay que distinguir estos dos caminos:

```text
Seguridad antes del controller
  -> JwtAuthenticationEntryPoint
  -> RestAccessDeniedHandler

Excepciones dentro del flujo MVC/controller
  -> GlobalExceptionHandler
```

Ejemplos:

| Caso | Quien responde |
| --- | --- |
| No hay token en endpoint protegido | `JwtAuthenticationEntryPoint` |
| Token valido pero rol incorrecto antes de entrar al controller | `RestAccessDeniedHandler` |
| Login ejecuta controller pero password es incorrecto | `GlobalExceptionHandler` maneja `BadCredentialsException` |
| Una anotacion de seguridad a nivel metodo lanza `AccessDeniedException` | `GlobalExceptionHandler` podria manejarla |

Por eso el proyecto tiene handlers de seguridad en `config/` y tambien handlers
de excepciones en `common/`.

## 12. Resumen mental

```text
@Valid
  -> activa validacion de DTOs

@NotBlank / @NotNull / @Positive
  -> definen reglas de campos

Spring MVC
  -> lanza excepciones cuando algo falla

@RestControllerAdvice
  -> escucha excepciones globalmente

@ExceptionHandler
  -> mapea tipo de excepcion a respuesta HTTP

ApiError
  -> formato uniforme de error
```

