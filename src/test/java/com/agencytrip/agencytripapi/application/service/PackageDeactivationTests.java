package com.agencytrip.agencytripapi.application.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.agencytrip.agencytripapi.domain.model.PackageTrip;
import com.agencytrip.agencytripapi.domain.port.out.TravelRepositoryPort;

class PackageDeactivationTests {
    @Test void preventsDeactivatingAnAlreadyInactivePackage() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class);
        PackageTrip trip = new PackageTrip(); trip.setEstado(false);
        when(repository.findPackage(1)).thenReturn(Optional.of(trip));
        assertThrows(BusinessException.class, () -> new PackageService(repository).disable(1));
    }
}
