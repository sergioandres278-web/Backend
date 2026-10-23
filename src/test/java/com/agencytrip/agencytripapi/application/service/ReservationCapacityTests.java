package com.agencytrip.agencytripapi.application.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.agencytrip.agencytripapi.application.dto.Requests;
import com.agencytrip.agencytripapi.domain.model.PackageTrip;
import com.agencytrip.agencytripapi.domain.port.out.TravelRepositoryPort;

class ReservationCapacityTests {
    @Test void rejectsReservationWhenRequestedPeopleExceedCapacity() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class);
        PackageTrip trip = new PackageTrip(); trip.setEstado(true); trip.setCupos(1); trip.setPrecio(BigDecimal.TEN);
        when(repository.findPackage(1)).thenReturn(Optional.of(trip));
        assertThrows(BusinessException.class, () -> new ReservationService(repository).create(1, new Requests.Reservation(1, 2)));
    }
}
