package com.agencytrip.agencytripapi.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

class UserLoginTests {
    @Test void issuesTokenForActiveUsersWithTheirRole() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class); PasswordHasherPort hasher = mock(PasswordHasherPort.class); TokenPort tokens = mock(TokenPort.class);
        User user = new User(); user.setIdUsuario(1); user.setNombre("Ana"); user.setCorreo("ana@test.co"); user.setPassword("hash"); user.setEstado(true); Role role = new Role(); role.setNombre("CLIENTE"); user.setRol(role);
        when(repository.findUserByEmail("ana@test.co")).thenReturn(Optional.of(user)); when(hasher.matches("clave", "hash")).thenReturn(true); when(tokens.create(1, "Ana", "ana@test.co", "CLIENTE")).thenReturn("jwt");
        assertEquals("jwt", new UserService(repository, hasher, tokens).login(new Requests.Login("ana@test.co", "clave")).token());
    }

    @Test void rejectsWhenUserNotFound() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class);
        when(repository.findUserByEmail("inexistente@test.co")).thenReturn(Optional.empty());
        org.junit.jupiter.api.Assertions.assertThrows(
            BusinessException.class,
            () -> new UserService(repository, mock(PasswordHasherPort.class), mock(TokenPort.class))
                    .login(new Requests.Login("inexistente@test.co", "clave"))
        );
    }

    @Test void rejectsWhenUserIsInactive() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class);
        User user = new User(); user.setEstado(false);
        when(repository.findUserByEmail("inactivo@test.co")).thenReturn(Optional.of(user));
        org.junit.jupiter.api.Assertions.assertThrows(
            BusinessException.class,
            () -> new UserService(repository, mock(PasswordHasherPort.class), mock(TokenPort.class))
                    .login(new Requests.Login("inactivo@test.co", "clave"))
        );
    }

    @Test void rejectsWhenPasswordDoesNotMatch() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class);
        PasswordHasherPort hasher = mock(PasswordHasherPort.class);
        User user = new User(); user.setCorreo("ana@test.co"); user.setPassword("hash"); user.setEstado(true);
        when(repository.findUserByEmail("ana@test.co")).thenReturn(Optional.of(user));
        when(hasher.matches("erronea", "hash")).thenReturn(false);
        org.junit.jupiter.api.Assertions.assertThrows(
            BusinessException.class,
            () -> new UserService(repository, hasher, mock(TokenPort.class))
                    .login(new Requests.Login("ana@test.co", "erronea"))
        );
    }
}
