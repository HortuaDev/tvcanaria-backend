package com.tvcanaria.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import com.tvcanaria.security.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CorsConfigurationSource corsConfigurationSource) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. Rutas públicas (Login, Registro, etc.)
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/articles/**").permitAll() // Por si las noticias son
                                                                                         // públicas
                        // 2. Rutas de Gestión de Artículos (ADMIN y REPORTER)
                        .requestMatchers(HttpMethod.POST, "/api/articles/upload").hasAnyAuthority("ADMIN", "REPORTER")
                        .requestMatchers(HttpMethod.DELETE, "/api/articles/{id}").hasAnyAuthority("ADMIN", "REPORTER")
                        .requestMatchers(HttpMethod.PUT, "/api/articles/{id}").hasAnyAuthority("ADMIN", "REPORTER")

                        // 3. Rutas de Administración pura
                        .requestMatchers("/api/admin/**").hasAuthority("ADMIN")

                        // 4. El endpoint de todos los usuarios para ADMIN (el que añadimos antes)
                        .requestMatchers(HttpMethod.GET, "/api/users").hasAuthority("ADMIN")

                        // 5. Rutas de Usuario (Perfil, etc.) - Requieren estar logueado
                        .requestMatchers("/api/users/**").authenticated()

                        // 6. CUALQUIER OTRA COSA (SIEMPRE AL FINAL)
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}