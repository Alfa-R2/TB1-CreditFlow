# Guia de lectura del backend Spring Boot

Esta guia esta pensada para leer el proyecto desde el punto de entrada hacia las
piezas que atienden una request HTTP. La idea es tener un mapa similar al que en
FastAPI normalmente se obtiene leyendo `app.py`, donde se crea `app` y se
registran los routers.

Guias complementarias:

```text
docs/guia-securityconfig.md
docs/guia-records-dtos.md
docs/guia-swagger-openapi.md
docs/guia-dataseeder.md
docs/guia-excepciones-validacion.md
```

## 1. Punto de entrada

Archivo:

```text
src/main/java/edu/upc/sistemas/tbcreditflow/TbCreditFlowApplication.java
```

Codigo principal:

```java
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class TbCreditFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(TbCreditFlowApplication.class, args);
    }
}
```

Este archivo cumple el rol mas cercano al `app.py` de FastAPI.

En FastAPI suele verse algo como:

```python
app = FastAPI()
app.include_router(auth_router)
app.include_router(solicitudes_router)
```

En este proyecto Spring Boot no registra los controladores manualmente en un
archivo central. En vez de eso, `SpringApplication.run(...)` arranca la
aplicacion y `@SpringBootApplication` activa el escaneo automatico de clases.

## 2. Que hace `@SpringBootApplication`

`@SpringBootApplication` agrupa tres comportamientos importantes:

| Responsabilidad | Que significa en este proyecto |
| --- | --- |
| Configuracion | La clase principal tambien puede ser fuente de configuracion Spring. |
| Autoconfiguracion | Spring Boot mira las dependencias del `pom.xml` y configura Web MVC, JPA, Security, validacion, etc. |
| Escaneo de componentes | Spring busca clases anotadas dentro del paquete base y sus subpaquetes. |

El paquete base esta definido por la linea:

```java
package edu.upc.sistemas.tbcreditflow;
```

Por eso Spring escanea automaticamente todo lo que este debajo de:

```text
edu.upc.sistemas.tbcreditflow.*
```

Ejemplos:

```text
edu.upc.sistemas.tbcreditflow.security
edu.upc.sistemas.tbcreditflow.origination
edu.upc.sistemas.tbcreditflow.scoring
edu.upc.sistemas.tbcreditflow.reporting
edu.upc.sistemas.tbcreditflow.config
```

Si una clase `@RestController`, `@Service`, `@Repository` o `@Configuration`
queda fuera de ese paquete base, Spring no la descubriria automaticamente salvo
que se configure un escaneo adicional.

## 3. Donde estan los "routers"

En Spring MVC, los equivalentes a routers de FastAPI son los controladores:

```text
src/main/java/edu/upc/sistemas/tbcreditflow/**/controller/*.java
```

Cada controlador usa:

```java
@RestController
@RequestMapping(...)
```

Y cada metodo usa anotaciones como:

```java
@GetMapping
@PostMapping
@PutMapping
@DeleteMapping
```

Ejemplo:

```java
@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    @PostMapping
    public ResponseEntity<SolicitudResponse> crear(...) {
        ...
    }

    @GetMapping("/{id}")
    public ResponseEntity<SolicitudResponse> obtener(...) {
        ...
    }
}
```

Eso produce estas rutas:

```text
POST /api/solicitudes
GET  /api/solicitudes/{id}
```

## 4. Mapa de controladores y rutas

| Archivo | Base path | Rutas principales |
| --- | --- | --- |
| `security/controller/AuthController.java` | `/api/auth` | `POST /login` |
| `origination/controller/SolicitudController.java` | `/api/solicitudes` | `POST /`, `GET /`, `GET /{id}`, `GET /{id}/estado`, `POST /{id}/documentos`, `POST /{id}/decision` |
| `scoring/controller/EvaluacionController.java` | `/api/solicitudes/{solicitudId}/evaluacion` | `POST /`, `GET /` |
| `scoring/controller/ReglaScoringController.java` | `/api/reglas` | `GET /`, `POST /`, `PUT /{id}`, `DELETE /{id}` |
| `reporting/controller/AuditoriaController.java` | `/api/auditoria` | `GET /` |
| `reporting/controller/ReporteController.java` | `/api/reportes` | `GET /indicadores` |

Importante: cuando una fila dice `POST /`, significa que la ruta final es el
`Base path` completo. Por ejemplo:

```text
Base path: /api/auth
Metodo:    POST /login
Final:     POST /api/auth/login
```

## 5. Flujo de una request

Una request HTTP no entra directamente al controller. Pasa por varias capas:

