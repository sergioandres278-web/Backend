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
import com.agencytrip.agencytripapi.domain.model.Payment;
import com.agencytrip.agencytripapi.domain.model.Reservation;
import com.agencytrip.agencytripapi.domain.port.out.TravelRepositoryPort;

class PaymentCreationTests {

    @Test
    void approvesPaymentConfirmsReservationAndDiscountsCapacity() {

        TravelRepositoryPort repository =
            mock(TravelRepositoryPort.class);

        Reservation reservation = new Reservation();

        reservation.setIdReserva(8);
        reservation.setIdUsuario(3);
        reservation.setIdPaquete(4);
        reservation.setCantidadPersonas(2);
        reservation.setPrecioTotal(
            new BigDecimal("50000")
        );

        PackageTrip trip = new PackageTrip();

        trip.setIdPaquete(4);
        trip.setCupos(5);
        trip.setEstado(true);

        when(repository.findReservation(8))
            .thenReturn(Optional.of(reservation));

        when(repository.findPackage(4))
            .thenReturn(Optional.of(trip));

        when(repository.hasApprovedPayment(8))
            .thenReturn(false);

        when(repository.savePayment(any()))
            .thenAnswer(invocation ->
                invocation.getArgument(0)
            );

        when(repository.saveReservation(any()))
            .thenAnswer(invocation ->
                invocation.getArgument(0)
            );

        when(repository.savePackage(any()))
            .thenAnswer(invocation ->
                invocation.getArgument(0)
            );

        Payment payment =
            new PaymentService(repository)
                .create(
                    3,
                    new Requests.Payment(8, "pse")
                )
                .payment();

        assertEquals(
            "APROBADO",
            payment.getEstado()
        );

        assertEquals(
            "PSE",
            payment.getMetodoPago()
        );

        assertEquals(
            "CONFIRMADA",
            reservation.getEstado()
        );

        assertEquals(
            3,
            trip.getCupos()
        );
    }

    @Test
    void rejectsPaymentWhenPackageIsInactive() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class);

        Reservation reservation = new Reservation();
        reservation.setIdReserva(8);
        reservation.setIdUsuario(3);
        reservation.setIdPaquete(4);
        reservation.setCantidadPersonas(2);
        reservation.setPrecioTotal(new BigDecimal("50000"));

        PackageTrip trip = new PackageTrip();
        trip.setIdPaquete(4);
        trip.setCupos(5);
        trip.setEstado(false);

        when(repository.findReservation(8)).thenReturn(Optional.of(reservation));
        when(repository.findPackage(4)).thenReturn(Optional.of(trip));
        when(repository.hasApprovedPayment(8)).thenReturn(false);

        org.junit.jupiter.api.Assertions.assertThrows(
            BusinessException.class,
            () -> new PaymentService(repository).create(3, new Requests.Payment(8, "PSE"))
        );
    }

    @Test
    void rejectsPaymentWhenReservationIsAlreadyConfirmed() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class);

        Reservation reservation = new Reservation();
        reservation.setIdReserva(8);
        reservation.setIdUsuario(3);
        reservation.setEstado("CONFIRMADA");

        when(repository.findReservation(8)).thenReturn(Optional.of(reservation));

        org.junit.jupiter.api.Assertions.assertThrows(
            BusinessException.class,
            () -> new PaymentService(repository).create(3, new Requests.Payment(8, "PSE"))
        );
    }

    @Test
    void rejectsPaymentWhenReservationDoesNotExist() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class);
        when(repository.findReservation(99)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(
            BusinessException.class,
            () -> new PaymentService(repository).create(1, new Requests.Payment(99, "PSE"))
        );
    }

    @Test
    void rejectsPaymentWhenReservationIsCancelled() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class);
        Reservation reservation = new Reservation();
        reservation.setIdReserva(8);
        reservation.setIdUsuario(3);
        reservation.setEstado("CANCELADA");
        when(repository.findReservation(8)).thenReturn(Optional.of(reservation));

        org.junit.jupiter.api.Assertions.assertThrows(
            BusinessException.class,
            () -> new PaymentService(repository).create(3, new Requests.Payment(8, "PSE"))
        );
    }

    @Test
    void rejectsPaymentWhenAlreadyHasApprovedPayment() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class);
        Reservation reservation = new Reservation();
        reservation.setIdReserva(8);
        reservation.setIdUsuario(3);
        reservation.setEstado("PENDIENTE");
        when(repository.findReservation(8)).thenReturn(Optional.of(reservation));
        when(repository.hasApprovedPayment(8)).thenReturn(true);

        org.junit.jupiter.api.Assertions.assertThrows(
            BusinessException.class,
            () -> new PaymentService(repository).create(3, new Requests.Payment(8, "PSE"))
        );
    }

    @Test
    void rejectsPaymentWithInvalidMethod() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class);
        Reservation reservation = new Reservation();
        reservation.setIdReserva(8);
        reservation.setIdUsuario(3);
        reservation.setEstado("PENDIENTE");
        when(repository.findReservation(8)).thenReturn(Optional.of(reservation));
        when(repository.hasApprovedPayment(8)).thenReturn(false);

        org.junit.jupiter.api.Assertions.assertThrows(
            BusinessException.class,
            () -> new PaymentService(repository).create(3, new Requests.Payment(8, "CRIPTOMONEDA"))
        );
    }

    @Test
    void rejectsPaymentWhenInsufficientCapacityAtPaymentTime() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class);
        Reservation reservation = new Reservation();
        reservation.setIdReserva(8);
        reservation.setIdUsuario(3);
        reservation.setIdPaquete(4);
        reservation.setCantidadPersonas(5);
        reservation.setEstado("PENDIENTE");

        PackageTrip trip = new PackageTrip();
        trip.setIdPaquete(4);
        trip.setCupos(2); // Only 2 left, but reservation needs 5
        trip.setEstado(true);

        when(repository.findReservation(8)).thenReturn(Optional.of(reservation));
        when(repository.hasApprovedPayment(8)).thenReturn(false);
        when(repository.findPackage(4)).thenReturn(Optional.of(trip));

        org.junit.jupiter.api.Assertions.assertThrows(
            BusinessException.class,
            () -> new PaymentService(repository).create(3, new Requests.Payment(8, "PSE"))
        );
    }
}