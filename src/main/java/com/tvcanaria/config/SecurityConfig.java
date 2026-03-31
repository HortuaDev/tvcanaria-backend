package com.tvcanaria.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. PUBLIC: Auth y Registro
                        .requestMatchers("/api/auth/**").permitAll()

                        // 2. ARTICLES: Reglas específicas antes de la general
                        .requestMatchers(HttpMethod.GET, "/api/articles/my-articles")
                        .hasAnyAuthority("ADMIN", "REPORTER")
                        .requestMatchers(HttpMethod.GET, "/api/articles/recommended").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/articles/upload").hasAnyAuthority("ADMIN", "REPORTER")
                        .requestMatchers(HttpMethod.PUT, "/api/articles/**").hasAnyAuthority("ADMIN", "REPORTER")
                        .requestMatchers(HttpMethod.DELETE, "/api/articles/{id}").hasAnyAuthority("ADMIN", "REPORTER")
                        .requestMatchers(HttpMethod.GET, "/api/articles/**").permitAll()

                        // 3. COMMENTS: Moderación y Participación
                        .requestMatchers(HttpMethod.GET, "/api/comments/reported").hasAnyAuthority("ADMIN", "MODERATOR")
                        .requestMatchers(HttpMethod.PUT, "/api/comments/**").hasAnyAuthority("ADMIN", "MODERATOR")
                        .requestMatchers(HttpMethod.POST, "/api/comments/**").authenticated() // Crear y reportar
                        .requestMatchers(HttpMethod.DELETE, "/api/comments/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/comments/**").permitAll()

                        // 4. MODERATORS: Sistema de solicitudes
                        .requestMatchers(HttpMethod.GET, "/api/moderators/*/reporters")
                        .hasAnyAuthority("ADMIN", "MODERATOR")
                        .requestMatchers(HttpMethod.GET, "/api/moderators/reporters/*")
                        .hasAnyAuthority("ADMIN", "REPORTER")
                        .requestMatchers("/api/moderators/requests/pending").hasAnyAuthority("MODERATOR", "READER")
                        .requestMatchers("/api/moderators/requests/my-requests").hasAuthority("REPORTER")
                        .requestMatchers(HttpMethod.POST, "/api/moderators/requests").hasAuthority("REPORTER")
                        .requestMatchers("/api/moderators/**").authenticated()

                        // 5. USERS: Perfil y Gestión
                        .requestMatchers(HttpMethod.GET, "/api/users").hasAuthority("ADMIN")
                        .requestMatchers("/api/users/profile").authenticated()
                        .requestMatchers("/api/users/**").authenticated()

                        // 6. ADMIN: Rutas de administración pura
                        .requestMatchers("/api/admin/**").hasAuthority("ADMIN")

                        // 7. CUALQUIER OTRA RUTA
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}