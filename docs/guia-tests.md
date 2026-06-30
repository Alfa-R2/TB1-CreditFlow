# Guia de tests y cobertura

Esta guia explica como esta armado el sistema de pruebas del proyecto, como
ejecutarlo, como leer los tests existentes y como interpretar el reporte de
JaCoCo.

## 1. Archivos principales

Configuracion Maven:

```text
pom.xml
```

Dependencias de test:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

Configuracion del perfil de pruebas:

```text
src/test/resources/application-test.yaml
```

Alli se define:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:crediflow;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
```

Esto significa:

```text
Los tests usan una base H2 en memoria.
H2 intenta comportarse como PostgreSQL con MODE=PostgreSQL.
Hibernate crea y elimina el esquema para el perfil test.
```

## 2. Comandos utiles

Ejecutar todos los tests:

```powershell
.\mvnw.cmd test
```

Ejecutar tests y generar reporte JaCoCo:

```powershell
.\mvnw.cmd verify
```

Ejecutar desde cero:

```powershell
.\mvnw.cmd clean verify
```

Ejecutar una clase puntual:

```powershell
.\mvnw.cmd -Dtest=ScoringEngineTest test
```

Ejecutar varias clases en PowerShell:

```powershell
.\mvnw.cmd "-Dtest=GlobalExceptionHandlerTest,HashUtilTest" test
```

Abrir el reporte de cobertura:

```powershell
Start-Process .\target\site\jacoco\index.html
```

## 3. Tipos de tests en el proyecto

El proyecto mezcla dos estilos:

| Tipo | Caracteristica | Ejemplos |
| --- | --- | --- |
| Unitario | No levanta Spring completo; prueba una clase o logica aislada | `ScoringEngineTest`, `HashUtilTest`, `GlobalExceptionHandlerTest` |
| Integracion | Levanta contexto Spring, seguridad, repositorios y MVC | `SolicitudIntegrationTest`, `AuthIntegrationTest`, `ScoringIntegrationTest` |

Regla practica:

```text
Si solo necesitas probar calculos, mapping o handlers sin servidor, usa test unitario.
Si necesitas probar endpoint + seguridad + persistencia, usa test de integracion.
```

## 4. Base de tests de integracion

Archivo:

```text
src/test/java/edu/upc/sistemas/tbcreditflow/support/AbstractIntegrationTest.java
```

La clase base tiene estas anotaciones:

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class AbstractIntegrationTest {
}
```

Que significa cada una:

| Anotacion | Funcion |
| --- | --- |
| `@SpringBootTest` | Levanta el contexto completo de Spring Boot |
| `@AutoConfigureMockMvc` | Habilita `MockMvc` para llamar endpoints sin abrir servidor real |
| `@ActiveProfiles("test")` | Usa `application-test.yaml` |
| `@Transactional` | Cada test corre en transaccion y se revierte al final |

Esto es parecido a tener una app FastAPI en memoria con un `TestClient`, pero
con el contexto completo de Spring: controllers, services, repositories,
seguridad y base de datos de prueba.

## 5. MockMvc

`MockMvc` permite ejecutar requests HTTP contra los controllers sin arrancar
Tomcat en un puerto real.

Ejemplo:

```java
mockMvc.perform(post("/api/solicitudes")
                .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ASESOR))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "tipoDoc": "DNI",
                          "numDoc": "12345678",
                          "nombres": "Ana",
                          "apellidos": "Perez",
                          "ingresoMensual": 4200,
                          "deudasActuales": 1750,
                          "monto": 30000,
                          "plazoMeses": 24
                        }
                        """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("REGISTRADA"));
```

Partes importantes:

```text
perform(...) ejecuta la request.
header(...) agrega el JWT.
contentType(...) declara JSON.
content(...) envia el body.
andExpect(...) valida status y respuesta.
jsonPath(...) inspecciona campos del JSON.
```

## 6. JWT y roles en tests

`AbstractIntegrationTest` tiene el helper:

```java
protected String bearer(RolNombre rolNombre) {
    usuarioDeRol(rolNombre);
    return "Bearer " + jwtService.generateToken("user_" + rolNombre.name(), rolNombre.name());
}
```

Ese metodo:

```text
Crea un usuario activo para el rol si no existe.
Genera un JWT valido con ese rol.
Devuelve el valor listo para Authorization.
```

Uso tipico:

```java
.header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ANALISTA))
```

Asi los tests verifican seguridad real, no solo logica del controller.

## 7. Datos de prueba

El helper mas usado es:

```java
protected Solicitud crearSolicitud(BigDecimal ingreso, BigDecimal deudas,
                                   BigDecimal monto, int plazoMeses)
```

Crea:

```text
Cliente real.
Usuario asesor real.
Solicitud real asociada a ambos.
```

Esto es importante porque las entities usan relaciones JPA reales:

```java
new Solicitud(cliente, asesor, monto, plazoMeses, LocalDateTime.now())
```

No se deben inventar ids manualmente para relaciones como `cliente_id` o
`asesor_id`.

## 8. Mapa de tests

