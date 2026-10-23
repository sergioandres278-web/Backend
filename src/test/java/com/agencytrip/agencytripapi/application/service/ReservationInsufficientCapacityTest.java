package com.agencytrip.agencytripapi.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.agencytrip.agencytripapi.application.dto.Requests;
import com.agencytrip.agencytripapi.domain.model.PackageTrip;
import com.agencytrip.agencytripapi.domain.port.out.TravelRepositoryPort;
import com.agencytrip.agencytripapi.application.service.BusinessException;

@ExtendWith(MockitoExtension.class)
class ReservationInsufficientCapacityTest {

    @Mock
    private TravelRepositoryPort repository;

    @InjectMocks
    private ReservationService reservationService;

    @Test
    void rejectsReservationWhenNotEnoughCapacity() {
        // Given – package with limited capacity
        int userId = 1;
        int packageId = 10;
        int requestedPersons = 5; // more than available
        PackageTrip packageTrip = new PackageTrip();
        packageTrip.setIdPaquete(packageId);
        packageTrip.setCupos(3); // only 3 seats available
        packageTrip.setEstado(true);
        packageTrip.setPrecio(java.math.BigDecimal.ONE);

        when(repository.findPackage(packageId)).thenReturn(Optional.of(packageTrip));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> reservationService.create(userId, new Requests.Reservation(packageId, requestedPersons)));
        assertEquals("No hay suficientes cupos disponibles.", ex.getMessage());

        verify(repository, never()).saveReservation(any());
        verify(repository, never()).savePackage(any());
    }
}
