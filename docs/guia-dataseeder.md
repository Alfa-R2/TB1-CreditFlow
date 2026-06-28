# Guia de DataSeeder

Esta guia explica como se agrego `DataSeeder`, por que se ejecuta
automaticamente y que datos iniciales inserta.

## 1. Archivo principal

Archivo:

```text
src/main/java/edu/upc/sistemas/tbcreditflow/config/DataSeeder.java
```

La clase empieza asi:

```java
@Component
public class DataSeeder implements CommandLineRunner {
```

Estas dos piezas son la clave:

| Pieza | Significado |
| --- | --- |
| `@Component` | Spring descubre la clase y la registra como bean. |
| `CommandLineRunner` | Spring ejecuta su metodo `run(...)` al arrancar la aplicacion. |

## 2. Cuando se ejecuta

`DataSeeder` se ejecuta durante el arranque de Spring Boot, despues de que el
contexto de Spring ya fue creado y las dependencias fueron inyectadas.

Flujo:

```text
SpringApplication.run(...)
  -> crea ApplicationContext
  -> descubre @Component
  -> crea DataSeeder
  -> inyecta repositorios y PasswordEncoder
  -> ejecuta CommandLineRunner.run(...)
```

El metodo ejecutado es:

```java
@Override
@Transactional
public void run(String... args) {
    seedRoles();
    seedAdmin();
    seedReglas();
}
```

## 3. Dependencias que recibe

`DataSeeder` usa inyeccion por constructor:

```java
public DataSeeder(RolRepository rolRepository,
                  UsuarioRepository usuarioRepository,
                  ReglaScoringRepository reglaScoringRepository,
                  PasswordEncoder passwordEncoder) {
    ...
}
```

Responsabilidad de cada dependencia:

| Dependencia | Para que se usa |
| --- | --- |
| `RolRepository` | Buscar y crear roles. |
| `UsuarioRepository` | Verificar y crear el usuario admin. |
| `ReglaScoringRepository` | Verificar y crear reglas iniciales de scoring. |
| `PasswordEncoder` | Guardar la password del admin como hash BCrypt. |

`PasswordEncoder` viene de:

```text
config/SecurityConfig.java
```

Alli se declara:

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

## 4. Que datos inserta

El comentario del archivo resume su objetivo:

```text
- 7 roles
- 1 usuario admin
- 4 reglas de scoring semilla
```

### 4.1 Roles

Metodo:

```java
private void seedRoles() {
    for (RolNombre nombre : RolNombre.values()) {
        if (rolRepository.findByNombre(nombre).isEmpty()) {
            rolRepository.save(new Rol(nombre, descripcionDe(nombre)));
        }
    }
}
```

Los roles vienen del enum:

```text
src/main/java/edu/upc/sistemas/tbcreditflow/security/domain/RolNombre.java
```

Valores:

```java
ASESOR,
ANALISTA,
ADMIN_CREDITO,
COMITE,
CUMPLIMIENTO,
AUDITOR,
GERENTE
```

### 4.2 Usuario admin

Metodo:

```java
private void seedAdmin() {
    if (usuarioRepository.existsByUsername(ADMIN_USERNAME)) {
        return;
    }
    ...
}
```

Credenciales semilla:

```text
username: admin
password: admin
rol: ADMIN_CREDITO
estado: ACTIVO
```

La password no se guarda en texto plano. Se guarda con BCrypt:

```java
passwordEncoder.encode(ADMIN_PASSWORD)
```

### 4.3 Reglas de scoring

Metodo:

```java
private void seedReglas() {
    if (reglaScoringRepository.count() > 0) {
        return;
    }
    ...
}
```

Inserta cuatro reglas iniciales:

```text
Endeudamiento alto
Cuota alta sobre capacidad
Bajo endeudamiento
Cuota muy alta sobre capacidad
```

Estas reglas usan constantes del motor de scoring:

```text
ScoringEngine.RATIO_ENDEUDAMIENTO
ScoringEngine.RATIO_CUOTA
```

## 5. Por que es idempotente

El seeder esta escrito para poder ejecutarse mas de una vez sin duplicar datos.

Idempotencia por bloque:

| Bloque | Como evita duplicados |
| --- | --- |
| Roles | Busca cada rol por nombre antes de crearlo. |
| Admin | Usa `existsByUsername("admin")`. |
| Reglas | Solo crea reglas si `reglaScoringRepository.count() == 0`. |

Esto importa porque `DataSeeder` corre en cada arranque de la aplicacion.

## 6. Transaccion

El metodo `run(...)` tiene:

```java
@Transactional
```

Eso hace que las operaciones de seed se ejecuten dentro de una transaccion.

Si algo falla a mitad del proceso, Spring puede revertir los cambios de esa
transaccion en vez de dejar el bootstrap parcialmente aplicado.

## 7. Relacion con JPA y la base de datos

El seeder asume que las tablas ya existen o pueden ser creadas/actualizadas por
Hibernate segun la configuracion:

```text
src/main/resources/application.yaml
```

Configuracion:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: ${DDL_AUTO:update}
```

Con el valor por defecto `update`, Hibernate intenta adaptar el esquema de base
de datos antes de que el seeder inserte datos.

En tests se usa:

```text
src/test/resources/application-test.yaml
```

Con:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop
```

## 8. Se ejecuta tambien en tests

`DataSeeder` no tiene `@Profile`.

Eso significa que se ejecuta en cualquier perfil donde arranque el contexto de
Spring, incluido el perfil `test`.

En los tests de integracion:

```java
@SpringBootTest
@ActiveProfiles("test")
```

Spring usa H2 en memoria, crea las tablas y tambien ejecuta `DataSeeder`.

Esto ayuda porque los roles base existen cuando los tests crean usuarios por
rol.

## 9. Riesgos y cuidados

El usuario semilla:

```text
admin / admin
```

es util para desarrollo, pero no deberia tratarse como credencial segura de
produccion.

Si esta aplicacion fuera a produccion, opciones razonables serian:

- Desactivar el seeder con perfiles.
- Crear el admin por migracion controlada.
- Leer credenciales semilla desde variables de entorno.
- Forzar cambio de password inicial.

Actualmente, como no hay `@Profile`, el seeder corre siempre que la aplicacion
arranca.

## 10. Resumen mental

```text
@Component
  -> Spring registra DataSeeder como bean

CommandLineRunner
  -> Spring ejecuta run(...) al arrancar

@Transactional
  -> seed dentro de una transaccion

Repositorios
  -> insertan roles, admin y reglas

Checks previos
  -> evitan duplicados
```

