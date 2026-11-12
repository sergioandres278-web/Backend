package com.agencytrip.agencytripapi.infrastructure.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.agencytrip.agencytripapi.application.dto.FacebookUserPayload;
import com.agencytrip.agencytripapi.application.service.BusinessException;

class FacebookGraphTokenVerifierTests {

    private static final String APP_ID = "123456789012345";
    private static final String APP_SECRET = "abcdef0123456789abcdef0123456789";
    private static final String GRAPH_VERSION = "v21.0";
    private static final String BASE_URL = "https://graph.facebook.com";

    private MockRestServiceServer mockServer;
    private FacebookGraphTokenVerifier verifier;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        verifier = new FacebookGraphTokenVerifier(
                restClient,
                APP_ID,
                APP_SECRET,
                GRAPH_VERSION,
                BASE_URL
        );
    }

    private String debugTokenUri(String token) {
        return BASE_URL + "/" + GRAPH_VERSION + "/debug_token?input_token=" + token + "&access_token=" + APP_ID + "%7C" + APP_SECRET;
    }

    private String meUri(String token) {
        return BASE_URL + "/" + GRAPH_VERSION + "/me?fields=id,name,first_name,last_name,email&access_token=" + token;
    }

    @Test
    @DisplayName("Debe verificar exitosamente un token de Facebook válido y contrastar con /me")
    void debeVerificarTokenValido() {
        String token = "valid-fb-user-token";
        long futureExp = Instant.now().getEpochSecond() + 3600;

        String debugResponse = """
            {
              "data": {
                "app_id": "123456789012345",
                "type": "USER",
                "application": "AgencyTrip CO",
                "expires_at": %d,
                "is_valid": true,
                "scopes": ["email", "public_profile"],
                "user_id": "999888777"
              }
            }
            """.formatted(futureExp);

        String meResponse = """
            {
              "id": "999888777",
              "name": "Carlos Gomez",
              "first_name": "Carlos",
              "last_name": "Gomez",
              "email": "carlos.gomez@example.com"
            }
            """;

        mockServer.expect(requestTo(debugTokenUri(token)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(debugResponse, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo(meUri(token)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(meResponse, MediaType.APPLICATION_JSON));

        FacebookUserPayload payload = verifier.verify(token);

        assertNotNull(payload);
        assertEquals("999888777", payload.id());
        assertEquals("carlos.gomez@example.com", payload.email());
        assertEquals("Carlos Gomez", payload.name());
        assertEquals("Carlos", payload.firstName());
        assertEquals("Gomez", payload.lastName());

        mockServer.verify();
    }

    @Test
    @DisplayName("Debe rechazar token cuando is_valid es falso")
    void debeRechazarTokenInvalido() {
        String token = "invalid-token";
        String debugResponse = """
            {
              "data": {
                "app_id": "123456789012345",
                "is_valid": false,
                "user_id": "999888777"
              }
            }
            """;

        mockServer.expect(requestTo(debugTokenUri(token)))
                .andRespond(withSuccess(debugResponse, MediaType.APPLICATION_JSON));

        BusinessException ex = assertThrows(BusinessException.class, () -> verifier.verify(token));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.status());
        assertTrue(ex.getMessage().contains("no es válido o ha sido revocado"));
        mockServer.verify();
    }

    @Test
    @DisplayName("Debe rechazar token cuando expires_at indica que ya caducó")
    void debeRechazarTokenVencido() {
        String token = "expired-token";
        long pastExp = Instant.now().getEpochSecond() - 3600;

        String debugResponse = """
            {
              "data": {
                "app_id": "123456789012345",
                "expires_at": %d,
                "is_valid": true,
                "user_id": "999888777"
              }
            }
            """.formatted(pastExp);

        mockServer.expect(requestTo(debugTokenUri(token)))
                .andRespond(withSuccess(debugResponse, MediaType.APPLICATION_JSON));

        BusinessException ex = assertThrows(BusinessException.class, () -> verifier.verify(token));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.status());
        assertTrue(ex.getMessage().contains("ha expirado"));
        mockServer.verify();
    }

    @Test
    @DisplayName("Debe rechazar token emitido para otra aplicación distinta (app_id mismatch)")
    void debeRechazarTokenDeOtraAplicacion() {
        String token = "token-from-other-app";
        long futureExp = Instant.now().getEpochSecond() + 3600;

        String debugResponse = """
            {
              "data": {
                "app_id": "999999999999999",
                "expires_at": %d,
                "is_valid": true,
                "user_id": "999888777"
              }
            }
            """.formatted(futureExp);

        mockServer.expect(requestTo(debugTokenUri(token)))
                .andRespond(withSuccess(debugResponse, MediaType.APPLICATION_JSON));

        BusinessException ex = assertThrows(BusinessException.class, () -> verifier.verify(token));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.status());
        assertTrue(ex.getMessage().contains("emitido para una aplicación distinta"));
        mockServer.verify();
    }

    @Test
    @DisplayName("Debe rechazar cuando el user_id de /me no coincide con el validado en debug_token")
    void debeRechazarCuandoIdentificadorDeMeNoCoincide() {
        String token = "tampered-user-token";
        long futureExp = Instant.now().getEpochSecond() + 3600;

        String debugResponse = """
            {
              "data": {
                "app_id": "123456789012345",
                "expires_at": %d,
                "is_valid": true,
                "user_id": "999888777"
              }
            }
            """.formatted(futureExp);

        String meResponse = """
            {
              "id": "111222333",
              "name": "Usuario Suplantador",
              "email": "suplantador@example.com"
            }
            """;

        mockServer.expect(requestTo(debugTokenUri(token)))
                .andRespond(withSuccess(debugResponse, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo(meUri(token)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(meResponse, MediaType.APPLICATION_JSON));

        BusinessException ex = assertThrows(BusinessException.class, () -> verifier.verify(token));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.status());
        assertTrue(ex.getMessage().contains("no coincide con la identidad verificada"));
        mockServer.verify();
    }

    @Test
    @DisplayName("Debe rechazar cuando Facebook no proporciona un correo válido")
    void debeRechazarCuandoFacebookNoProporcionaCorreo() {
        String token = "no-email-token";
        long futureExp = Instant.now().getEpochSecond() + 3600;

        String debugResponse = """
            {
              "data": {
                "app_id": "123456789012345",
                "expires_at": %d,
                "is_valid": true,
                "user_id": "999888777"
              }
            }
            """.formatted(futureExp);

        String meResponse = """
            {
              "id": "999888777",
              "name": "Usuario Sin Email",
              "first_name": "Usuario",
              "last_name": "Sin Email"
            }
            """;

        mockServer.expect(requestTo(debugTokenUri(token)))
                .andRespond(withSuccess(debugResponse, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo(meUri(token)))
                .andRespond(withSuccess(meResponse, MediaType.APPLICATION_JSON));

        BusinessException ex = assertThrows(BusinessException.class, () -> verifier.verify(token));
        assertEquals(HttpStatus.BAD_REQUEST, ex.status());
        assertTrue(ex.getMessage().contains("Se requiere un correo válido para registrarte en AgencyTrip"));
        mockServer.verify();
    }

    @Test
    @DisplayName("Debe rechazar token vacío o nulo sin realizar peticiones de red")
    void debeRechazarTokenVacioONulo() {
        BusinessException exNull = assertThrows(BusinessException.class, () -> verifier.verify(null));
        assertEquals(HttpStatus.UNAUTHORIZED, exNull.status());

        BusinessException exBlank = assertThrows(BusinessException.class, () -> verifier.verify("   "));
        assertEquals(HttpStatus.UNAUTHORIZED, exBlank.status());
    }

    @Test
    @DisplayName("Debe rechazar cuando Meta responde con error HTTP en debug_token")
    void debeRechazarCuandoMetaRetornaError() {
        String token = "malformed-token";

        mockServer.expect(requestTo(debugTokenUri(token)))
                .andRespond(withBadRequest().body("""
                    {
                      "error": {
                        "message": "Invalid OAuth access token.",
                        "type": "OAuthException",
                        "code": 190
                      }
                    }
                    """));

        BusinessException ex = assertThrows(BusinessException.class, () -> verifier.verify(token));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.status());
        assertTrue(ex.getMessage().contains("El token de Facebook es inválido"));
        mockServer.verify();
    }

    @Test
    @DisplayName("Debe inicializarse correctamente con RestClient.Builder provisto")
    void debeInicializarseConRestClientBuilder() {
        RestClient.Builder builder = RestClient.builder();
        FacebookGraphTokenVerifier v = new FacebookGraphTokenVerifier(builder, APP_ID, APP_SECRET, GRAPH_VERSION, BASE_URL);
        assertNotNull(v);
    }

    @Test
    @DisplayName("Debe inicializarse correctamente con fallback cuando RestClient.Builder es nulo")
    void debeInicializarseConFallbackCuandoRestClientBuilderEsNulo() {
        FacebookGraphTokenVerifier v = new FacebookGraphTokenVerifier((RestClient.Builder) null, APP_ID, APP_SECRET, GRAPH_VERSION, BASE_URL);
        assertNotNull(v);
    }

    @Test
    @DisplayName("Debe inicializarse correctamente con el constructor de 4 argumentos")
    void debeInicializarseConConstructorDeCuatroArgumentos() {
        FacebookGraphTokenVerifier v = new FacebookGraphTokenVerifier(APP_ID, APP_SECRET, GRAPH_VERSION, BASE_URL);
        assertNotNull(v);
    }
}
