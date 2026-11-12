package com.agencytrip.agencytripapi.application.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.agencytrip.agencytripapi.application.port.out.PasswordHasherPort;
import com.agencytrip.agencytripapi.application.port.out.TokenPort;
import com.agencytrip.agencytripapi.domain.model.User;
import com.agencytrip.agencytripapi.domain.port.out.TravelRepositoryPort;

class UserDeactivationTests {
    @Test void deactivatesAnActiveUser() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class); User user = new User(); user.setEstado(true);
        when(repository.findUserById(2)).thenReturn(Optional.of(user)); when(repository.saveUser(any())).thenAnswer(invocation -> invocation.getArgument(0));
        new UserService(repository, mock(PasswordHasherPort.class), mock(TokenPort.class)).disable(2);
        assertFalse(user.isEstado());
    }

    @Test void rejectsWhenUserNotFound() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class);
        when(repository.findUserById(99)).thenReturn(Optional.empty());
        org.junit.jupiter.api.Assertions.assertThrows(
            BusinessException.class,
            () -> new UserService(repository, mock(PasswordHasherPort.class), mock(TokenPort.class)).disable(99)
        );
    }

    @Test void rejectsWhenUserAlreadyDeactivated() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class);
        User user = new User(); user.setEstado(false);
        when(repository.findUserById(2)).thenReturn(Optional.of(user));
        org.junit.jupiter.api.Assertions.assertThrows(
            BusinessException.class,
            () -> new UserService(repository, mock(PasswordHasherPort.class), mock(TokenPort.class)).disable(2)
        );
    }
}
