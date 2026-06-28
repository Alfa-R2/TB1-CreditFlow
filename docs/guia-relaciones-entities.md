# Guia de relaciones entre entities

Esta guia explica como se mapean las relaciones entre entities con JPA en el
proyecto, por que algunas aparecen como foreign key en PostgreSQL y por que la
auditoria es un caso especial.

## 1. Diagnostico: columna vs relacion

PostgreSQL solo muestra una relacion entre tablas si existe una foreign key real.

En JPA, una columna `Long` llamada `cliente_id` o `solicitud_id` no crea por si
sola una relacion:

```java
@Column(name = "solicitud_id", nullable = false)
private Long solicitudId;
```

Eso crea solo una columna numerica (`solicitud_id bigint not null`), sin FK.

Para que Hibernate genere la relacion (y la FK) se necesita una asociacion:

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "solicitud_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_documento_solicitud"))
private Solicitud solicitud;
```

## 2. Enfoque actual: relaciones JPA puras

Las relaciones entre entities de negocio se modelan como **una sola asociacion
JPA** (sin columna `Long` duplicada). La asociacion **es** la dueña de la columna
FK y la escribe directamente.

Ejemplo (`Documento`):

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "solicitud_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_documento_solicitud"))
private Solicitud solicitud;
```

Por eso **ya no existen** `getSolicitudId()`, `getClienteId()` ni `getAsesorId()`
en esas entities. Se navega por la relacion:

```java
documento.getSolicitud();          // la entidad (proxy LAZY)
documento.getSolicitud().getId();  // el id — no dispara consulta (el proxy ya lo conoce)
```

Y los constructores reciben las **entidades**, no ids:

```java
new Solicitud(cliente, asesor, monto, plazoMeses, fechaRegistro);
new Documento(solicitud, tipo, urlArchivo, hash, fechaCarga);
new EvaluacionRiesgo(solicitud, capacidadPago, score, nivelRiesgo, justificacion, fecha);
```

> Nota historica: una version anterior uso un enfoque "hibrido" (`Long` + una
> asociacion de solo lectura con `insertable=false, updatable=false`). Se
> reemplazo por relaciones puras: menos codigo y una sola fuente de verdad por FK.

## 3. Relaciones mapeadas (con FK en BD)

| Tabla origen | Columna | Tabla destino | Tipo JPA |
| --- | --- | --- | --- |
| `usuario` | `rol_id` | `rol.id` | `@ManyToOne` |
| `solicitud` | `cliente_id` | `cliente.id` | `@ManyToOne` |
| `solicitud` | `asesor_id` | `usuario.id` | `@ManyToOne` |
| `documento` | `solicitud_id` | `solicitud.id` | `@ManyToOne` |
| `evaluacion_riesgo` | `solicitud_id` | `solicitud.id` | `@OneToOne` |

`evaluacion_riesgo` es `@OneToOne` porque `solicitud_id` es unico (una sola
evaluacion vigente por solicitud):

```java
@OneToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "solicitud_id", nullable = false, unique = true,
        foreignKey = @ForeignKey(name = "fk_evaluacion_solicitud"))
private Solicitud solicitud;
```

## 4. Caso especial: auditoria aislada (sin asociacion)

`RegistroAuditoria` **no** mapea una asociacion a `Solicitud`. Referencia la
solicitud solo por un `Long`:

```java
@Column(name = "solicitud_id", nullable = false)
private Long solicitudId;
```

Esto es **intencional**: el modulo `audit` debe permanecer **aislado** (§2 del
spec: "audit no depende de nadie"). Mapear `@ManyToOne Solicitud` obligaria a
`audit` a importar `origination`, rompiendo ese aislamiento.

Consecuencia: en PostgreSQL **no** habra FK
`registro_auditoria.solicitud_id -> solicitud.id`. La columna existe, pero la
integridad de auditoria no se delega a la BD: el registro es append-only y se
protege con el hash encadenado. Lo mismo aplica al `usuario` de la auditoria, que
se guarda como `String` (username estable), no como relacion.

## 5. Por que `FetchType.LAZY`

Las asociaciones son `LAZY`: no cargan la entidad relacionada hasta que se accede
a un campo distinto del id.

```java
solicitud.getCliente().getId();        // NO consulta (id conocido por el proxy)
solicitud.getCliente().getNombres();   // SI consulta (inicializa el proxy)
```

Como `open-in-view` esta en `false`, hay que acceder a campos no-id **dentro de
una transaccion**. El proyecto lo respeta devolviendo siempre DTOs (`*Response`)
construidos en la capa `@Transactional`, nunca la entity directa.

## 6. Efecto en tests

Con las FK reales `solicitud.cliente_id -> cliente.id` y
`solicitud.asesor_id -> usuario.id`, ya no es valido crear una `Solicitud` con
ids inventados. Por eso el helper de tests crea un cliente y un asesor reales:

```java
Cliente cliente = clienteRepository.save(...);
Usuario asesor  = usuarioDeRol(RolNombre.ASESOR);
solicitudRepository.save(new Solicitud(cliente, asesor, monto, plazo, fecha));
```

La auditoria, en cambio, **no** tiene FK; aun asi los tests registran auditoria
sobre solicitudes reales por coherencia del escenario, no por una restriccion de
BD.

## 7. Nota sobre bases ya creadas (FK y migraciones)

`application.yaml` usa:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: ${DDL_AUTO:update}
```

Con `update`, Hibernate intenta crear las FK nuevas, pero no es una herramienta
de migraciones completa. Si la base ya tiene datos, antes de crear las FK hay que
verificar que no existan registros huerfanos:

```text
solicitud.cliente_id que no exista en cliente.id
solicitud.asesor_id que no exista en usuario.id
documento.solicitud_id que no exista en solicitud.id
evaluacion_riesgo.solicitud_id que no exista en solicitud.id
```

(`registro_auditoria.solicitud_id` no lleva FK, asi que no aplica.)

En produccion, estas FK deberian crearse con una migracion controlada (Flyway o
Liquibase) tras limpiar datos invalidos.

## 8. Resumen mental

```text
Solicitud solicitud + @ManyToOne/@OneToOne + @JoinColumn
  -> relacion JPA + foreign key en BD + navegacion

Long solicitudId + @Column   (solo en RegistroAuditoria)
  -> columna numerica, SIN FK -> audit aislado (decision de arquitectura)

getCliente().getId()
  -> lee el id sin cargar la entidad relacionada (proxy LAZY)
```