| Clase | Tipo | Que valida |
| --- | --- | --- |
| `TbCreditFlowApplicationTests` | Integracion minima | El contexto Spring levanta |
| `AuthIntegrationTest` | Integracion | Login, credenciales y token |
| `SecurityAccessTest` | Integracion | Reglas de acceso por rol |
| `SolicitudIntegrationTest` | Integracion | Registro, consulta y cambios de estado de solicitudes |
| `DocumentoIntegrationTest` | Integracion | Carga de documentos y validaciones |
| `ScoringEngineTest` | Unitario | Calculo de capacidad, score y riesgo |
| `ScoringIntegrationTest` | Integracion | Evaluacion desde endpoint y restricciones |
| `ReglaScoringIntegrationTest` | Integracion | CRUD/activacion de reglas de scoring |
| `AuditServiceTest` | Integracion | Integridad de hash de auditoria |
| `DecisionAuditIntegrationTest` | Integracion | Flujo decision + auditoria |
| `AuditoriaConsultaIntegrationTest` | Integracion | Consulta de auditoria por filtros |
| `ReporteIntegrationTest` | Integracion | Reportes operativos |
| `OpenApiDocsTest` | Integracion | Exposicion de `/v3/api-docs` |
| `GlobalExceptionHandlerTest` | Unitario | Traduccion de excepciones a `ApiError` |
| `HashUtilTest` | Unitario | SHA-256 para texto y bytes |

## 9. Tests de `common`

Los tests agregados para subir cobertura de `common` estan en:

```text
src/test/java/edu/upc/sistemas/tbcreditflow/common/GlobalExceptionHandlerTest.java
src/test/java/edu/upc/sistemas/tbcreditflow/common/HashUtilTest.java
```

`GlobalExceptionHandlerTest` instancia directamente el handler:

```java
private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
private final HttpServletRequest request = new MockHttpServletRequest("GET", "/api/prueba");
```

No levanta Spring completo porque el objetivo no es probar el framework, sino el
contrato propio del handler:

```text
status HTTP correcto.
mensaje correcto.
error uniforme ApiError.
path correcto.
timestamp presente.
```

Tambien cubre ramas internas:

```text
Errores de campo con mensaje.
Errores de campo sin defaultMessage.
Validacion sin field errors, usando fallback generico.
```

`HashUtilTest` valida hashes conocidos:

```java
HashUtil.sha256("abc")
```

contra el SHA-256 esperado:

```text
ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad
```

## 10. JaCoCo

JaCoCo esta configurado en `pom.xml`.

El plugin hace tres cosas:

```text
prepare-agent: conecta JaCoCo antes de correr tests.
report: genera HTML/CSV/XML en verify.
check: falla el build si no se cumple el minimo de cobertura.
```

El umbral actual es:

```xml
<counter>LINE</counter>
<value>COVEREDRATIO</value>
<minimum>0.95</minimum>
```

Eso significa:

```text
El build debe tener al menos 95% de lineas cubiertas.
```

Reporte HTML:

```text
target/site/jacoco/index.html
```

CSV para inspeccion rapida:

```text
target/site/jacoco/jacoco.csv
```

## 11. Como leer JaCoCo

Columnas principales:

| Columna | Significado |
| --- | --- |
| `Instructions` | Instrucciones bytecode ejecutadas |
| `Branches` | Ramas de `if`, ternarios, `switch`, etc. |
| `Lines` | Lineas de codigo fuente ejecutadas |
| `Methods` | Metodos ejecutados |
| `Classes` | Clases ejecutadas |

Para mejorar cobertura real, normalmente conviene mirar primero:

```text
Branches bajos.
Lineas rojas en services.
Handlers de errores no probados.
Validaciones de negocio sin test negativo.
```

No siempre vale la pena perseguir 100%. Ejemplo: en `HashUtil`, el `catch` de
`NoSuchAlgorithmException` es defensivo porque `SHA-256` forma parte del JDK.
Forzarlo haria el test mas fragil que util.

## 12. Limitaciones de H2

Aunque H2 corre en `MODE=PostgreSQL`, no es PostgreSQL real.

Puede pasar que un test en H2 funcione y PostgreSQL falle por diferencias de:

```text
Inferencia de tipos.
Consultas con parametros null.
Funciones SQL.
Restricciones especificas.
Tipos de datos.
```

Ejemplo real del proyecto:

```sql
where (? is null or fecha >= ?)
```

H2 lo toleraba, pero PostgreSQL podia fallar con:

```text
could not determine data type of parameter
```

Por eso, si un bug aparece solo "en caliente" contra PostgreSQL, no hay que
descartarlo solo porque H2 pase.

## 13. Como agregar un nuevo test

Para logica pura:

```text
Crear una clase `*Test`.
Instanciar la clase directamente.
Usar AssertJ con `assertThat`.
No extender `AbstractIntegrationTest`.
```

Ejemplo:

```java
class MiServicioUnitTest {

    @Test
    void casoEsperado() {
        Resultado r = servicio.calcular(...);

        assertThat(r.valor()).isEqualTo(...);
    }
}
```

Para endpoint o seguridad:

```text
Crear una clase `*IntegrationTest`.
Extender `AbstractIntegrationTest`.
Usar `mockMvc`.
Usar `bearer(RolNombre.X)` si requiere autenticacion.
Preparar datos con helpers o repositorios.
```

Ejemplo:

```java
class MiEndpointIntegrationTest extends AbstractIntegrationTest {

    @Test
    void rolNoAutorizado_403() throws Exception {
        mockMvc.perform(get("/api/recurso")
                        .header(HttpHeaders.AUTHORIZATION, bearer(RolNombre.ASESOR)))
                .andExpect(status().isForbidden());
    }
}
```

## 14. Criterio practico

Un buen test en este proyecto deberia responder una de estas preguntas:

```text
La regla de negocio se calcula correctamente?
El endpoint devuelve el status esperado?
La seguridad permite/deniega el rol correcto?
La validacion convierte errores en ApiError uniforme?
La persistencia respeta las relaciones reales entre entities?
El flujo completo deja auditoria o estado correcto?
```

Si el test solo ejecuta codigo para subir porcentaje, pero no valida
comportamiento relevante, probablemente no vale la pena.
