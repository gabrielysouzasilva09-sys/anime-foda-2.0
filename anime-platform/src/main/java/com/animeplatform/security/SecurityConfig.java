package com.animeplatform.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Configuração do Spring Security com autenticação por JWT.
 *
 *  - Sessões stateless (sem cookies de sessão)
 *  - CSRF desactivado (API REST consumida por fetch/JWT)
 *  - Endpoints de leitura (GET) públicos
 *  - Escrita (POST) exige token JWT válido
 *  - Passwords encriptadas com BCrypt
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          CorsConfigurationSource corsConfigurationSource) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Preflight CORS sempre permitido
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Registo e login não exigem token
                .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()

                // Leitura pública de resenhas, animes e temporadas
                .requestMatchers(HttpMethod.GET,
                        "/api/reviews/**", "/api/animes/**", "/api/seasons/**").permitAll()

                // Qualquer outra rota da API (POST/DELETE, /api/auth/me) exige JWT
                .requestMatchers("/api/**").authenticated()

                // Front-end estático e restantes recursos
                .anyRequest().permitAll()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
