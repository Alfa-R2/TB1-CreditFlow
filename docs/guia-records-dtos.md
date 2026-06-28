# Guia de records, DTOs y entities

Esta guia aclara la diferencia entre `record`, DTO y entity dentro del backend.
La confusion aparece porque varios DTOs del proyecto estan implementados como
`record`, pero no son conceptos equivalentes.

## 1. Idea central

`record` es una herramienta del lenguaje Java.

DTO es un rol arquitectonico.

Por tanto:

```text
Un DTO puede estar implementado como record.
Un record no necesariamente es un DTO.
```

En este proyecto, muchos DTOs estan implementados como `record`.

## 2. Ejemplo de DTO de entrada

Archivo:

```text
src/main/java/edu/upc/sistemas/tbcreditflow/security/domain/dto/LoginRequest.java
```

Codigo:

```java
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password
) {
}
```

Este tipo es ambas cosas:

```text
record -> porque usa la sintaxis de Java record
DTO    -> porque transporta datos desde el request HTTP hacia la aplicacion
```

El controller recibe ese DTO asi:

```java
@PostMapping("/login")
public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(authService.login(request));
}
```

## 3. Ejemplo de DTO de salida

Archivo:

```text
src/main/java/edu/upc/sistemas/tbcreditflow/security/domain/dto/TokenResponse.java
```

Codigo:

```java
public record TokenResponse(
        String token,
        String rol
) {
}
```

Este es un DTO de salida. Su objetivo no es representar una tabla ni tener
logica de negocio; solo transporta la respuesta HTTP:

```json
{
  "token": "...",
  "rol": "ASESOR"
}
```

## 4. Comparacion rapida

| Concepto | Que es | Ejemplo en el proyecto |
| --- | --- | --- |
| `record` | Forma corta e inmutable de declarar datos en Java | `LoginRequest`, `TokenResponse` |
| DTO | Objeto usado para transportar datos entre capas o hacia/desde la API | `CrearSolicitudRequest`, `SolicitudResponse`, `ReglaScoringResponse` |
| Entity | Objeto persistido en base de datos con JPA | `Solicitud`, `Cliente`, `Usuario`, `ReglaScoring` |

## 5. Por que usar records para DTOs

Un `record` funciona bien para DTOs simples porque:

- Es inmutable.
- Evita setters.
- Genera constructor automaticamente.
- Genera `equals`, `hashCode` y `toString`.
- Funciona bien con JSON/Jackson.
- Deja claro que el objeto solo transporta datos.

En un record, los campos se leen con metodos que tienen el mismo nombre del
campo:

```java
request.username()
request.password()
```

No se leen asi:

```java
request.getUsername()
request.getPassword()
```

## 6. Por que las entities no son records

Las entities del proyecto usan clases normales.

Ejemplo:

```text
src/main/java/edu/upc/sistemas/tbcreditflow/origination/domain/entity/Solicitud.java
```

Codigo:

```java
@Entity
public class Solicitud {
    ...
}
```

JPA suele encajar mejor con clases normales porque las entities pueden necesitar:

- Constructor vacio.
- Mutabilidad.
- Setters o acceso por campos.
- Proxies de Hibernate.
- Relaciones con otras entities.
- Ciclo de vida de persistencia.

Por eso una entity no deberia mezclarse con un DTO aunque tengan campos
parecidos.

## 7. Regla mental para este proyecto

```text
Si entra por HTTP: Request record.
Si sale por HTTP: Response record.
Si se guarda en DB: Entity class.
Si tiene logica: Service class.
Si expone endpoints: Controller class.
```

La pregunta correcta no es:

```text
Records o DTOs?
```

La pregunta correcta es:

```text
Este DTO lo implemento como record o como class?
```

En este backend, la respuesta general es:

```text
DTO simple de entrada/salida -> record
Entity JPA                   -> class
Servicio con logica          -> class
Configuracion Spring         -> class
Filtro Spring Security       -> class
```

## 8. DTOs del proyecto implementados como records

Algunos ejemplos:

```text
security/domain/dto/LoginRequest.java
security/domain/dto/TokenResponse.java
origination/domain/dto/CrearSolicitudRequest.java
origination/domain/dto/ClienteRequest.java
origination/domain/dto/DecisionRequest.java
origination/domain/dto/SolicitudResponse.java
origination/domain/dto/DocumentoResponse.java
origination/domain/dto/EstadoResponse.java
scoring/domain/dto/ReglaScoringRequest.java
scoring/domain/dto/ReglaScoringResponse.java
scoring/domain/dto/EvaluacionResponse.java
audit/domain/dto/RegistroAuditoriaResponse.java
reporting/domain/ReporteIndicadores.java
```

## 9. Entities del proyecto implementadas como classes

Algunos ejemplos:

```text
security/domain/entity/Usuario.java
security/domain/entity/Rol.java
origination/domain/entity/Cliente.java
origination/domain/entity/Solicitud.java
origination/domain/entity/Documento.java
scoring/domain/entity/ReglaScoring.java
scoring/domain/entity/EvaluacionRiesgo.java
audit/domain/entity/RegistroAuditoria.java
```

## 10. Cuando NO usar record para un DTO

Aunque los records son comodos, no siempre convienen.

Preferir una clase normal si el DTO necesita:

- Muchos campos opcionales con construccion compleja.
- Un builder local ya establecido en el proyecto.
- Mutabilidad real.
- Compatibilidad con una libreria que exige constructor vacio y setters.
- Representar un formulario parcial tipo PATCH donde distinguir `null`,
  ausente y valor enviado sea importante.

En este proyecto, para los DTOs actuales, `record` es una decision razonable:
son objetos simples de entrada y salida, sin identidad propia ni logica de
negocio.
