package com.agencytrip.agencytripapi.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.agencytrip.agencytripapi.application.dto.Requests;
import com.agencytrip.agencytripapi.application.port.out.PasswordHasherPort;
import com.agencytrip.agencytripapi.application.port.out.TokenPort;
import com.agencytrip.agencytripapi.domain.model.User;
import com.agencytrip.agencytripapi.domain.port.out.TravelRepositoryPort;

class UserRegistrationTests {
    @Test void registersAnActiveClientWithHashedPassword() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class); PasswordHasherPort hasher = mock(PasswordHasherPort.class);
        when(repository.emailExists("ana@test.co", null)).thenReturn(false); when(hasher.hash("clave")).thenReturn("hash"); when(repository.saveUser(any())).thenAnswer(invocation -> invocation.getArgument(0));
        User user = new UserService(repository, hasher, mock(TokenPort.class)).register(new Requests.Register("Ana", "Pérez", "ana@test.co", "clave"));
        assertEquals("hash", user.getPassword()); assertEquals(2, user.getIdRol()); assertTrue(user.isEstado());
    }

    @Test void rejectsWhenEmailAlreadyExists() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class);
        when(repository.emailExists("ana@test.co", null)).thenReturn(true);
        org.junit.jupiter.api.Assertions.assertThrows(
            BusinessException.class,
            () -> new UserService(repository, mock(PasswordHasherPort.class), mock(TokenPort.class))
                    .register(new Requests.Register("Ana", "Pérez", "ana@test.co", "clave"))
        );
    }
}
