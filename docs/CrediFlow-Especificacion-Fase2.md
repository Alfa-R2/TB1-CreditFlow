# CrediFlow — Especificación Fase 2 (Backend)

> **Propósito.** Especificar e indicar cómo **verificar** la Fase 2 del backend de CrediFlow. Es
> complementaria a `docs/CrediFlow-Especificacion-Desarrollo.md` (en adelante, *el doc base*): se
> reutilizan su stack (§1), arquitectura (§2), modelo de datos (§3), manejo de errores (§7) y
> matriz de seguridad (§6). Aquí solo se detalla lo nuevo de la Fase 2.
>
> **Regla de oro (igual que el doc base):** si algo no está definido, NO se inventa — se pregunta.
> Nombres de campos/endpoints **exactos**.

## Tabla de contenido
1. [Alcance](#1-alcance)
2. [Decisiones y supuestos a revisar](#2-decisiones-y-supuestos-a-revisar)
3. [Contratos de API](#3-contratos-de-api)
4. [Reglas de negocio](#4-reglas-de-negocio)
5. [Seguridad (filas añadidas a la matriz §6)](#5-seguridad)
6. [Historias de usuario ↔ implementación](#6-historias-de-usuario--implementación)
7. [Pruebas](#7-pruebas)
8. [Checklist de verificación (para el desarrollador)](#8-checklist-de-verificación)
9. [Comandos de humo](#9-comandos-de-humo)

---

## 1. Alcance

Fase 2 = "Resto" del plan por fases del doc base (§10). Completa las 4 HU pendientes:

| HU | Descripción | Endpoint(s) | Rol(es) |
|---|---|---|---|
| **HU04** | Consultar estado de una solicitud | `GET /api/solicitudes/{id}/estado` | ASESOR |
| **HU07** | CRUD de reglas de scoring (parametrización) | `GET/POST/PUT/DELETE /api/reglas` | ADMIN_CREDITO |
| **HU10** | Consulta de auditoría (historial) | `GET /api/auditoria?clienteId=&desde=&hasta=` | AUDITOR, CUMPLIMIENTO |
| **HU13** | Indicadores y tiempos | `GET /api/reportes/indicadores` | GERENTE |

**DoD Fase 2 (del doc base):** las 13 HU operativas; CP01–CP08 verdes; cobertura ≥ 80%;
SonarQube sin críticos.

---

## 2. Decisiones y supuestos a revisar

> Estos puntos resuelven ambigüedades del doc base. **El desarrollador debe confirmarlos**; si
> alguno no coincide con lo esperado, se ajusta.

1. **Ubicación de HU10 (consulta de auditoría).** El módulo `audit` **permanece aislado** (§2 del
   doc base: "audit no depende de nadie"). Como la consulta por cliente requiere *join* a
   `Solicitud`, la orquestación de HU10 vive en el módulo **`reporting`** (al que §2 permite "lectura
   de los demás repos"). El endpoint sigue siendo `GET /api/auditoria`. Así `audit` no adquiere
   dependencias salientes.
2. **`desde` / `hasta` (HU10)** son **fechas** ISO (`yyyy-MM-dd`), **inclusivas** ambos extremos.
   Internamente: `fecha >= desde 00:00` y `fecha < (hasta + 1 día) 00:00`. Ambos son opcionales.
3. **`tiempoPromedioDias` (HU13)** = promedio, sobre las solicitudes **con decisión** (APROBADA o
   RECHAZADA), de los días entre `Solicitud.fechaRegistro` y la `fecha` del `RegistroAuditoria` de su
   decisión. Sin decisiones ⇒ `0`. Valor con 2 decimales.
4. **`porcentajeAprobacion` (HU13)** = `aprobadas / totalSolicitudes * 100` (2 decimales). Si
   `totalSolicitudes == 0` ⇒ `0`.
5. **`distribucionRiesgo` (HU13)** cuenta `EvaluacionRiesgo` por nivel; siempre incluye las 3 claves
   `BAJO`, `MEDIO`, `ALTO` (0 si no hay).
6. **HU07 — `parametro`** solo admite `ratioEndeudamiento` o `ratioCuota` (§3 del doc base). Otro
   valor ⇒ **400** (evita reglas que el motor ignoraría en silencio).
7. **HU07 — códigos:** `GET` 200 · `POST` 201 · `PUT` 200 · `DELETE` 204; `PUT/DELETE` sobre id
   inexistente ⇒ 404.

---

## 3. Contratos de API

Base `/api`. Todas requieren `Authorization: Bearer <JWT>`. Cuerpo de error uniforme: §7 del doc base.

### HU04 — Consultar estado
- `GET /api/solicitudes/{id}/estado` → `200 { "estado": "REGISTRADA|EVALUADA|APROBADA|RECHAZADA" }`
  | `404` si la solicitud no existe.

### HU07 — CRUD de reglas
Forma de `ReglaScoring` (request/response):
```json
{ "id": 1, "nombre": "Endeudamiento alto", "parametro": "ratioEndeudamiento",
  "operador": "GT", "umbral": 0.40, "ponderacion": -30, "activa": true }
```
- `GET /api/reglas` → `200 [ReglaScoring]`.
- `POST /api/reglas` — body sin `id` → `201 ReglaScoring` | `400` (validación / `parametro` inválido).
- `PUT /api/reglas/{id}` — body sin `id` → `200 ReglaScoring` | `400` | `404`.
- `DELETE /api/reglas/{id}` → `204` | `404`.

Validación del body (POST/PUT): `nombre` no vacío; `parametro` ∈ {`ratioEndeudamiento`,`ratioCuota`};
`operador` ∈ {`GT`,`LT`,`EQ`}; `umbral` no nulo; `ponderacion` no nula (entero con signo); `activa`
no nula.

### HU10 — Consulta de auditoría
- `GET /api/auditoria?clienteId=&desde=&hasta=` → `200 [RegistroAuditoria]` ordenado por `id` asc.
  Filtros opcionales y combinables: `clienteId` (join a Solicitud), `desde`/`hasta` (rango de fechas).
```json
{ "id": 1, "solicitudId": 5, "accion": "APROBADA", "usuario": "comite1",
  "fecha": "2026-06-21T18:00:00", "hashIntegridad": "…64 hex…", "hashPrevio": "…|''" }
```

### HU13 — Indicadores
- `GET /api/reportes/indicadores` → `200`:
```json
{ "totalSolicitudes": 10, "aprobadas": 6, "rechazadas": 2,
  "porcentajeAprobacion": 60.00, "tiempoPromedioDias": 1.50,
  "distribucionRiesgo": { "BAJO": 3, "MEDIO": 5, "ALTO": 2 } }
```

---

## 4. Reglas de negocio

- **HU04:** lectura pura; no cambia estado.
- **HU07:** las reglas son parámetros del motor (§4.3 del doc base). Crear/editar/activar/desactivar
  una regla **afecta a las evaluaciones posteriores** (no recalcula evaluaciones ya persistidas). El
  `DELETE` es físico (la tabla de reglas no es append-only).
- **HU10:** solo lectura sobre `RegistroAuditoria` (que sigue siendo append-only). El filtro por
  `clienteId` resuelve primero las solicitudes del cliente y luego acota los registros.
- **HU13:** solo lectura agregada; fórmulas según §2 (decisiones 3–5).

---

## 5. Seguridad

Filas que se hacen efectivas sobre la matriz del §6 del doc base (acceso indebido ⇒ 403; sin token ⇒ 401):

| Endpoint | Rol(es) |
|---|---|
| `GET /api/solicitudes/{id}/estado` | ASESOR |
| `GET/POST/PUT/DELETE /api/reglas` | ADMIN_CREDITO |
| `GET /api/auditoria` | AUDITOR, CUMPLIMIENTO |
| `GET /api/reportes/indicadores` | GERENTE |

---

## 6. Historias de usuario ↔ implementación

| HU | Módulo | Clases principales |
|---|---|---|
| HU04 | origination | `SolicitudController.consultarEstado`, `SolicitudService.consultarEstado` |
| HU07 | scoring | `ReglaScoringController`, `ReglaScoringService`, `ReglaScoringRequest/Response`, `ReglaScoring.actualizar` |
| HU10 | reporting (+ audit aislado) | `AuditoriaController`, `AuditoriaConsultaService`; `RegistroAuditoriaRepository.buscarPorRango`, `RegistroAuditoriaResponse` |
| HU13 | reporting | `ReporteController`, `ReporteService`, `ReporteIndicadores` |

---

## 7. Pruebas

- **CP07** (funcional): consulta de historial filtrada por cliente y por rango de fechas.
- HU04: estado correcto (200) y 404.
- HU07: alta/edición/baja (201/200/204), 400 (parámetro inválido / datos faltantes), 404, y acceso
  por rol (403 a no-ADMIN).
- HU10: lista, filtro por `clienteId`, filtro por fechas, y rol (403 a no AUDITOR/CUMPLIMIENTO).
- HU13: presencia y corrección de los indicadores; rol (403 a no GERENTE).
- **CP08:** cobertura de líneas ≥ 80% (JaCoCo) sobre toda la suite.

---

## 8. Checklist de verificación

> Para el desarrollador: marcar comprobándolo de forma objetiva (con `curl`/tests).

### HU04 — Estado
- [ ] `GET /api/solicitudes/{id}/estado` devuelve el estado actual (200).
- [ ] Solicitud inexistente ⇒ 404.
- [ ] Solo ASESOR (otro rol ⇒ 403; sin token ⇒ 401).

### HU07 — CRUD reglas
- [ ] `GET /api/reglas` lista las reglas (incluidas las 4 semilla iniciales).
- [ ] `POST` con datos válidos ⇒ 201 y la regla aparece en `GET`.
- [ ] `POST/PUT` con `parametro` distinto de `ratioEndeudamiento`/`ratioCuota` ⇒ 400.
- [ ] `POST/PUT` con campo obligatorio faltante ⇒ 400.
- [ ] `PUT /api/reglas/{id}` modifica la regla (200); id inexistente ⇒ 404.
- [ ] `DELETE /api/reglas/{id}` elimina (204); id inexistente ⇒ 404.
- [ ] Crear/editar una regla **cambia el resultado** de una evaluación posterior (probarlo).
- [ ] Solo ADMIN_CREDITO (otro rol ⇒ 403).

### HU10 — Consulta de auditoría
- [ ] `GET /api/auditoria` sin filtros lista todos los registros (orden por id asc).
- [ ] `?clienteId=X` devuelve **solo** los registros de las solicitudes del cliente X.
- [ ] `?desde=&hasta=` acota por fecha (inclusivo en ambos extremos).
- [ ] Combinación `clienteId + fechas` funciona (CP07).
- [ ] Solo AUDITOR y CUMPLIMIENTO (otro rol ⇒ 403).
- [ ] El módulo `audit` **no importa** clases de otros módulos de negocio (sigue aislado).

### HU13 — Indicadores
- [ ] La respuesta incluye `totalSolicitudes`, `aprobadas`, `rechazadas`, `porcentajeAprobacion`,
      `tiempoPromedioDias`, `distribucionRiesgo:{BAJO,MEDIO,ALTO}`.
- [ ] `aprobadas + rechazadas ≤ totalSolicitudes`; conteos coinciden con la BD.
- [ ] `porcentajeAprobacion` = aprobadas/total*100 (0 si total 0).
- [ ] `distribucionRiesgo` suma = nº de evaluaciones; `tiempoPromedioDias ≥ 0`.
- [ ] Solo GERENTE (otro rol ⇒ 403).

### General
- [ ] Las **13 HU** responden con los códigos del §7 del doc base.
- [ ] Cuerpo de error uniforme en los nuevos endpoints.
- [ ] `mvn verify` verde con **cobertura ≥ 80%** (JaCoCo); CP01–CP08 verdes.

---

## 9. Comandos de humo

```bash
TOKEN_ADMIN=...     # login admin (rol ADMIN_CREDITO)
TOKEN_ASESOR=...; TOKEN_AUDITOR=...; TOKEN_GERENTE=...

# HU04 — estado
curl -s localhost:8080/api/solicitudes/1/estado -H "Authorization: Bearer $TOKEN_ASESOR"

# HU07 — listar / crear regla
curl -s localhost:8080/api/reglas -H "Authorization: Bearer $TOKEN_ADMIN"
curl -s -X POST localhost:8080/api/reglas -H "Authorization: Bearer $TOKEN_ADMIN" \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Cuota media","parametro":"ratioCuota","operador":"GT","umbral":0.30,"ponderacion":-10,"activa":true}'

# HU10 — auditoría filtrada
curl -s "localhost:8080/api/auditoria?clienteId=1&desde=2026-01-01&hasta=2026-12-31" \
  -H "Authorization: Bearer $TOKEN_AUDITOR"

# HU13 — indicadores
curl -s localhost:8080/api/reportes/indicadores -H "Authorization: Bearer $TOKEN_GERENTE"
```
