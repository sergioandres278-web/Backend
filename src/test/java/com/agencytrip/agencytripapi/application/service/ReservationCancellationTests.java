package com.agencytrip.agencytripapi.application.service;

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

class ReservationCancellationTests {

    @Test
    void restoresCapacityWhenConfirmedReservationIsCancelled() {

        TravelRepositoryPort repository =
            mock(TravelRepositoryPort.class);

        Reservation reservation =
            new Reservation();

        reservation.setIdPaquete(3);
        reservation.setCantidadPersonas(2);
        reservation.setEstado("CONFIRMADA");

        PackageTrip trip =
            new PackageTrip();

        trip.setCupos(4);

        when(repository.findReservation(7))
            .thenReturn(Optional.of(reservation));

        when(repository.findPackage(3))
            .thenReturn(Optional.of(trip));

        when(repository.savePackage(any()))
            .thenAnswer(invocation ->
                invocation.getArgument(0)
            );

        when(repository.saveReservation(any()))
            .thenAnswer(invocation ->
                invocation.getArgument(0)
            );

        new ReservationService(repository)
            .changeStatus(
                7,
                new Requests.ReservationStatus(
                    "CANCELADA"
                )
            );

        assertEquals(
            "CANCELADA",
            reservation.getEstado()
        );

        // La reserva confirmada había consumido 2 cupos.
        // Al cancelarla, esos 2 cupos se devuelven.
        assertEquals(
            6,
            trip.getCupos()
        );
    }

    @Test
    void doesNotAlterCapacityWhenPendingReservationIsCancelled() {

        TravelRepositoryPort repository =
            mock(TravelRepositoryPort.class);

        Reservation reservation =
            new Reservation();

        reservation.setIdPaquete(3);
        reservation.setCantidadPersonas(2);
        reservation.setEstado("PENDIENTE");

        when(repository.findReservation(7))
            .thenReturn(Optional.of(reservation));

        when(repository.saveReservation(any()))
            .thenAnswer(invocation ->
                invocation.getArgument(0)
            );

        new ReservationService(repository)
            .changeStatus(
                7,
                new Requests.ReservationStatus(
                    "CANCELADA"
                )
            );

        assertEquals(
            "CANCELADA",
            reservation.getEstado()
        );
    }

    @Test
    void confirmsReservationAndDeductsCapacityWhenPendingReservationIsConfirmed() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class);

        Reservation reservation = new Reservation();
        reservation.setIdPaquete(3);
        reservation.setCantidadPersonas(2);
        reservation.setEstado("PENDIENTE");

        PackageTrip trip = new PackageTrip();
        trip.setCupos(6);
        trip.setEstado(true);

        when(repository.findReservation(7)).thenReturn(Optional.of(reservation));
        when(repository.findPackage(3)).thenReturn(Optional.of(trip));
        when(repository.savePackage(any())).thenAnswer(i -> i.getArgument(0));
        when(repository.saveReservation(any())).thenAnswer(i -> i.getArgument(0));

        new ReservationService(repository).changeStatus(7, new Requests.ReservationStatus("CONFIRMADA"));

        assertEquals("CONFIRMADA", reservation.getEstado());
        assertEquals(4, trip.getCupos());
    }

    @Test
    void rejectsConfirmingPendingReservationWhenInsufficientCapacity() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class);

        Reservation reservation = new Reservation();
        reservation.setIdPaquete(3);
        reservation.setCantidadPersonas(5);
        reservation.setEstado("PENDIENTE");

        PackageTrip trip = new PackageTrip();
        trip.setCupos(2);
        trip.setEstado(true);

        when(repository.findReservation(7)).thenReturn(Optional.of(reservation));
        when(repository.findPackage(3)).thenReturn(Optional.of(trip));

        org.junit.jupiter.api.Assertions.assertThrows(
            BusinessException.class,
            () -> new ReservationService(repository).changeStatus(7, new Requests.ReservationStatus("CONFIRMADA"))
        );
    }

    @Test
    void restoresCapacityWhenConfirmedReservationIsSetBackToPending() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class);

        Reservation reservation = new Reservation();
        reservation.setIdPaquete(3);
        reservation.setCantidadPersonas(2);
        reservation.setEstado("CONFIRMADA");

        PackageTrip trip = new PackageTrip();
        trip.setCupos(4);

        when(repository.findReservation(7)).thenReturn(Optional.of(reservation));
        when(repository.findPackage(3)).thenReturn(Optional.of(trip));
        when(repository.savePackage(any())).thenAnswer(i -> i.getArgument(0));
        when(repository.saveReservation(any())).thenAnswer(i -> i.getArgument(0));

        new ReservationService(repository).changeStatus(7, new Requests.ReservationStatus("PENDIENTE"));

        assertEquals("PENDIENTE", reservation.getEstado());
        assertEquals(6, trip.getCupos());
    }

    @Test
    void preventsReactivatingCancelledReservation() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class);

        Reservation reservation = new Reservation();
        reservation.setIdPaquete(3);
        reservation.setCantidadPersonas(2);
        reservation.setEstado("CANCELADA");

        when(repository.findReservation(7)).thenReturn(Optional.of(reservation));

        org.junit.jupiter.api.Assertions.assertThrows(
            BusinessException.class,
            () -> new ReservationService(repository).changeStatus(7, new Requests.ReservationStatus("CONFIRMADA"))
        );
    }

    @Test
    void preventsConfirmingAnAlreadyConfirmedReservation() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class);

        Reservation reservation = new Reservation();
        reservation.setIdPaquete(3);
        reservation.setCantidadPersonas(2);
        reservation.setEstado("CONFIRMADA");

        when(repository.findReservation(7)).thenReturn(Optional.of(reservation));

        org.junit.jupiter.api.Assertions.assertThrows(
            BusinessException.class,
            () -> new ReservationService(repository).changeStatus(7, new Requests.ReservationStatus("CONFIRMADA"))
        );
    }
}