package com.animeplatform;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração global de CORS.
 *
 * Permite que qualquer origem (incluindo o browser quando o HTML é aberto
 * directamente via file:// ou via outro porto) consiga chamar a API.
 *
 * Cobre:
 *  - Preflight OPTIONS enviado pelo browser para POST com Content-Type: application/json
 *  - GET e POST nas rotas /api/**
 *  - Headers personalizados (Content-Type, Authorization)
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")   // aceita qualquer origem, incluindo file://
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD")
                .allowedHeaders("*")
                .exposedHeaders("Content-Type", "Authorization")
                .allowCredentials(false)
                .maxAge(3600);
    }

    /**
     * CorsFilter garante que o preflight OPTIONS é respondido ANTES
     * de qualquer outro filtro (ex: Spring Security, se vier a ser adicionado).
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(false);
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.addExposedHeader("Content-Type");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);

        return new CorsFilter(source);
    }
}
