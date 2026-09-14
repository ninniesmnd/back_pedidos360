package com.ventas.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                // Catálogo: cualquier rol autenticado puede consultarlo (lo necesita el Cliente para armar su pedido).
                .requestMatchers(HttpMethod.GET, "/api/productos", "/api/productos/*").hasAnyAuthority(
                        "APPROLE_Cliente", "APPROLE_OperadorCocina", "APPROLE_Repartidor",
                        "APPROLE_AdminLocal", "APPROLE_AdminGeneral")
                .requestMatchers(HttpMethod.POST, "/api/productos").hasAnyAuthority(
                        "APPROLE_AdminLocal", "APPROLE_AdminGeneral")
                .requestMatchers(HttpMethod.PUT, "/api/productos/*").hasAnyAuthority(
                        "APPROLE_AdminLocal", "APPROLE_AdminGeneral")
                .requestMatchers(HttpMethod.PATCH, "/api/productos/*/estado").hasAnyAuthority(
                        "APPROLE_AdminLocal", "APPROLE_AdminGeneral")
                // Cocina puede rebajar stock al preparar (ej. se acaba un ingrediente).
                .requestMatchers(HttpMethod.PATCH, "/api/productos/*/stock").hasAnyAuthority(
                        "APPROLE_AdminLocal", "APPROLE_AdminGeneral", "APPROLE_OperadorCocina")
                .requestMatchers("/api/ventas/**").hasAnyAuthority(
                        "APPROLE_AdminLocal", "APPROLE_AdminGeneral")
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthorityPrefix("APPROLE_");
        authorities.setAuthoritiesClaimName("roles");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200", "https://52.71.213.234"));
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}