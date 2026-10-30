package com.agencytrip.agencytripapi.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.agencytrip.agencytripapi.application.dto.Requests;
import com.agencytrip.agencytripapi.application.port.out.PasswordHasherPort;
import com.agencytrip.agencytripapi.application.port.out.TokenPort;
import com.agencytrip.agencytripapi.domain.model.Role;
import com.agencytrip.agencytripapi.domain.model.User;
import com.agencytrip.agencytripapi.domain.port.out.TravelRepositoryPort;

class UserUpdateTests {
    @Test void updatesUserRoleWhenEmailIsAvailable() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class); User user = new User(); Role role = new Role(); role.setNombre("ADMINISTRADOR");
        when(repository.findUserById(1)).thenReturn(Optional.of(user)); when(repository.emailExists("admin@test.co", 1)).thenReturn(false); when(repository.findRole(1)).thenReturn(Optional.of(role)); when(repository.saveUser(any())).thenAnswer(invocation -> invocation.getArgument(0));
        User updated = new UserService(repository, mock(PasswordHasherPort.class), mock(TokenPort.class)).update(1, new Requests.UserUpdate("Ana", "Pérez", "admin@test.co", 1));
        assertEquals("ADMINISTRADOR", updated.getRol().getNombre()); assertEquals("admin@test.co", updated.getCorreo());
    }

    @Test void rejectsWhenUserNotFound() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class);
        when(repository.findUserById(99)).thenReturn(Optional.empty());
        org.junit.jupiter.api.Assertions.assertThrows(
            BusinessException.class,
            () -> new UserService(repository, mock(PasswordHasherPort.class), mock(TokenPort.class))
                    .update(99, new Requests.UserUpdate("Ana", "Pérez", "admin@test.co", 1))
        );
    }

    @Test void rejectsWhenEmailAlreadyExistsForAnotherUser() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class);
        User user = new User();
        when(repository.findUserById(1)).thenReturn(Optional.of(user));
        when(repository.emailExists("duplicado@test.co", 1)).thenReturn(true);
        org.junit.jupiter.api.Assertions.assertThrows(
            BusinessException.class,
            () -> new UserService(repository, mock(PasswordHasherPort.class), mock(TokenPort.class))
                    .update(1, new Requests.UserUpdate("Ana", "Pérez", "duplicado@test.co", 1))
        );
    }

    @Test void rejectsWhenRoleDoesNotExist() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class);
        User user = new User();
        when(repository.findUserById(1)).thenReturn(Optional.of(user));
        when(repository.emailExists("admin@test.co", 1)).thenReturn(false);
        when(repository.findRole(99)).thenReturn(Optional.empty());
        org.junit.jupiter.api.Assertions.assertThrows(
            BusinessException.class,
            () -> new UserService(repository, mock(PasswordHasherPort.class), mock(TokenPort.class))
                    .update(1, new Requests.UserUpdate("Ana", "Pérez", "admin@test.co", 99))
        );
    }
}
