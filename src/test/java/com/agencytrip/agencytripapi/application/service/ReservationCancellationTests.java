package com.agencytrip.agencytripapi.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.agencytrip.agencytripapi.application.dto.Requests;
import com.agencytrip.agencytripapi.domain.model.PackageTrip;
import com.agencytrip.agencytripapi.domain.model.Reservation;
import com.agencytrip.agencytripapi.domain.port.out.TravelRepositoryPort;

class ReservationCancellationTests {
    @Test void restoresCapacityWhenReservationIsCancelled() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class);
        Reservation reservation = new Reservation(); reservation.setIdPaquete(3); reservation.setCantidadPersonas(2); reservation.setEstado("PENDIENTE");
        PackageTrip trip = new PackageTrip(); trip.setCupos(4);
        when(repository.findReservation(7)).thenReturn(Optional.of(reservation));
        when(repository.findPackage(3)).thenReturn(Optional.of(trip));
        when(repository.saveReservation(any())).thenAnswer(invocation -> invocation.getArgument(0));
        new ReservationService(repository).changeStatus(7, new Requests.ReservationStatus("CANCELADA"));
        assertEquals("CANCELADA", reservation.getEstado());
        assertEquals(6, trip.getCupos());
    }
}
