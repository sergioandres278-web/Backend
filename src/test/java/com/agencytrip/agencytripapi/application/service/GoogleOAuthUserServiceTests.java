package com.agencytrip.agencytripapi.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import com.agencytrip.agencytripapi.application.dto.GoogleUserPayload;
import com.agencytrip.agencytripapi.application.dto.Requests;
import com.agencytrip.agencytripapi.application.port.out.GoogleTokenVerifierPort;
import com.agencytrip.agencytripapi.application.port.out.PasswordHasherPort;
import com.agencytrip.agencytripapi.application.port.out.TokenPort;
import com.agencytrip.agencytripapi.domain.model.Role;
import com.agencytrip.agencytripapi.domain.model.SocialAccount;
import com.agencytrip.agencytripapi.domain.model.User;
import com.agencytrip.agencytripapi.domain.port.out.TravelRepositoryPort;

class GoogleOAuthUserServiceTests {

    private TravelRepositoryPort repository;
    private PasswordHasherPort hasher;
    private TokenPort tokens;
    private GoogleTokenVerifierPort googleVerifier;
    private UserService userService;

    @BeforeEach
    void setUp() {
        repository = mock(TravelRepositoryPort.class);
        hasher = mock(PasswordHasherPort.class);
        tokens = mock(TokenPort.class);
        googleVerifier = mock(GoogleTokenVerifierPort.class);

        when(hasher.hash(any())).thenReturn("$2a$10$hashedRandomPasswordForSocialUser");

        Role clienteRole = new Role();
        clienteRole.setIdRol(2);
        clienteRole.setNombre("CLIENTE");
        when(repository.findRole(2)).thenReturn(Optional.of(clienteRole));

        userService = new UserService(repository, hasher, tokens, googleVerifier);
    }

