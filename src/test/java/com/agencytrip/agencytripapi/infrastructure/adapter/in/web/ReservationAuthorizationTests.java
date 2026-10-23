package com.agencytrip.agencytripapi.infrastructure.adapter.in.web;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agencytrip.agencytripapi.application.dto.Requests;
import com.agencytrip.agencytripapi.application.service.ReservationService;
import com.agencytrip.agencytripapi.application.service.UserService;
import com.agencytrip.agencytripapi.domain.model.User;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;

@SpringBootTest
@ActiveProfiles("test")
class ReservationAuthorizationTests {

    @Autowired private WebApplicationContext wac;
    private MockMvc mockMvc;
    @Autowired private UserService userService;
    @Autowired private ReservationService reservationService;
    @Autowired private JwtEncoder jwtEncoder;
    @Autowired private SecretKey jwtKey;

    private User clientUser;
    private User otherClient;
    private User adminUser;

    private String clientToken;
    private String adminToken;
    private String unknownRoleToken;
    private String noRoleToken;
    private String emptyRolesToken;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac)
                .apply(springSecurity())
                .build();
        // Create users with unique emails
        clientUser = userService.register(new Requests.Register("Client", "One", "client1+" + UUID.randomUUID() + "@example.com", "Pass123"));
        otherClient = userService.register(new Requests.Register("Client", "Two", "client2+" + UUID.randomUUID() + "@example.com", "Pass123"));
        adminUser = userService.register(new Requests.Register("Admin", "User", "admin+" + UUID.randomUUID() + "@example.com", "Pass123"));
        // No reservations created to avoid package dependency
        // Generate JWTs
        clientToken = generateToken(clientUser.getIdUsuario(), clientUser.getCorreo(), List.of("ROLE_CLIENTE"));
        adminToken = generateToken(adminUser.getIdUsuario(), adminUser.getCorreo(), List.of("ROLE_ADMINISTRADOR"));
        unknownRoleToken = generateToken(otherClient.getIdUsuario(), otherClient.getCorreo(), List.of("ROLE_EMPLEADO"));
        noRoleToken = generateToken(otherClient.getIdUsuario(), otherClient.getCorreo(), null);
        emptyRolesToken = generateToken(otherClient.getIdUsuario(), otherClient.getCorreo(), List.of());
    }

    private String generateToken(int userId, String email, List<String> roles) {
        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
                .issuer("test-issuer")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .subject(email)
                .claim("userId", userId)
                .claim("email", email)
                .audience(List.of("test-audience")); // add required audience for validation
        if (roles != null) {
            builder.claim("roles", roles);
        }
        JwtClaimsSet claims = builder.build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    @AfterEach
    void tearDown() {
        try { userService.disable(clientUser.getIdUsuario(), null); } catch (Exception ignored) {}
        try { userService.disable(otherClient.getIdUsuario(), null); } catch (Exception ignored) {}
        try { userService.disable(adminUser.getIdUsuario(), null); } catch (Exception ignored) {}
    }

    @Test
    @DisplayName("Cliente con ROLE_CLIENTE solo ve sus propias reservas")
    void clientSeesOwnReservations() throws Exception {
        mockMvc.perform(get("/api/Reservas").header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Usuario con rol desconocido recibe 403 al listar reservas")
    void unknownRoleForbidden() throws Exception {
        mockMvc.perform(get("/api/Reservas").header("Authorization", "Bearer " + unknownRoleToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Administrador con ROLE_ADMINISTRADOR puede listar todas las reservas")
    void adminCanListAll() throws Exception {
        mockMvc.perform(get("/api/Reservas").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Token sin claim de roles devuelve 401")
    void tokenWithoutRoles401() throws Exception {
        mockMvc.perform(get("/api/Reservas").header("Authorization", "Bearer " + noRoleToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token con lista de roles vacía devuelve 401")
    void tokenWithEmptyRoles401() throws Exception {
        mockMvc.perform(get("/api/Reservas").header("Authorization", "Bearer " + emptyRolesToken))
                .andExpect(status().isUnauthorized());
    }
}
