# CrediFlow — Backend

API REST para la gestión del ciclo de vida de **solicitudes de crédito**: registro de solicitudes y
clientes, carga de documentos, evaluación de riesgo por reglas parametrizables, decisión del comité,
auditoría inmutable encadenada y reportes de indicadores. Todo con seguridad por **JWT** y acceso por
**rol**.

> Backend (Spring Boot). El frontend Angular es un cliente separado; aquí solo se exponen la API y CORS.

## Tabla de contenido
- [Stack](#stack)
- [Arquitectura](#arquitectura)
- [Modelo de dominio](#modelo-de-dominio)
- [Cómo ejecutar](#cómo-ejecutar)
- [Configuración](#configuración)
- [API REST](#api-rest)
- [Seguridad y roles](#seguridad-y-roles)
- [Reglas de negocio clave](#reglas-de-negocio-clave)
- [Pruebas y cobertura](#pruebas-y-cobertura)
- [Estado del proyecto](#estado-del-proyecto)
- [Documentación](#documentación)

## Stack

| Aspecto | Tecnología |
|---|---|
| Lenguaje / runtime | Java 17 (compila a `release 17`) |
| Framework | Spring Boot **4.1.0** (Spring Framework 7, Spring Security 7) |
| Web / validación | Spring Web MVC + Bean Validation (`jakarta.validation`) |
| Persistencia | Spring Data JPA / Hibernate |
| Base de datos | PostgreSQL (producción) · H2 en memoria (pruebas) |
| Seguridad | Spring Security + JWT (`io.jsonwebtoken:jjwt` 0.12.6); contraseñas BCrypt |
| JSON | Jackson 3 (`tools.jackson`) |
| Pruebas / cobertura | JUnit 5 + Spring Test + JaCoCo (gate de líneas ≥ 80 %) |
| Build / CI | Maven (wrapper) + GitHub Actions + Docker |

> **Nota Boot 4.** El JSON usa Jackson 3 (`tools.jackson.databind`), los *starters* están divididos
> (`spring-boot-starter-webmvc`, `-jackson`, `-data-jpa`) y algunas autoconfiguraciones de test/seguridad
> cambiaron de paquete. Detalle en `docs/CrediFlow-Especificacion-Desarrollo.md` §1.

## Arquitectura

Monolito **modular por capas** (Controller · Service · Repository · Domain). Los módulos son paquetes
Java en un único despliegue y se comunican por **inyección de beans** (no por HTTP). Una sola base de
datos.

```
edu.upc.sistemas.tbcreditflow
├── TbCreditFlowApplication
├── common/        # ApiError, excepciones, GlobalExceptionHandler, HashUtil (SHA-256)
├── config/        # SecurityConfig (JWT), CorsConfig, DataSeeder, handlers 401/403
├── security/      # HU12: Usuario, Rol, JwtService, AuthService, login, filtro JWT
├── origination/   # HU01–HU04: Cliente, Solicitud, Documento (registro, documentos, estado)
├── scoring/       # HU05–HU07: EvaluacionRiesgo, ReglaScoring, motor de scoring, CRUD reglas
├── audit/         # HU09–HU11: RegistroAuditoria append-only + hash encadenado (módulo aislado)
└── reporting/     # HU10, HU13: consulta de auditoría e indicadores (lectura cross-módulo)
```

**Dependencias permitidas:** `scoring → origination`; `scoring`/`origination` → `audit`;
todos → `security`; `reporting` → lectura de los demás repos. **`audit` no depende de nadie** (aislado).

## Modelo de dominio

8 tablas: `cliente` (único `tipoDoc+numDoc`), `solicitud`, `documento`, `evaluacion_riesgo`
(único `solicitudId`), `regla_scoring`, `registro_auditoria` (append-only), `usuario`, `rol`.

**Ciclo de vida de la solicitud:**

```
REGISTRADA ──POST evaluación──► EVALUADA ──POST decisión──► APROBADA | RECHAZADA   (terminales)
```

Toda transición no contemplada ⇒ **409**. Pasar a estado terminal genera un `RegistroAuditoria`.

## Cómo ejecutar

Requisitos: JDK 17+ (probado con 17 y 23) y Docker (para PostgreSQL). Se usa el **Maven wrapper**
(`mvnw` / `mvnw.cmd`), no requiere Maven instalado.

```bash
# 1) Base de datos PostgreSQL
docker run --name crediflow-db -e POSTGRES_DB=crediflow -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 -d postgres

# 2) Build + pruebas + cobertura (reporte en target/site/jacoco/index.html)
./mvnw verify          # En Windows: .\mvnw.cmd verify

# 3) Levantar la API en http://localhost:8080/api
./mvnw spring-boot:run

# 4) Empaquetar imagen Docker (build multi-stage) y ejecutarla
docker build -t crediflow .
docker run -p 8080:8080 -e DB_URL=jdbc:postgresql://host.docker.internal:5432/crediflow crediflow
```

Las pruebas usan **H2 en memoria** (perfil `test`), por lo que `./mvnw verify` **no** necesita
PostgreSQL.

Al arrancar, el `DataSeeder` inserta (idempotente): los **7 roles**, un usuario **`admin` / `admin`**
(rol `ADMIN_CREDITO`) y las **4 reglas de scoring** semilla.

### Humo rápido

```bash
# Login (admin)
curl -s -X POST localhost:8080/api/auth/login \
  -H "Content-Type: application/json" -d '{"username":"admin","password":"admin"}'
```

### Documentación interactiva (Swagger UI)

Con la app levantada (springdoc-openapi):

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs

Como toda la API es JWT, en Swagger UI: haz login en `POST /api/auth/login`, copia el `token`, pulsa
**Authorize** y pégalo. A partir de ahí puedes "probar" los endpoints protegidos. En producción se
puede desactivar con `springdoc.api-docs.enabled=false` y `springdoc.swagger-ui.enabled=false`.

## Configuración

`src/main/resources/application.yaml`. Todo es sobreescribible por variable de entorno:

| Propiedad | Variable | Por defecto |
|---|---|---|
| `spring.datasource.url` | `DB_URL` | `jdbc:postgresql://localhost:5432/crediflow` |
| `spring.datasource.username` | `DB_USERNAME` | `postgres` |
| `spring.datasource.password` | `DB_PASSWORD` | `postgres` |
| `spring.jpa.hibernate.ddl-auto` | `DDL_AUTO` | `update` |
| `jwt.secret` | `JWT_SECRET` | (clave de desarrollo; **cambiar en producción**) |
| `jwt.expiration` | `JWT_EXPIRATION` | `86400000` ms (24 h) |
| `app.uploads.dir` | `UPLOADS_DIR` | `./uploads` |
| `app.cors.allowed-origins` | `CORS_ALLOWED_ORIGINS` | `http://localhost:4200` |

Subida de archivos: máximo **5 MB** (`spring.servlet.multipart`).

## API REST

Base `/api`. JSON UTF-8, fechas ISO-8601. Todas (excepto login) requieren
`Authorization: Bearer <JWT>`.

| Método | Endpoint | Descripción | Rol |
|---|---|---|---|
| POST | `/api/auth/login` | Login → `{ token, rol }` | público |
| POST | `/api/solicitudes` | Registrar solicitud (crea/reutiliza cliente) | ASESOR |
| GET | `/api/solicitudes?estado=&clienteId=` | Listar solicitudes | ASESOR |
| GET | `/api/solicitudes/{id}` | Detalle de solicitud | autenticado |
| GET | `/api/solicitudes/{id}/estado` | Estado actual | ASESOR |
| POST | `/api/solicitudes/{id}/documentos` | Adjuntar documento (PDF/JPG ≤5 MB) | ASESOR |
| POST | `/api/solicitudes/{id}/evaluacion` | Evaluar riesgo → EVALUADA | ANALISTA |
| GET | `/api/solicitudes/{id}/evaluacion` | Ver evaluación | ANALISTA, COMITE |
| POST | `/api/solicitudes/{id}/decision` | Aprobar/Rechazar (genera auditoría) | COMITE |
| GET/POST/PUT/DELETE | `/api/reglas`, `/api/reglas/{id}` | CRUD de reglas de scoring | ADMIN_CREDITO |
| GET | `/api/auditoria?clienteId=&desde=&hasta=` | Historial de auditoría | AUDITOR, CUMPLIMIENTO |
| GET | `/api/reportes/indicadores` | Indicadores y tiempos | GERENTE |

**Errores** uniformes (`@RestControllerAdvice`):
```json
{ "timestamp": "...", "status": 400, "error": "Bad Request", "message": "...", "path": "/api/..." }
```
Códigos: 400 (validación / datos), 401 (sin token / credenciales), 403 (rol no autorizado),
404 (no encontrado), 409 (transición inválida).

## Seguridad y roles

JWT *stateless* (HS256); el rol viaja como authority `ROLE_<RolNombre>`. Sin token ⇒ 401; rol no
autorizado ⇒ 403. Contraseñas con **BCrypt**.

Roles: `ASESOR`, `ANALISTA`, `ADMIN_CREDITO`, `COMITE`, `CUMPLIMIENTO`, `AUDITOR`, `GERENTE`.

## Reglas de negocio clave

- **Capacidad de pago:** `capacidadPago = ingreso − deudas`. `ingreso = 0` ⇒ 400;
  `capacidad ≤ 0` ⇒ riesgo **ALTO**, score 0.
- **Motor de scoring:** parte de 100 y aplica las reglas activas (penaliza/bonifica por
  `ratioEndeudamiento` y `ratioCuota`). Nivel: `≥70 BAJO · 40–69 MEDIO · <40 ALTO`.
  Ejemplo: `ratioEnd 0.42 + ratioCuota 0.51 ⇒ 45 / MEDIO`.
- **Auditoría:** `registro_auditoria` es append-only; cada registro encadena
  `hashIntegridad = SHA-256(id | solicitudId | accion | usuario | fecha | hashPrevio)` (no repudio).

## Pruebas y cobertura

```bash
./mvnw verify
```

- **52 pruebas** (unitarias + integración con MockMvc sobre H2).
- Cobertura de líneas **≈ 95 %** (JaCoCo; *gate* ≥ 80 %, falla el build si no se cumple).
- Casos CP01–CP08 cubiertos (registro, validación, capacidad, riesgo alto, auditoría inmutable,
  acceso por rol, consulta de historial, cobertura).
- CI: `.github/workflows/ci.yml` ejecuta `mvnw verify` en cada push/PR.

## Estado del proyecto

✅ **Las 13 historias de usuario (HU01–HU13) operativas** — Fases 0, 1 y 2 completas.
Pendiente externo: análisis de SonarQube (requiere servidor/token).

## Documentación

- `docs/CrediFlow-Especificacion-Desarrollo.md` — especificación base (Fases 0–1) y de verificación.
- `docs/CrediFlow-Especificacion-Fase2.md` — especificación de la Fase 2 y su checklist de verificación.
