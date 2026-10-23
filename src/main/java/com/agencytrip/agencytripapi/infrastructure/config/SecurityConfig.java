package com.agencytrip.agencytripapi.infrastructure.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.proc.SecurityContext;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.core.Authentication;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecretKey jwtKey(
            @Value("${agencytrip.jwt.secret:ABCDEFGHIJKLMNOPQRSTUVWXYZ123456}") String secret) {

        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET debe configurarse con al menos 32 caracteres."
            );
        }

        return new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
    }

    @Bean
    @Primary
    JwtDecoder jwtDecoder(SecretKey key,
                         @Value("${agencytrip.jwt.issuer:AgencyTripAPI}") String issuer,
                         @Value("${agencytrip.jwt.audience:AgencyTripAngular}") String audience) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).build();
        OAuth2TokenValidator<Jwt> defaultValidator = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<Object>(
                "aud",
                aud -> aud instanceof List<?> list ? list.contains(audience) : audience.equals(aud)
        );
        OAuth2TokenValidator<Jwt> combined = new DelegatingOAuth2TokenValidator<>(defaultValidator, audienceValidator);
        decoder.setJwtValidator(combined);
        return decoder;
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey key) {
        return new NimbusJwtEncoder(
                new com.nimbusds.jose.jwk.source.ImmutableSecret<SecurityContext>(key)
        );
    }

    @Bean
    SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtDecoder decoder,
            JwtAuthenticationConverter converter) throws Exception {

        return http
                .csrf(csrf -> csrf.disable())

                .cors(Customizer.withDefaults())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // Registro y login públicos
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/Usuarios/registro",
                                "/api/Usuarios/login",
                                "/api/Registro"
                        ).permitAll()

                        // Consultas públicas
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/Destinos",
                                "/api/Paquetes",
                                "/api/Hoteles",
                                "/api/Transportes"
                        ).permitAll()

                        // Swagger / OpenAPI públicos
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // Administración
                        .requestMatchers(
                                "/api/Admin/**",
                                "/api/Roles/**"
                        ).hasRole("ADMINISTRADOR")

                        // Reservas requieren autenticación basada en roles
                        .requestMatchers(HttpMethod.GET, "/api/Reservas").authenticated()
                        // Todo lo demás requiere autenticación
                        .anyRequest().authenticated()
                )

                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt
                                        .decoder(decoder)
                                        .jwtAuthenticationConverter(converter)
                        )
                )

                .build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter roles =
                new JwtGrantedAuthoritiesConverter();

        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("");

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(roles);

        return converter;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        // Variable de entorno que puede contener varios orígenes separados por comas.
        // Si está vacía o no se define, se usa el origen local por defecto.
        String allowedOrigins = System.getenv("AGENCYTRIP_CORS_ALLOWED_ORIGIN");
        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            allowedOrigins = "http://localhost:4200";
        }
        // Eliminar espacios y crear lista de orígenes.
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        if (origins.isEmpty()) {
            origins = List.of("http://localhost:4200");
        }
        configuration.setAllowedOrigins(origins);

        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }
}