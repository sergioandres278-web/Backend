package com.agencytrip.agencytripapi.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.agencytrip.agencytripapi.application.dto.Requests;
import com.agencytrip.agencytripapi.application.port.out.PasswordHasherPort;
import com.agencytrip.agencytripapi.application.port.out.TokenPort;
import com.agencytrip.agencytripapi.domain.model.Role;
import com.agencytrip.agencytripapi.domain.model.User;
import com.agencytrip.agencytripapi.domain.port.out.TravelRepositoryPort;

/**
 * Pruebas unitarias de seguridad para el Módulo 4:
 * Protección de la cuenta administradora contra auto-desactivación y auto-democión.
 */
class UserAdminSelfProtectionTests {

    private TravelRepositoryPort repository;
    private PasswordHasherPort hasher;
    private TokenPort tokens;
    private UserService service;

    private User adminUser;
    private User otherUser;
    private Role adminRole;
    private Role clientRole;

    @BeforeEach
    void setUp() {
        repository = mock(TravelRepositoryPort.class);
        hasher = mock(PasswordHasherPort.class);
        tokens = mock(TokenPort.class);
        service = new UserService(repository, hasher, tokens);

        adminRole = new Role();
        adminRole.setIdRol(1);
        adminRole.setNombre("ADMINISTRADOR");

        clientRole = new Role();
        clientRole.setIdRol(2);
        clientRole.setNombre("CLIENTE");

        adminUser = new User();
        adminUser.setIdUsuario(4);
        adminUser.setNombre("Admin");
        adminUser.setApellido("Principal");
        adminUser.setCorreo("admin@agenciaviajes.com");
        adminUser.setIdRol(1);
        adminUser.setRol(adminRole);
        adminUser.setEstado(true);

        otherUser = new User();
        otherUser.setIdUsuario(5);
        otherUser.setNombre("Carlos");
        otherUser.setApellido("Pérez");
        otherUser.setCorreo("carlos@example.com");
        otherUser.setIdRol(2);
        otherUser.setRol(clientRole);
        otherUser.setEstado(true);
    }

    @Test
    @DisplayName("1. Un administrador no puede desactivarse a sí mismo (debe arrojar 400 Bad Request)")
    void adminCannotDeactivateThemselves() {
        BusinessException exception = assertThrows(BusinessException.class, () ->
                service.disable(4, 4)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
        assertEquals("Por seguridad, no puedes desactivar tu propia cuenta administradora.", exception.getMessage());
        verify(repository, never()).saveUser(any());
    }

    @Test
    @DisplayName("2. Un administrador no puede quitarse su propio rol ADMINISTRADOR (debe arrojar 400 Bad Request)")
    void adminCannotDemoteThemselvesFromAdminRole() {
        when(repository.findUserById(4)).thenReturn(Optional.of(adminUser));
        when(repository.emailExists("admin@agenciaviajes.com", 4)).thenReturn(false);
        when(repository.findRole(2)).thenReturn(Optional.of(clientRole));

        Requests.UserUpdate demoteRequest = new Requests.UserUpdate("Admin", "Principal", "admin@agenciaviajes.com", 2);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                service.update(4, demoteRequest, 4)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
        assertEquals("No puedes revocar tu propio rol de ADMINISTRADOR.", exception.getMessage());
        verify(repository, never()).saveUser(any());
    }

    @Test
    @DisplayName("3. Un administrador puede modificar sus propios campos permitidos manteniendo el rol ADMINISTRADOR")
    void adminCanModifyOwnAllowedFieldsKeepingAdminRole() {
        when(repository.findUserById(4)).thenReturn(Optional.of(adminUser));
        when(repository.emailExists("admin.nuevo@agenciaviajes.com", 4)).thenReturn(false);
        when(repository.findRole(1)).thenReturn(Optional.of(adminRole));
        when(repository.saveUser(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Requests.UserUpdate updateRequest = new Requests.UserUpdate("AdminModificado", "ApellidoModificado", "admin.nuevo@agenciaviajes.com", 1);

        User result = service.update(4, updateRequest, 4);

        assertNotNull(result);
        assertEquals("AdminModificado", result.getNombre());
        assertEquals("ApellidoModificado", result.getApellido());
        assertEquals("admin.nuevo@agenciaviajes.com", result.getCorreo());
        assertEquals(1, result.getIdRol());
        assertEquals("ADMINISTRADOR", result.getRol().getNombre());
        verify(repository).saveUser(adminUser);
    }

    @Test
    @DisplayName("4. Un administrador autorizado puede gestionar (desactivar y actualizar) a otro usuario")
    void authorizedAdminCanManageAnotherUser() {
        // Desactivación de otro usuario por el administrador
        when(repository.findUserById(5)).thenReturn(Optional.of(otherUser));
        when(repository.saveUser(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.disable(5, 4);
        assertFalse(otherUser.isEstado());
        verify(repository).saveUser(otherUser);

        // Actualización de otro usuario por el administrador
        when(repository.findUserById(5)).thenReturn(Optional.of(otherUser));
        when(repository.emailExists("carlos.nuevo@example.com", 5)).thenReturn(false);
        when(repository.findRole(1)).thenReturn(Optional.of(adminRole));

        Requests.UserUpdate promoRequest = new Requests.UserUpdate("Carlos", "Pérez", "carlos.nuevo@example.com", 1);
        User updated = service.update(5, promoRequest, 4);

        assertNotNull(updated);
        assertEquals("ADMINISTRADOR", updated.getRol().getNombre());
        assertEquals(1, updated.getIdRol());
    }

    @Test
    @DisplayName("7. La validación compara inequívocamente el ID autenticado con el ID del usuario objetivo")
    void protectionTriggeredOnlyWhenTargetIdMatchesAuthenticatedId() {
        when(repository.findUserById(5)).thenReturn(Optional.of(otherUser));
        when(repository.emailExists("carlos@example.com", 5)).thenReturn(false);
        when(repository.findRole(2)).thenReturn(Optional.of(clientRole));
        when(repository.saveUser(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // El admin 4 puede actualizar a usuario 5 con rol CLIENTE sin ser bloqueado por auto-democión
        Requests.UserUpdate clientUpdate = new Requests.UserUpdate("Carlos", "Pérez", "carlos@example.com", 2);
        User result = service.update(5, clientUpdate, 4);

        assertNotNull(result);
        assertEquals("CLIENTE", result.getRol().getNombre());

        // Pero si el usuario objetivo es el mismo que el autenticado (4 con 4), se rechaza
        when(repository.findUserById(4)).thenReturn(Optional.of(adminUser));
        when(repository.emailExists("admin@agenciaviajes.com", 4)).thenReturn(false);
        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.update(4, clientUpdate, 4)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.status());
    }

    @Test
    @DisplayName("8. Reversibilidad y consistencia: los métodos sobrecargados sin authenticatedUserId continúan funcionando")
    void overloadsWithoutAuthUserIdWorkForBackwardCompatibility() {
        when(repository.findUserById(5)).thenReturn(Optional.of(otherUser));
        when(repository.saveUser(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.disable(5);
        assertFalse(otherUser.isEstado());
    }
}

