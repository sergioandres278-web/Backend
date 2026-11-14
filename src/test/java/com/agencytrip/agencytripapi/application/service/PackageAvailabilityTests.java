package com.agencytrip.agencytripapi.application.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.agencytrip.agencytripapi.application.dto.Requests;
import com.agencytrip.agencytripapi.domain.model.Destination;
import com.agencytrip.agencytripapi.domain.port.out.TravelRepositoryPort;

class PackageAvailabilityTests {
    @Test void rejectsPackagesWhenDestinationIsInactive() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class);
        Destination destination = new Destination(); destination.setEstado(false);
        when(repository.findDestination(1)).thenReturn(Optional.of(destination));
        PackageService service = new PackageService(repository);
        LocalDateTime departure = LocalDateTime.of(2026, 6, 10, 8, 0);
        Requests.PackageTrip request = new Requests.PackageTrip("Plan", null, BigDecimal.TEN, departure, departure.plusDays(1), 2, 1, null, null);
        assertThrows(BusinessException.class, () -> service.create(request));
    }
}
