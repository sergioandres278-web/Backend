package com.agencytrip.agencytripapi.infrastructure.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@SpringBootTest
class JwtAudienceDebugTest {

    @Autowired private JwtEncoder jwtEncoder;
    @Autowired private SecretKey jwtKey;

    @Test
    void printAudience() {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("test-issuer")
                .audience(List.of("test-audience"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .subject("debug")
                .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        System.out.println("Generated token: " + token);

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtKey).build();
        Jwt decoded = decoder.decode(token);
        System.out.println("Decoded audience: " + decoded.getAudience());
    }
}
