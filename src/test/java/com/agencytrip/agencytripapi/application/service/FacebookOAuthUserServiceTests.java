package com.agencytrip.agencytripapi.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.agencytrip.agencytripapi.application.dto.FacebookUserPayload;
import com.agencytrip.agencytripapi.application.dto.Requests;
import com.agencytrip.agencytripapi.application.port.out.FacebookTokenVerifierPort;
import com.agencytrip.agencytripapi.application.port.out.PasswordHasherPort;
import com.agencytrip.agencytripapi.application.port.out.TokenPort;
import com.agencytrip.agencytripapi.domain.model.Role;
import com.agencytrip.agencytripapi.domain.model.SocialAccount;
import com.agencytrip.agencytripapi.domain.model.User;
import com.agencytrip.agencytripapi.domain.port.out.TravelRepositoryPort;

@ExtendWith(MockitoExtension.class)
class FacebookOAuthUserServiceTests {

    @Mock
    private TravelRepositoryPort repo;

    @Mock
    private PasswordHasherPort hasher;

    @Mock
    private TokenPort tokens;

    @Mock
    private FacebookTokenVerifierPort facebookVerifier;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(repo, hasher, tokens, null, facebookVerifier);
    }

    private Role createRole(int id, String nombre) {
        Role r = new Role();
        r.setIdRol(id);
        r.setNombre(nombre);
        return r;
    }

    @Test
    @DisplayName("Debe auto-registrar nuevo usuario con rol CLIENTE cuando no existe cuenta social ni correo")
    void debeRegistrarNuevoUsuarioConRolClienteSiNoExiste() {
        String token = "valid-token";
        FacebookUserPayload payload = new FacebookUserPayload(
                "fb-uid-100",
                "laura@example.com",
                "Laura Sanchez",
                "Laura",
                "Sanchez"
        );

        when(facebookVerifier.verify(token)).thenReturn(payload);
        when(repo.findSocialAccount("FACEBOOK", "fb-uid-100")).thenReturn(Optional.empty());
        when(repo.emailExists("laura@example.com", null)).thenReturn(false);
        when(hasher.hash(anyString())).thenReturn("$2a$10$hashedUnpredictablePassword");

        User savedUser = new User();
        savedUser.setIdUsuario(25);
        savedUser.setNombre("Laura");
        savedUser.setApellido("Sanchez");
        savedUser.setCorreo("laura@example.com");
        savedUser.setIdRol(2);
        savedUser.setEstado(true);

        when(repo.saveUser(any(User.class))).thenReturn(savedUser);
        Role roleCliente = createRole(2, "CLIENTE");
        when(repo.findRole(2)).thenReturn(Optional.of(roleCliente));
        when(tokens.create(eq(25), eq("Laura"), eq("laura@example.com"), eq("CLIENTE"))).thenReturn("jwt-agencytrip-xyz");

        Requests.FacebookOAuthLogin request = new Requests.FacebookOAuthLogin(token);
        UserService.LoginResult result = userService.loginWithFacebook(request);

        assertNotNull(result);
        assertEquals("jwt-agencytrip-xyz", result.token());
        assertEquals("CLIENTE", result.role());
        assertEquals(25, result.user().getIdUsuario());

        // Verificar persistencia de usuario
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(repo).saveUser(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertEquals("Laura", capturedUser.getNombre());
        assertEquals("Sanchez", capturedUser.getApellido());
        assertEquals("laura@example.com", capturedUser.getCorreo());
        assertEquals(2, capturedUser.getIdRol(), "El rol asignado DEBE ser estrictamente 2 (CLIENTE)");
        assertTrue(capturedUser.isEstado());

        // Verificar persistencia de relación social
        ArgumentCaptor<SocialAccount> saCaptor = ArgumentCaptor.forClass(SocialAccount.class);
        verify(repo).saveSocialAccount(saCaptor.capture());
        SocialAccount capturedSa = saCaptor.getValue();
        assertEquals(25, capturedSa.getIdUsuario());
        assertEquals("FACEBOOK", capturedSa.getProveedor());
        assertEquals("fb-uid-100", capturedSa.getIdProveedor());
        assertEquals("laura@example.com", capturedSa.getCorreoProveedor());
    }

    @Test
    @DisplayName("Debe iniciar sesión de usuario existente vinculado a Facebook sin crear duplicados")
    void debeIniciarSesionSiCuentaFacebookYaExiste() {
        String token = "existing-token";
        FacebookUserPayload payload = new FacebookUserPayload(
                "fb-uid-200",
                "existente@example.com",
                "Juan Perez",
                "Juan",
                "Perez"
        );

        User existingUser = new User();
        existingUser.setIdUsuario(7);
        existingUser.setNombre("Juan");
        existingUser.setApellido("Perez");
        existingUser.setCorreo("existente@example.com");
        existingUser.setIdRol(2);
        existingUser.setRol(createRole(2, "CLIENTE"));
        existingUser.setEstado(true);

        SocialAccount existingSa = new SocialAccount();
        existingSa.setIdCuentaSocial(3);
        existingSa.setIdUsuario(7);
        existingSa.setProveedor("FACEBOOK");
        existingSa.setIdProveedor("fb-uid-200");
        existingSa.setUsuario(existingUser);

        when(facebookVerifier.verify(token)).thenReturn(payload);
        when(repo.findSocialAccount("FACEBOOK", "fb-uid-200")).thenReturn(Optional.of(existingSa));
        when(tokens.create(eq(7), eq("Juan"), eq("existente@example.com"), eq("CLIENTE"))).thenReturn("jwt-existente-abc");

        Requests.FacebookOAuthLogin request = new Requests.FacebookOAuthLogin(token);
        UserService.LoginResult result = userService.loginWithFacebook(request);

        assertNotNull(result);
        assertEquals("jwt-existente-abc", result.token());
        assertEquals("CLIENTE", result.role());
        verify(repo, never()).saveUser(any(User.class));
        verify(repo, never()).saveSocialAccount(any(SocialAccount.class));
    }

    @Test
    @DisplayName("Debe rechazar con HTTP 409 Conflict si el correo ya pertenece a una cuenta tradicional sin vinculación social")
    void debeRechazarConConflicto409SiCorreoExisteEnCuentaTradicional() {
        String token = "conflict-token";
        FacebookUserPayload payload = new FacebookUserPayload(
                "fb-uid-300",
                "tradicional@example.com",
                "Pedro Perez",
                "Pedro",
                "Perez"
        );

        when(facebookVerifier.verify(token)).thenReturn(payload);
        when(repo.findSocialAccount("FACEBOOK", "fb-uid-300")).thenReturn(Optional.empty());
        when(repo.emailExists("tradicional@example.com", null)).thenReturn(true);

        Requests.FacebookOAuthLogin request = new Requests.FacebookOAuthLogin(token);
        BusinessException ex = assertThrows(BusinessException.class, () -> userService.loginWithFacebook(request));

        assertEquals(HttpStatus.CONFLICT, ex.status());
        assertTrue(ex.getMessage().contains("cuenta tradicional"));
        verify(repo, never()).saveUser(any(User.class));
        verify(repo, never()).saveSocialAccount(any(SocialAccount.class));
    }

    @Test
    @DisplayName("Nunca debe asignar el rol ADMINISTRADOR mediante registro con Facebook")
    void nuncaDebeAsignarRolAdministradorEnRegistroFacebook() {
        String token = "admin-spoof-token";
        FacebookUserPayload payload = new FacebookUserPayload(
                "fb-uid-400",
                "hacker@example.com",
                "Administrador Falso",
                "Admin",
                "Falso"
        );

        when(facebookVerifier.verify(token)).thenReturn(payload);
        when(repo.findSocialAccount("FACEBOOK", "fb-uid-400")).thenReturn(Optional.empty());
        when(repo.emailExists("hacker@example.com", null)).thenReturn(false);
        when(hasher.hash(anyString())).thenReturn("$2a$10$hashedUnpredictablePassword");

        User savedUser = new User();
        savedUser.setIdUsuario(99);
        savedUser.setIdRol(2);
        savedUser.setRol(createRole(2, "CLIENTE"));
        savedUser.setEstado(true);

        when(repo.saveUser(any(User.class))).thenReturn(savedUser);
        when(tokens.create(anyInt(), any(), any(), eq("CLIENTE"))).thenReturn("jwt-client-token");

        Requests.FacebookOAuthLogin request = new Requests.FacebookOAuthLogin(token);
        UserService.LoginResult result = userService.loginWithFacebook(request);

        assertNotEquals("ADMINISTRADOR", result.role());
        assertEquals("CLIENTE", result.role());
    }

    @Test
    @DisplayName("Debe rechazar la autenticación con HTTP 401 si el usuario asociado se encuentra inactivo")
    void debeRechazarLoginSiUsuarioAsociadoEstaInactivo() {
        String token = "inactive-token";
        FacebookUserPayload payload = new FacebookUserPayload(
                "fb-uid-500",
                "inactivo@example.com",
                "Usuario Inactivo",
                "Usuario",
                "Inactivo"
        );

        User inactiveUser = new User();
        inactiveUser.setIdUsuario(50);
        inactiveUser.setEstado(false); // Inactivo

        SocialAccount sa = new SocialAccount();
        sa.setIdUsuario(50);
        sa.setUsuario(inactiveUser);

        when(facebookVerifier.verify(token)).thenReturn(payload);
        when(repo.findSocialAccount("FACEBOOK", "fb-uid-500")).thenReturn(Optional.of(sa));

        Requests.FacebookOAuthLogin request = new Requests.FacebookOAuthLogin(token);
        BusinessException ex = assertThrows(BusinessException.class, () -> userService.loginWithFacebook(request));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.status());
        assertTrue(ex.getMessage().contains("Usuario inactivo"));
    }
}