```text
Cliente HTTP
  -> servidor web embebido de Spring Boot
    -> filtros de seguridad
      -> DispatcherServlet
        -> Controller
          -> Service
            -> Repository
              -> Base de datos
```

Lectura por archivos:

| Capa | Donde mirar | Responsabilidad |
| --- | --- | --- |
| Entrada de aplicacion | `TbCreditFlowApplication.java` | Arranca Spring Boot. |
| Configuracion HTTP y seguridad | `config/SecurityConfig.java` | Define permisos, roles, JWT y filtros. |
| Controllers | `*/controller/*.java` | Exponen endpoints HTTP. |
| Services | `*/service/*.java` | Contienen reglas de negocio. |
| Repositories | `*/repository/*.java` | Acceso a datos con Spring Data JPA. |
| Domain | `*/domain/*.java` | Entidades, requests, responses y enums. |
| Configuracion externa | `src/main/resources/application.yaml` | DB, JWT, uploads, CORS, JPA. |

## 6. Seguridad: antes del controller

Archivo clave:

```text
src/main/java/edu/upc/sistemas/tbcreditflow/config/SecurityConfig.java
```

El detalle completo esta separado en:

```text
docs/guia-securityconfig.md
```

Resumen rapido:

- `SecurityConfig` arma el `SecurityFilterChain`.
- `JwtAuthenticationFilter` valida el JWT y llena el `SecurityContextHolder`.
- `JwtService` genera y valida tokens.
- `AuthService` valida credenciales y emite el JWT.
- `JwtAuthenticationEntryPoint` responde `401`.
- `RestAccessDeniedHandler` responde `403`.
- Las reglas `hasRole(...)` esperan authorities con prefijo `ROLE_`.
- La aplicacion es stateless: cada request protegida debe traer
  `Authorization: Bearer <token>`.

## 7. Configuracion externa

Archivo:

```text
src/main/resources/application.yaml
```

Aqui estan valores externos del backend:

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/crediflow}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:admin}

jwt:
  secret: ${JWT_SECRET:...}
  expiration: ${JWT_EXPIRATION:86400000}

app:
  uploads:
    dir: ${UPLOADS_DIR:./uploads}
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:4200}
```

La sintaxis:

```text
${NOMBRE_VARIABLE:valor_por_defecto}
```

significa:

- Si existe la variable de entorno, usa esa.
- Si no existe, usa el valor por defecto.

## 8. Como leer una funcionalidad de punta a punta

Ejemplo: crear una solicitud.

Ruta:

```text
POST /api/solicitudes
```

Orden recomendado de lectura:

1. `config/SecurityConfig.java`
   - Verifica que el endpoint requiere rol `ASESOR`.

2. `origination/controller/SolicitudController.java`
   - Encuentra el metodo `crear(...)`.
   - Recibe un `CrearSolicitudRequest`.
   - Devuelve un `SolicitudResponse`.

3. `origination/service/SolicitudService.java`
   - Contiene la logica de negocio para crear la solicitud.

4. `origination/repository/SolicitudRepository.java`
   - Acceso a base de datos.

5. `origination/domain/*.java`
   - Estructuras de entrada, salida, entidad y enums relacionados.

Ese mismo patron se repite en las otras funcionalidades:

```text
Controller -> Service -> Repository -> Domain
```

## 9. Equivalencia mental con FastAPI

| FastAPI | Spring Boot en este proyecto |
| --- | --- |
| `app = FastAPI()` | `SpringApplication.run(TbCreditFlowApplication.class, args)` |
| `include_router(...)` | Escaneo automatico de `@RestController` |
| `APIRouter(prefix="/api/...")` | `@RequestMapping("/api/...")` en el controller |
| `@router.get(...)` | `@GetMapping(...)` |
| `@router.post(...)` | `@PostMapping(...)` |
| Pydantic model | Clases request/response en `domain` |
| Depends | Inyeccion por constructor |
| Middleware | Filtros, por ejemplo `JwtAuthenticationFilter` |
| Exception handlers | `GlobalExceptionHandler` con `@RestControllerAdvice` |

## 10. Checklist rapido para orientarse

Cuando quieras entender una ruta, busca en este orden:

1. Ruta y rol:

```text
config/SecurityConfig.java
```

2. Endpoint HTTP:

```text
*/controller/*.java
```

3. Logica de negocio:

```text
*/service/*.java
```

4. Persistencia:

```text
*/repository/*.java
```

5. Datos de entrada/salida:

```text
*/domain/*.java
```

6. Configuracion externa:

```text
src/main/resources/application.yaml
```

Con este orden se puede trazar una funcionalidad desde la URL publica hasta la
base de datos sin depender de entender todo Spring Boot de golpe.
