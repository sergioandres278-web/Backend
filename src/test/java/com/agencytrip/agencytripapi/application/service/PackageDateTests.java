package com.agencytrip.agencytripapi.application.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.agencytrip.agencytripapi.application.dto.Requests;
import com.agencytrip.agencytripapi.domain.port.out.TravelRepositoryPort;

class PackageDateTests {
    @Test void rejectsPackagesWithReturnBeforeDeparture() {
        PackageService service = new PackageService(mock(TravelRepositoryPort.class));
        LocalDateTime departure = LocalDateTime.of(2026, 6, 10, 8, 0);
        Requests.PackageTrip request = new Requests.PackageTrip("Plan", null, BigDecimal.TEN, departure, departure.minusDays(1), 2, 1, null, null);
        assertThrows(BusinessException.class, () -> service.create(request));
    }

    @Test void rejectsPackagesWithInactiveOrMissingTransport() {
        TravelRepositoryPort repo = mock(TravelRepositoryPort.class);
        com.agencytrip.agencytripapi.domain.model.Destination dest = new com.agencytrip.agencytripapi.domain.model.Destination();
        dest.setEstado(true);
        when(repo.findDestination(1)).thenReturn(java.util.Optional.of(dest));
        when(repo.findTransport(5)).thenReturn(java.util.Optional.empty());

        PackageService service = new PackageService(repo);
        LocalDateTime departure = LocalDateTime.of(2026, 6, 10, 8, 0);
        Requests.PackageTrip request = new Requests.PackageTrip("Plan", null, BigDecimal.TEN, departure, departure.plusDays(1), 2, 1, 5, null);
        assertThrows(BusinessException.class, () -> service.create(request));
    }

    @Test void rejectsPackagesWithInactiveOrMissingHotel() {
        TravelRepositoryPort repo = mock(TravelRepositoryPort.class);
        com.agencytrip.agencytripapi.domain.model.Destination dest = new com.agencytrip.agencytripapi.domain.model.Destination();
        dest.setEstado(true);
        when(repo.findDestination(1)).thenReturn(java.util.Optional.of(dest));
        when(repo.findHotel(3)).thenReturn(java.util.Optional.empty());

        PackageService service = new PackageService(repo);
        LocalDateTime departure = LocalDateTime.of(2026, 6, 10, 8, 0);
        Requests.PackageTrip request = new Requests.PackageTrip("Plan", null, BigDecimal.TEN, departure, departure.plusDays(1), 2, 1, null, 3);
        assertThrows(BusinessException.class, () -> service.create(request));
    }
}
