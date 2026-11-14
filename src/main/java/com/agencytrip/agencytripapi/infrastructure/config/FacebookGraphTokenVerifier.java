package com.agencytrip.agencytripapi.infrastructure.config;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.agencytrip.agencytripapi.application.dto.FacebookUserPayload;
import com.agencytrip.agencytripapi.application.port.out.FacebookTokenVerifierPort;
import com.agencytrip.agencytripapi.application.service.BusinessException;
import com.fasterxml.jackson.annotation.JsonProperty;

@Component
public class FacebookGraphTokenVerifier implements FacebookTokenVerifierPort {

    private final RestClient restClient;
    private final String appId;
    private final String appSecret;
    private final String graphVersion;
    private final String graphBaseUrl;

    @Autowired
    public FacebookGraphTokenVerifier(
            @Autowired(required = false) RestClient.Builder restClientBuilder,
            @Value("${agencytrip.oauth.facebook.app-id:}") String appId,
            @Value("${agencytrip.oauth.facebook.app-secret:}") String appSecret,
            @Value("${agencytrip.oauth.facebook.graph-version:v21.0}") String graphVersion,
            @Value("${agencytrip.oauth.facebook.graph-base-url:https://graph.facebook.com}") String graphBaseUrl) {
        this(
                (restClientBuilder != null ? restClientBuilder : RestClient.builder()).build(),
                appId,
                appSecret,
                graphVersion,
                graphBaseUrl
        );
    }

    public FacebookGraphTokenVerifier(
            String appId,
            String appSecret,
            String graphVersion,
            String graphBaseUrl) {
        this(RestClient.builder().build(), appId, appSecret, graphVersion, graphBaseUrl);
    }

    public FacebookGraphTokenVerifier(
            RestClient restClient,
            String appId,
            String appSecret,
            String graphVersion,
            String graphBaseUrl) {
        this.restClient = Objects.requireNonNull(restClient, "restClient cannot be null");
        this.appId = appId != null ? appId.trim() : "";
        this.appSecret = appSecret != null ? appSecret.trim() : "";
        this.graphVersion = graphVersion != null ? graphVersion.trim() : "v21.0";
        this.graphBaseUrl = graphBaseUrl != null ? graphBaseUrl.trim() : "https://graph.facebook.com";
    }

    @Override
    public FacebookUserPayload verify(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "El token de acceso de Facebook es obligatorio.");
        }

        if (this.appId.isBlank() || this.appSecret.isBlank()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "Autenticación con Facebook no configurada en el servidor (faltan credenciales de aplicación).");
        }

        String appAccessToken = this.appId + "|" + this.appSecret;

        // 1. Validar el token con el endpoint oficial debug_token
        DebugTokenResponse debugResponse;
        try {
            String debugUri = this.graphBaseUrl + "/" + this.graphVersion + "/debug_token?input_token=" + accessToken + "&access_token=" + appAccessToken;
            debugResponse = restClient.get()
                    .uri(debugUri)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        throw new BusinessException(HttpStatus.UNAUTHORIZED, "El token de Facebook es inválido o ha sido rechazado por Meta.");
                    })
                    .body(DebugTokenResponse.class);
        } catch (BusinessException be) {
            throw be;
        } catch (RestClientException ex) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Error de comunicación al verificar el token con Facebook: " + ex.getMessage());
        }

        if (debugResponse == null || debugResponse.data() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Respuesta vacía o inválida del servicio de verificación de Facebook.");
        }

        DebugData data = debugResponse.data();

        // 2. Validar is_valid
        if (!Boolean.TRUE.equals(data.isValid())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "El token de Facebook no es válido o ha sido revocado.");
        }

        // 3. Validar app_id
        if (!this.appId.equals(data.appId())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "El token de Facebook fue emitido para una aplicación distinta.");
        }

        // 4. Validar vencimiento (expires_at)
        if (data.expiresAt() != null && data.expiresAt() > 0) {
            long nowEpoch = Instant.now().getEpochSecond();
            if (data.expiresAt() < nowEpoch) {
                throw new BusinessException(HttpStatus.UNAUTHORIZED, "El token de Facebook ha expirado.");
            }
        }

        // 5. Validar user_id
        String validatedUserId = data.userId();
        if (validatedUserId == null || validatedUserId.isBlank()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "El token de Facebook no contiene un identificador de usuario válido.");
        }

        // 6. Consultar perfil en /me para contrastar identidad y obtener correo
        MeResponse meResponse;
        try {
            String meUri = this.graphBaseUrl + "/" + this.graphVersion + "/me?fields=id,name,first_name,last_name,email&access_token=" + accessToken;
            meResponse = restClient.get()
                    .uri(meUri)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        throw new BusinessException(HttpStatus.UNAUTHORIZED, "No fue posible obtener el perfil del usuario desde Facebook.");
                    })
                    .body(MeResponse.class);
        } catch (BusinessException be) {
            throw be;
        } catch (RestClientException ex) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Error al consultar el perfil del usuario en Facebook: " + ex.getMessage());
        }

        if (meResponse == null || meResponse.id() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "No se recibieron datos de perfil desde Facebook.");
        }

        // 7. Contrastar el identificador obtenido mediante /me con el user_id validado en debug_token
        if (!validatedUserId.equals(meResponse.id())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "El identificador del usuario obtenido en el perfil no coincide con la identidad verificada por Meta.");
        }

        // 8. Validar correo electrónico
        String email = meResponse.email();
        if (email == null || email.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "No fue posible obtener un correo electrónico válido desde tu cuenta de Facebook. Se requiere un correo válido para registrarte en AgencyTrip.");
        }

        return new FacebookUserPayload(
                validatedUserId,
                email.toLowerCase().trim(),
                meResponse.name(),
                meResponse.firstName(),
                meResponse.lastName()
        );
    }

    // Records internos para deserializar respuestas de Meta Graph API
    public record DebugTokenResponse(DebugData data) {}

    public record DebugData(
            @JsonProperty("app_id") String appId,
            @JsonProperty("type") String type,
            @JsonProperty("application") String application,
            @JsonProperty("data_access_expires_at") Long dataAccessExpiresAt,
            @JsonProperty("expires_at") Long expiresAt,
            @JsonProperty("is_valid") Boolean isValid,
            @JsonProperty("issued_at") Long issuedAt,
            @JsonProperty("scopes") List<String> scopes,
            @JsonProperty("user_id") String userId
    ) {}

    public record MeResponse(
            String id,
            String name,
            @JsonProperty("first_name") String firstName,
            @JsonProperty("last_name") String lastName,
            String email
    ) {}
}
