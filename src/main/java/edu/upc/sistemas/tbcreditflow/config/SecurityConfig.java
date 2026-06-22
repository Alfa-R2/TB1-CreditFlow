package edu.upc.sistemas.tbcreditflow.config;

import edu.upc.sistemas.tbcreditflow.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Seguridad JWT stateless y matriz de acceso por rol (§6). Cada endpoint exige el rol indicado;
 * acceso indebido ⇒ 403, sin token ⇒ 401.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          JwtAuthenticationEntryPoint authenticationEntryPoint,
                          RestAccessDeniedHandler accessDeniedHandler,
                          CorsConfigurationSource corsConfigurationSource) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Público
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        // Solicitudes / Documentos (ASESOR)
                        .requestMatchers(HttpMethod.POST, "/api/solicitudes").hasRole("ASESOR")
                        .requestMatchers(HttpMethod.GET, "/api/solicitudes").hasRole("ASESOR")
                        .requestMatchers(HttpMethod.POST, "/api/solicitudes/*/documentos").hasRole("ASESOR")
                        // Scoring (ANALISTA / COMITE)
                        .requestMatchers(HttpMethod.POST, "/api/solicitudes/*/evaluacion").hasRole("ANALISTA")
                        .requestMatchers(HttpMethod.GET, "/api/solicitudes/*/evaluacion").hasAnyRole("ANALISTA", "COMITE")
                        // Decisión (COMITE)
                        .requestMatchers(HttpMethod.POST, "/api/solicitudes/*/decision").hasRole("COMITE")
                        // Detalle de una solicitud: cualquier usuario autenticado
                        // (la matriz §6 no fija un rol para GET /api/solicitudes/{id})
                        .requestMatchers(HttpMethod.GET, "/api/solicitudes/*").authenticated()
                        // Resto: requiere autenticación
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