    @Test
    @DisplayName("Usuario nuevo: Se crea automáticamente con rol CLIENTE, se vincula y recibe JWT propio")
    void newUserRegistersAsClienteAndReceivesJwt() {
        GoogleUserPayload payload = new GoogleUserPayload(
                "sub-google-new-987",
                "nuevo.viajero@gmail.com",
                true,
                "Carlos",
                "Gomez",
                "Carlos Gomez"
        );
        when(googleVerifier.verify("token-google-valid")).thenReturn(payload);
        when(repository.findSocialAccount("GOOGLE", "sub-google-new-987")).thenReturn(Optional.empty());
        when(repository.emailExists("nuevo.viajero@gmail.com", null)).thenReturn(false);

        when(repository.saveUser(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setIdUsuario(45);
            return u;
        });
        when(repository.saveSocialAccount(any(SocialAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokens.create(eq(45), eq("Carlos"), eq("nuevo.viajero@gmail.com"), eq("CLIENTE")))
                .thenReturn("agencytrip-jwt-token-cliente");

        var result = userService.loginWithGoogle(new Requests.GoogleOAuthLogin("token-google-valid"));

        assertNotNull(result);
        assertEquals("agencytrip-jwt-token-cliente", result.token());
        assertEquals("CLIENTE", result.role());
        assertEquals(45, result.user().getIdUsuario());
        assertEquals(2, result.user().getIdRol());

        // Verificar que el usuario guardado tiene estrictamente idRol = 2
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(repository).saveUser(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals(2, savedUser.getIdRol(), "El rol de registro social debe ser estrictamente CLIENTE (idRol = 2)");
        assertEquals("nuevo.viajero@gmail.com", savedUser.getCorreo());
        assertTrue(savedUser.isEstado());

        // Verificar vinculación de cuenta social
        ArgumentCaptor<SocialAccount> socialCaptor = ArgumentCaptor.forClass(SocialAccount.class);
        verify(repository).saveSocialAccount(socialCaptor.capture());
        SocialAccount savedSa = socialCaptor.getValue();
        assertEquals("GOOGLE", savedSa.getProveedor());
        assertEquals("sub-google-new-987", savedSa.getIdProveedor());
        assertEquals("nuevo.viajero@gmail.com", savedSa.getCorreoProveedor());
        assertEquals(45, savedSa.getIdUsuario());
    }

    @Test
    @DisplayName("Usuario social existente: Inicia sesión directamente y emite el JWT de AgencyTrip")
    void existingSocialUserLogsInDirectly() {
        GoogleUserPayload payload = new GoogleUserPayload(
                "sub-google-existing-555",
                "viajero.frecuente@gmail.com",
                true,
                "Maria",
                "Lopez",
                "Maria Lopez"
        );
        when(googleVerifier.verify("token-google-existing")).thenReturn(payload);

        User existingUser = new User();
        existingUser.setIdUsuario(12);
        existingUser.setNombre("Maria");
        existingUser.setApellido("Lopez");
        existingUser.setCorreo("viajero.frecuente@gmail.com");
        existingUser.setIdRol(2);
        Role role = new Role();
        role.setIdRol(2);
        role.setNombre("CLIENTE");
        existingUser.setRol(role);
        existingUser.setEstado(true);

        SocialAccount existingSa = new SocialAccount();
        existingSa.setIdCuentaSocial(3);
        existingSa.setIdUsuario(12);
        existingSa.setProveedor("GOOGLE");
        existingSa.setIdProveedor("sub-google-existing-555");
        existingSa.setUsuario(existingUser);

        when(repository.findSocialAccount("GOOGLE", "sub-google-existing-555")).thenReturn(Optional.of(existingSa));
        when(tokens.create(12, "Maria", "viajero.frecuente@gmail.com", "CLIENTE")).thenReturn("jwt-existente");

        var result = userService.loginWithGoogle(new Requests.GoogleOAuthLogin("token-google-existing"));

        assertEquals("jwt-existente", result.token());
        assertEquals("CLIENTE", result.role());
        verify(repository, never()).saveUser(any());
    }

    @Test
    @DisplayName("Conflicto con cuenta tradicional: Rechaza con 409 Conflict si el correo ya existe sin vinculación social")
    void conflictWithTraditionalAccountThrowsConflict409() {
        GoogleUserPayload payload = new GoogleUserPayload(
                "sub-google-unlinked-111",
                "usuario.tradicional@agenciaviajes.com",
                true,
                "Pedro",
                "Perez",
                "Pedro Perez"
        );
        when(googleVerifier.verify("token-google-tradicional")).thenReturn(payload);
        when(repository.findSocialAccount("GOOGLE", "sub-google-unlinked-111")).thenReturn(Optional.empty());
        // El correo ya existe en usuarios
        when(repository.emailExists("usuario.tradicional@agenciaviajes.com", null)).thenReturn(true);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> userService.loginWithGoogle(new Requests.GoogleOAuthLogin("token-google-tradicional"))
        );

        assertEquals(HttpStatus.CONFLICT, ex.status());
        assertTrue(ex.getMessage().contains("El correo ya está registrado con una cuenta tradicional"));
        verify(repository, never()).saveUser(any());
        verify(repository, never()).saveSocialAccount(any());
    }

    @Test
    @DisplayName("Garantizar que nunca se asigne ADMINISTRADOR automáticamente mediante Google")
    void neverAssignsAdministratorRoleViaSocialLogin() {
        GoogleUserPayload payload = new GoogleUserPayload(
                "sub-admin-attempt",
                "admin.attempt@gmail.com",
                true,
                "Admin",
                "Fake",
                "Admin Fake"
        );
        when(googleVerifier.verify("token-fake-admin")).thenReturn(payload);
        when(repository.findSocialAccount("GOOGLE", "sub-admin-attempt")).thenReturn(Optional.empty());
        when(repository.emailExists("admin.attempt@gmail.com", null)).thenReturn(false);

        when(repository.saveUser(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setIdUsuario(99);
            return u;
        });
        when(repository.saveSocialAccount(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokens.create(eq(99), any(), any(), eq("CLIENTE"))).thenReturn("jwt-cliente-safe");

        var result = userService.loginWithGoogle(new Requests.GoogleOAuthLogin("token-fake-admin"));

        assertNotEquals("ADMINISTRADOR", result.role());
        assertEquals("CLIENTE", result.role());
        assertEquals(2, result.user().getIdRol());
        verify(tokens).create(eq(99), any(), any(), eq("CLIENTE"));
    }

    @Test
    @DisplayName("Usuario social inactivo: Lanza 401 Unauthorized")
    void inactiveSocialUserThrowsUnauthorized() {
        GoogleUserPayload payload = new GoogleUserPayload(
                "sub-google-inactive",
                "inactivo@gmail.com",
                true,
                "Inactivo",
                "User",
                "Inactivo User"
        );
        when(googleVerifier.verify("token-inactive")).thenReturn(payload);

        User inactiveUser = new User();
        inactiveUser.setIdUsuario(20);
        inactiveUser.setEstado(false);

        SocialAccount sa = new SocialAccount();
        sa.setIdUsuario(20);
        sa.setUsuario(inactiveUser);
        when(repository.findSocialAccount("GOOGLE", "sub-google-inactive")).thenReturn(Optional.of(sa));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> userService.loginWithGoogle(new Requests.GoogleOAuthLogin("token-inactive"))
        );
        assertEquals(HttpStatus.UNAUTHORIZED, ex.status());
    }
}

