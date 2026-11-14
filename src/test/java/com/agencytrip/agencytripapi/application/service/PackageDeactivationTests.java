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

    @Test void rejectsDisablingNonExistentPackage() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class);
        when(repository.findPackage(99)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> new PackageService(repository).disable(99));
    }

    @Test void rejectsUpdatingNonExistentPackage() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class);
        when(repository.findPackage(99)).thenReturn(Optional.empty());
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        com.agencytrip.agencytripapi.application.dto.Requests.PackageTrip req =
            new com.agencytrip.agencytripapi.application.dto.Requests.PackageTrip("Plan", "Desc", java.math.BigDecimal.TEN, now, now.plusDays(1), 5, 1, null, null);
        assertThrows(BusinessException.class, () -> new PackageService(repository).update(99, req));
    }

    @Test void rejectsUpdatingDeactivatedPackage() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class);
        PackageTrip trip = new PackageTrip(); trip.setEstado(false);
        when(repository.findPackage(1)).thenReturn(Optional.of(trip));
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        com.agencytrip.agencytripapi.application.dto.Requests.PackageTrip req =
            new com.agencytrip.agencytripapi.application.dto.Requests.PackageTrip("Plan", "Desc", java.math.BigDecimal.TEN, now, now.plusDays(1), 5, 1, null, null);
        assertThrows(BusinessException.class, () -> new PackageService(repository).update(1, req));
    }
}
