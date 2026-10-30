package com.agencytrip.agencytripapi.infrastructure.adapter.in.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class JwtAudienceDebugTest {

    @Autowired private JwtEncoder jwtEncoder;
    @Autowired private SecretKey jwtKey;

    @Test
    @DisplayName("Verifica la codificacion y decodificacion correcta de claims de audiencia y emisor en tokens JWT")
    void verifyAudienceAndIssuerEncodingAndDecoding() {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("test-issuer")
                .audience(List.of("test-audience"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .subject("debug")
                .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        assertNotNull(token, "El token generado no debe ser nulo");
        assertFalse(token.isBlank(), "El token generado no debe estar en blanco");

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtKey).build();
        Jwt decoded = decoder.decode(token);
        assertNotNull(decoded, "El token decodificado no debe ser nulo");
        assertEquals(List.of("test-audience"), decoded.getAudience(), "La audiencia decodificada debe coincidir con la configurada");
        assertNotNull(decoded.getClaimAsString("iss"), "El emisor del token debe estar presente");
        assertEquals("test-issuer", decoded.getClaimAsString("iss"), "El emisor debe coincidir con test-issuer");
        assertEquals("debug", decoded.getSubject(), "El sujeto debe coincidir con debug");
    }
}