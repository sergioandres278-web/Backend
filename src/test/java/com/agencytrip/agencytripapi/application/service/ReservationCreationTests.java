package com.agencytrip.agencytripapi.application.service;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.agencytrip.agencytripapi.application.dto.Requests;
import com.agencytrip.agencytripapi.domain.model.PackageTrip;
import com.agencytrip.agencytripapi.domain.model.Reservation;
import com.agencytrip.agencytripapi.domain.port.out.TravelRepositoryPort;

class ReservationCreationTests {

    @Test
    void createsPendingReservationWithoutDiscountingPackageCapacity() {

        TravelRepositoryPort repository =
            mock(TravelRepositoryPort.class);

        PackageTrip trip = new PackageTrip();

        trip.setIdPaquete(4);
        trip.setEstado(true);
        trip.setCupos(5);
        trip.setPrecio(
            new BigDecimal("120000")
        );

        when(repository.findPackage(4))
            .thenReturn(Optional.of(trip));

        when(repository.saveReservation(any()))
            .thenAnswer(invocation ->
                invocation.getArgument(0)
            );

        Reservation reservation =
            new ReservationService(repository)
                .create(
                    9,
                    new Requests.Reservation(4, 2)
                );

        assertEquals(
            "240000",
            reservation.getPrecioTotal()
                .toPlainString()
        );

        assertEquals(
            "PENDIENTE",
            reservation.getEstado()
        );
        // Crear la reserva no debe descontar cupos.
        assertEquals(
            5,
            trip.getCupos()
        );
    }

    @Test
    void rejectsReservationWhenPackageDoesNotExist() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class);
        when(repository.findPackage(99)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(
            BusinessException.class,
            () -> new ReservationService(repository).create(1, new Requests.Reservation(99, 2))
        );
    }

    @Test
    void rejectsReservationWhenPackageIsInactive() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class);
        PackageTrip trip = new PackageTrip();
        trip.setIdPaquete(4);
        trip.setEstado(false);
        trip.setCupos(10);
        when(repository.findPackage(4)).thenReturn(Optional.of(trip));

        org.junit.jupiter.api.Assertions.assertThrows(
            BusinessException.class,
            () -> new ReservationService(repository).create(1, new Requests.Reservation(4, 2))
        );
    }
}