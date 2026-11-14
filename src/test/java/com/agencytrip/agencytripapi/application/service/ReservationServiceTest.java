package com.agencytrip.agencytripapi.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.agencytrip.agencytripapi.application.dto.Requests;
import com.agencytrip.agencytripapi.domain.model.PackageTrip;
import com.agencytrip.agencytripapi.domain.model.Reservation;
import com.agencytrip.agencytripapi.domain.port.out.TravelRepositoryPort;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private TravelRepositoryPort repository;

    @InjectMocks
    private ReservationService reservationService;

    @Test
    void createsValidReservationWithoutChangingPackageCapacity() {
        // Given
        int userId = 42;
        int packageId = 7;
        int persons = 3;
        BigDecimal pricePerPerson = new BigDecimal("1250.00");
        PackageTrip packageTrip = new PackageTrip();
        packageTrip.setIdPaquete(packageId);
        packageTrip.setPrecio(pricePerPerson);
        packageTrip.setCupos(10); // enough capacity
        packageTrip.setEstado(true); // active

        when(repository.findPackage(packageId)).thenReturn(Optional.of(packageTrip));
        // Simulate saveReservation returning the persisted reservation
        when(repository.saveReservation(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Reservation result = reservationService.create(userId, new Requests.Reservation(packageId, persons));

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getIdUsuario());
        assertEquals(packageId, result.getIdPaquete());
        assertEquals(persons, result.getCantidadPersonas());
        assertEquals("PENDIENTE", result.getEstado());
        assertEquals(pricePerPerson.multiply(BigDecimal.valueOf(persons)), result.getPrecioTotal());

        // Verify repository interactions
        verify(repository).findPackage(packageId);
        verify(repository).saveReservation(any(Reservation.class));
        verify(repository, never()).savePackage(any()); // capacity must not be altered here
    }
}
