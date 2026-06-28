# Guia de SecurityConfig y flujo JWT

Esta guia explica el funcionamiento de `SecurityConfig`, los componentes con
los que se relaciona y el flujo completo de autenticacion/autorizacion en el
backend.

## 1. Archivo principal

Archivo:

```text
src/main/java/edu/upc/sistemas/tbcreditflow/config/SecurityConfig.java
```

`SecurityConfig` define el bean central de seguridad:

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    ...
}
```

Ese `SecurityFilterChain` decide:

- Que rutas son publicas.
- Que rutas requieren autenticacion.
- Que rol necesita cada endpoint.
- Que filtro JWT se ejecuta antes de llegar al controller.
- Como responder ante errores `401` y `403`.
- Como aplicar CORS.
- Que politica de sesion usa la aplicacion.

## 2. Componentes relacionados

`SecurityConfig` no contiene toda la logica de autenticacion. Su trabajo
principal es armar la cadena de seguridad y conectar varios componentes:

| Componente | Archivo | Responsabilidad |
| --- | --- | --- |
| `SecurityConfig` | `config/SecurityConfig.java` | Define la cadena de filtros, rutas publicas, rutas protegidas, roles, CORS, manejo de errores y politica stateless. |
| `JwtAuthenticationFilter` | `security/JwtAuthenticationFilter.java` | Lee el header `Authorization`, valida el JWT y llena el `SecurityContextHolder`. |
| `JwtService` | `security/service/JwtService.java` | Genera, parsea y valida tokens JWT usando `jwt.secret` y `jwt.expiration`. |
| `JwtAuthenticationEntryPoint` | `config/JwtAuthenticationEntryPoint.java` | Devuelve `401 Unauthorized` cuando no hay autenticacion valida. |
| `RestAccessDeniedHandler` | `config/RestAccessDeniedHandler.java` | Devuelve `403 Forbidden` cuando el usuario esta autenticado pero no tiene el rol requerido. |
| `CorsConfig` | `config/CorsConfig.java` | Crea el bean `CorsConfigurationSource` usado por `SecurityConfig`. |
| `AuthController` | `security/controller/AuthController.java` | Expone `POST /api/auth/login`. |
| `AuthService` | `security/service/AuthService.java` | Valida usuario/password con BCrypt y emite el JWT. |
| `UsuarioService` | `security/service/UsuarioService.java` | Lee el usuario autenticado desde `SecurityContextHolder` y lo resuelve contra base de datos. |

## 3. Como se usa `CorsConfig`

`CorsConfig` no se usa con una llamada directa tipo:

```java
new CorsConfig()
```

Tampoco se importa como una utilidad. Se usa de forma indirecta por el contenedor
de Spring.

La clase esta marcada con:

```java
@Configuration
public class CorsConfig {
```

Y expone este bean:

```java
@Bean
public CorsConfigurationSource corsConfigurationSource(...) {
    ...
}
```

Eso significa que, al arrancar la aplicacion, Spring ejecuta ese metodo y guarda
el resultado como un bean de tipo:

```java
CorsConfigurationSource
```

Luego `SecurityConfig` pide una dependencia de ese mismo tipo en su constructor:

```java
public SecurityConfig(...,
                      CorsConfigurationSource corsConfigurationSource) {
    this.corsConfigurationSource = corsConfigurationSource;
}
```

Y la conecta aqui:

```java
.cors(cors -> cors.configurationSource(corsConfigurationSource))
```

Flujo:

```text
CorsConfig
  -> crea bean CorsConfigurationSource

Spring container
  -> guarda ese bean

SecurityConfig
  -> recibe CorsConfigurationSource por constructor
  -> lo entrega a Spring Security con .cors(...)
```

En otras palabras: `CorsConfig` si se usa, pero no aparece como llamada directa.
Spring lo descubre por `@Configuration`, registra su `@Bean` y luego lo inyecta
en `SecurityConfig`.

La configuracion concreta permite:

```text
Origins: definidos por app.cors.allowed-origins
Methods: GET, POST, PUT, DELETE, OPTIONS
Headers: *
Credentials: true
Path: /**
```

## 4. Que configura `SecurityConfig`

La clase esta marcada con:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
```

Eso indica que esta clase aporta configuracion de Spring Security.

Sus dependencias llegan por constructor:

```java
public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                      JwtAuthenticationEntryPoint authenticationEntryPoint,
                      RestAccessDeniedHandler accessDeniedHandler,
                      CorsConfigurationSource corsConfigurationSource) {
    ...
}
```

Esas dependencias ya existen como beans porque sus clases usan `@Component`,
`@Service`, `@Configuration` o `@Bean`.

Tambien declara este bean:

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

Ese `PasswordEncoder` se usa en `AuthService` para comparar el password recibido
en login contra el hash guardado en base de datos.

## 5. Flujo de login

Ruta publica:

```text
POST /api/auth/login
```

En `SecurityConfig` aparece como:

```java
.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
```

Eso significa que se puede llamar sin JWT.

Flujo:

```text
Cliente
  -> POST /api/auth/login
    -> AuthController.login(...)
      -> AuthService.login(...)
        -> UsuarioRepository.findByUsername(...)
        -> PasswordEncoder.matches(...)
        -> JwtService.generateToken(...)
          -> respuesta TokenResponse { token, rol }
```

El token generado contiene:

| Dato | Donde va en el JWT |
| --- | --- |
| `username` | `subject` |
| `rol` | claim llamado `rol` |
| expiracion | `expiration` |
| firma | HS256 con `jwt.secret` |

Luego el cliente debe enviar ese token en las rutas protegidas:

```http
Authorization: Bearer <token>
```

## 6. Flujo de una request protegida

Ejemplo:

```text
POST /api/solicitudes
```

En `SecurityConfig` esa ruta exige rol:

```java
.requestMatchers(HttpMethod.POST, "/api/solicitudes").hasRole("ASESOR")
```

Flujo:

```text
Cliente
  -> POST /api/solicitudes
     Header: Authorization: Bearer <token>

SecurityFilterChain
  -> JwtAuthenticationFilter
      -> lee Authorization
      -> extrae el token despues de "Bearer "
      -> JwtService.isValid(token)
      -> JwtService.extractUsername(token)
      -> JwtService.extractRol(token)
      -> crea UsernamePasswordAuthenticationToken
      -> crea authority ROLE_<rol>
      -> guarda la autenticacion en SecurityContextHolder

Spring Security
  -> compara la ruta contra las reglas de SecurityConfig
  -> verifica que exista ROLE_ASESOR

DispatcherServlet
  -> SolicitudController.crear(...)
```

Detalle importante:

```java
new SimpleGrantedAuthority("ROLE_" + rol)
```

Spring Security usa el prefijo `ROLE_` para las reglas `hasRole(...)`.
Por eso:

```java
hasRole("ASESOR")
```

realmente espera encontrar:

```text
ROLE_ASESOR
```

El filtro construye esa autoridad desde el rol guardado en el token.

## 7. Matriz de autorizacion

Las reglas de `authorizeHttpRequests(...)` funcionan como una matriz de acceso:

| Regla | Resultado |
| --- | --- |
| `permitAll()` | No exige autenticacion. |
| `authenticated()` | Exige cualquier usuario autenticado. |
| `hasRole("ASESOR")` | Exige authority `ROLE_ASESOR`. |
| `hasAnyRole("ANALISTA", "COMITE")` | Exige cualquiera de esas authorities. |
| `anyRequest().authenticated()` | Regla final: todo lo no declarado antes requiere autenticacion. |

En este proyecto:

| Endpoint | Regla |
| --- | --- |
| `POST /api/auth/login` | Publico |
| Swagger/OpenAPI | Publico |
| `POST /api/solicitudes` | `ASESOR` |
| `GET /api/solicitudes` | `ASESOR` |
| `POST /api/solicitudes/*/documentos` | `ASESOR` |
| `GET /api/solicitudes/*/estado` | `ASESOR` |
| `POST /api/solicitudes/*/evaluacion` | `ANALISTA` |
| `GET /api/solicitudes/*/evaluacion` | `ANALISTA` o `COMITE` |
| `POST /api/solicitudes/*/decision` | `COMITE` |
| `/api/reglas`, `/api/reglas/**` | `ADMIN_CREDITO` |
| `GET /api/auditoria` | `AUDITOR` o `CUMPLIMIENTO` |
| `GET /api/reportes/**` | `GERENTE` |
| `GET /api/solicitudes/*` | Cualquier usuario autenticado |
| Cualquier otra ruta | Cualquier usuario autenticado |

## 8. Diferencia entre 401 y 403

`SecurityConfig` conecta dos handlers distintos:

```java
.exceptionHandling(ex -> ex
        .authenticationEntryPoint(authenticationEntryPoint)
        .accessDeniedHandler(accessDeniedHandler))
```

La diferencia practica:

| Caso | Handler | HTTP |
| --- | --- | --- |
| No se envio token | `JwtAuthenticationEntryPoint` | `401 Unauthorized` |
| Token invalido o expirado | `JwtAuthenticationEntryPoint` | `401 Unauthorized` |
| Token valido, pero rol incorrecto | `RestAccessDeniedHandler` | `403 Forbidden` |

Ejemplos:

```text
Sin Authorization header
  -> 401

Authorization: Bearer token_invalido
  -> 401

Token valido con rol ASESOR llamando POST /api/reglas
  -> 403
```

## 9. Por que la aplicacion es stateless

En `SecurityConfig` aparece:

```java
.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```

Eso significa que el backend no guarda sesion HTTP del usuario. Cada request
protegida debe traer su JWT.

Consecuencia:

```text
No hay sesion de servidor
No hay login persistido en memoria del backend
Cada request se autentica con Authorization: Bearer <token>
```

## 10. Que no hace `JwtAuthenticationFilter`

El comentario del propio filtro es importante: no accede a la base de datos.

El filtro solo confia en el contenido del JWT si:

- La firma es valida.
- El token no expiro.
- El claim `rol` existe y se puede leer.

Luego crea una autenticacion en memoria para esa request:

```text
username + ROLE_<rol>
```

Si algun servicio necesita el objeto completo del usuario, usa:

```text
security/service/UsuarioService.java
```

Ese servicio lee el username desde `SecurityContextHolder` y recien ahi consulta
`UsuarioRepository`.

## 11. Orden recomendado para depurar seguridad

Cuando una llamada falla por seguridad, revisar en este orden:

1. Endpoint y metodo HTTP exactos.

```text
POST /api/solicitudes
GET /api/reportes/indicadores
```

2. Regla correspondiente en:

```text
config/SecurityConfig.java
```

3. Si la ruta es publica, protegida o requiere rol especifico.

4. Header enviado por el cliente:

```http
Authorization: Bearer <token>
```

5. Contenido del token:

```text
subject = username
claim rol = ASESOR / ANALISTA / COMITE / ...
```

6. Authority creada por el filtro:

```text
ROLE_<rol>
```

7. Diferenciar el error:

```text
401 = no hay autenticacion valida
403 = hay autenticacion, pero el rol no alcanza
```
