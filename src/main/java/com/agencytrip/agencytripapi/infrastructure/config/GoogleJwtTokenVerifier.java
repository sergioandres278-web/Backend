package com.agencytrip.agencytripapi.infrastructure.config;

import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import com.agencytrip.agencytripapi.application.dto.GoogleUserPayload;
import com.agencytrip.agencytripapi.application.port.out.GoogleTokenVerifierPort;
import com.agencytrip.agencytripapi.application.service.BusinessException;

@Component
public class GoogleJwtTokenVerifier implements GoogleTokenVerifierPort {

    private final JwtDecoder jwtDecoder;
    private final String expectedClientId;

    @Autowired
    public GoogleJwtTokenVerifier(
            @Value("${agencytrip.oauth.google.jwk-set-uri:https://www.googleapis.com/oauth2/v3/certs}") String jwkSetUri,
            @Value("${agencytrip.oauth.google.client-id:563051774007-cbaen0fb0doko2cmt7p0n0vj4i21svv4.apps.googleusercontent.com}") String clientId) {
        this(NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build(), clientId);
    }

    public GoogleJwtTokenVerifier(JwtDecoder jwtDecoder, String expectedClientId) {
        this.jwtDecoder = Objects.requireNonNull(jwtDecoder, "jwtDecoder cannot be null");
        this.expectedClientId = Objects.requireNonNull(expectedClientId, "expectedClientId cannot be null");
    }

    @Override
    public GoogleUserPayload verify(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "El ID Token de Google es obligatorio.");
        }

        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(idToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "El token de Google es inválido o ha expirado.");
        }

        String issuer = jwt.getIssuer() != null ? jwt.getIssuer().toString() : "";
        if (!"https://accounts.google.com".equals(issuer) && !"accounts.google.com".equals(issuer)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Emisor de token de Google no válido: " + issuer);
        }

        List<String> audience = jwt.getAudience();
        if (audience == null || !audience.contains(expectedClientId)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "La audiencia del token de Google no coincide.");
        }

        Object emailVerifiedObj = jwt.getClaims().get("email_verified");
        boolean emailVerified = Boolean.TRUE.equals(emailVerifiedObj)
                || "true".equalsIgnoreCase(String.valueOf(emailVerifiedObj));
        if (!emailVerified) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "El correo de la cuenta de Google no ha sido verificado.");
        }

        String sub = jwt.getSubject();
        if (sub == null || sub.isBlank()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "El token de Google no contiene el identificador de usuario (sub).");
        }

        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "El token de Google no contiene una dirección de correo válida.");
        }

        String givenName = jwt.getClaimAsString("given_name");
        String familyName = jwt.getClaimAsString("family_name");
        String name = jwt.getClaimAsString("name");

        return new GoogleUserPayload(sub, email, true, givenName, familyName, name);
    }
}
