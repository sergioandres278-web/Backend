package com.agencytrip.agencytripapi.infrastructure.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import com.agencytrip.agencytripapi.application.dto.GoogleUserPayload;
import com.agencytrip.agencytripapi.application.service.BusinessException;

class GoogleJwtTokenVerifierTests {

    private static final String CLIENT_ID = "563051774007-cbaen0fb0doko2cmt7p0n0vj4i21svv4.apps.googleusercontent.com";
    private JwtDecoder jwtDecoder;
    private GoogleJwtTokenVerifier verifier;

    @BeforeEach
    void setUp() {
        jwtDecoder = mock(JwtDecoder.class);
        verifier = new GoogleJwtTokenVerifier(jwtDecoder, CLIENT_ID);
    }

    private Jwt.Builder validJwtBuilder() {
        Instant now = Instant.now();
        return Jwt.withTokenValue("valid.id.token")
                .header("alg", "RS256")
                .header("kid", "key-1")
                .claim("iss", "https://accounts.google.com")
                .claim("aud", List.of(CLIENT_ID))
                .claim("sub", "google-sub-123456789")
                .claim("email", "usuario.google@gmail.com")
                .claim("email_verified", true)
                .claim("given_name", "Juan")
                .claim("family_name", "Perez")
                .claim("name", "Juan Perez")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600));
    }

    @Test
    @DisplayName("Token válido: Verifica la firma y extrae los datos de identidad correctamente")
    void validTokenReturnsGoogleUserPayload() {
        Jwt jwt = validJwtBuilder().build();
        when(jwtDecoder.decode("valid.id.token")).thenReturn(jwt);

        GoogleUserPayload payload = verifier.verify("valid.id.token");

        assertNotNull(payload);
        assertEquals("google-sub-123456789", payload.sub());
        assertEquals("usuario.google@gmail.com", payload.email());
        assertTrue(payload.emailVerified());
        assertEquals("Juan", payload.givenName());
        assertEquals("Perez", payload.familyName());
        assertEquals("Juan Perez", payload.name());
    }

    @Test
    @DisplayName("Token expirado: Lanza 401 Unauthorized")
    void expiredTokenThrowsUnauthorized() {
        when(jwtDecoder.decode("expired.id.token")).thenThrow(new BadJwtException("Jwt expired"));

        BusinessException ex = assertThrows(BusinessException.class, () -> verifier.verify("expired.id.token"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.status());
        assertTrue(ex.getMessage().contains("El token de Google es inválido o ha expirado"));
    }

    @Test
    @DisplayName("Token con firma inválida: Lanza 401 Unauthorized")
    void invalidSignatureThrowsUnauthorized() {
        when(jwtDecoder.decode("tampered.id.token")).thenThrow(new BadJwtException("Signature verification failed"));

        BusinessException ex = assertThrows(BusinessException.class, () -> verifier.verify("tampered.id.token"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.status());
        assertTrue(ex.getMessage().contains("El token de Google es inválido o ha expirado"));
    }

    @Test
    @DisplayName("Audiencia incorrecta: Lanza 401 Unauthorized")
    void wrongAudienceThrowsUnauthorized() {
        Jwt jwt = validJwtBuilder().claim("aud", List.of("wrong-client-id.apps.googleusercontent.com")).build();
        when(jwtDecoder.decode("wrong-aud.token")).thenReturn(jwt);

        BusinessException ex = assertThrows(BusinessException.class, () -> verifier.verify("wrong-aud.token"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.status());
        assertTrue(ex.getMessage().contains("La audiencia del token de Google no coincide"));
    }

    @Test
    @DisplayName("Emisor incorrecto: Lanza 401 Unauthorized")
    void wrongIssuerThrowsUnauthorized() {
        Jwt jwt = validJwtBuilder().claim("iss", "https://malicious-issuer.com").build();
        when(jwtDecoder.decode("wrong-iss.token")).thenReturn(jwt);

        BusinessException ex = assertThrows(BusinessException.class, () -> verifier.verify("wrong-iss.token"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.status());
        assertTrue(ex.getMessage().contains("Emisor de token de Google no válido"));
    }

    @Test
    @DisplayName("Correo no verificado: Lanza 400 Bad Request")
    void unverifiedEmailThrowsBadRequest() {
        Jwt jwt = validJwtBuilder().claim("email_verified", false).build();
        when(jwtDecoder.decode("unverified.token")).thenReturn(jwt);

        BusinessException ex = assertThrows(BusinessException.class, () -> verifier.verify("unverified.token"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.status());
        assertTrue(ex.getMessage().contains("El correo de la cuenta de Google no ha sido verificado"));
    }

    @Test
    @DisplayName("Token nulo o en blanco: Lanza 401 Unauthorized")
    void blankTokenThrowsUnauthorized() {
        assertThrows(BusinessException.class, () -> verifier.verify(""));
        assertThrows(BusinessException.class, () -> verifier.verify(null));
    }
}

