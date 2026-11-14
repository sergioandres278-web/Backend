package com.agencytrip.agencytripapi.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.agencytrip.agencytripapi.application.dto.Requests;
import com.agencytrip.agencytripapi.domain.model.PackageTrip;
import com.agencytrip.agencytripapi.domain.model.Payment;
import com.agencytrip.agencytripapi.domain.model.Reservation;
import com.agencytrip.agencytripapi.domain.port.out.TravelRepositoryPort;

class PaymentStatusTests {
    @Test void returnsReservationToPendingWhenPaymentIsRejected() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class);
        Payment payment = new Payment(); payment.setIdReserva(2);
        Reservation reservation = new Reservation();
        when(repository.findPayment(6)).thenReturn(Optional.of(payment)); when(repository.findReservation(2)).thenReturn(Optional.of(reservation)); when(repository.savePayment(any())).thenAnswer(invocation -> invocation.getArgument(0));
        new PaymentService(repository).updateStatus(6, new Requests.PaymentStatus("RECHAZADO"));
        assertEquals("RECHAZADO", payment.getEstado()); assertEquals("PENDIENTE", reservation.getEstado());
    }

    @Test
    void restoresCapacityWhenApprovedPaymentIsRejected() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class);

        Payment payment = new Payment();
        payment.setIdReserva(2);
        payment.setEstado("APROBADO");

        Reservation reservation = new Reservation();
        reservation.setIdPaquete(4);
        reservation.setCantidadPersonas(3);
        reservation.setEstado("CONFIRMADA");

        PackageTrip trip = new PackageTrip();
        trip.setIdPaquete(4);
        trip.setCupos(2);

        when(repository.findPayment(6)).thenReturn(Optional.of(payment));
        when(repository.findReservation(2)).thenReturn(Optional.of(reservation));
        when(repository.findPackage(4)).thenReturn(Optional.of(trip));
        when(repository.savePayment(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.savePackage(any())).thenAnswer(invocation -> invocation.getArgument(0));

        new PaymentService(repository).updateStatus(6, new Requests.PaymentStatus("RECHAZADO"));

        assertEquals("RECHAZADO", payment.getEstado());
        assertEquals("PENDIENTE", reservation.getEstado());
        assertEquals(5, trip.getCupos());
    }

    @Test
    void doesNotAlterCancelledReservationWhenPaymentIsRejected() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class);

        Payment payment = new Payment();
        payment.setIdReserva(2);
        payment.setEstado("PENDIENTE");

        Reservation reservation = new Reservation();
        reservation.setEstado("CANCELADA");

        when(repository.findPayment(6)).thenReturn(Optional.of(payment));
        when(repository.findReservation(2)).thenReturn(Optional.of(reservation));
        when(repository.savePayment(any())).thenAnswer(invocation -> invocation.getArgument(0));

        new PaymentService(repository).updateStatus(6, new Requests.PaymentStatus("RECHAZADO"));

        assertEquals("RECHAZADO", payment.getEstado());
        assertEquals("CANCELADA", reservation.getEstado());
    }

    @Test
    void rejectsApprovingPaymentForCancelledReservation() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class);

        Payment payment = new Payment();
        payment.setIdReserva(2);
        payment.setEstado("PENDIENTE");

        Reservation reservation = new Reservation();
        reservation.setEstado("CANCELADA");

        when(repository.findPayment(6)).thenReturn(Optional.of(payment));
        when(repository.hasApprovedPayment(2)).thenReturn(false);
        when(repository.findReservation(2)).thenReturn(Optional.of(reservation));

        org.junit.jupiter.api.Assertions.assertThrows(
            BusinessException.class,
            () -> new PaymentService(repository).updateStatus(6, new Requests.PaymentStatus("APROBADO"))
        );
    }

    @Test
    void rejectsApprovingPaymentWhenPackageIsInactive() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class);

        Payment payment = new Payment();
        payment.setIdReserva(2);
        payment.setEstado("PENDIENTE");

        Reservation reservation = new Reservation();
        reservation.setIdPaquete(4);
        reservation.setCantidadPersonas(2);
        reservation.setEstado("PENDIENTE");

        PackageTrip trip = new PackageTrip();
        trip.setCupos(5);
        trip.setEstado(false);

        when(repository.findPayment(6)).thenReturn(Optional.of(payment));
        when(repository.hasApprovedPayment(2)).thenReturn(false);
        when(repository.findReservation(2)).thenReturn(Optional.of(reservation));
        when(repository.findPackage(4)).thenReturn(Optional.of(trip));

        org.junit.jupiter.api.Assertions.assertThrows(
            BusinessException.class,
            () -> new PaymentService(repository).updateStatus(6, new Requests.PaymentStatus("APROBADO"))
        );
    }

    @Test
    void rejectsApprovingAnAlreadyApprovedPayment() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class);

        Payment payment = new Payment();
        payment.setIdReserva(2);
        payment.setEstado("APROBADO");

        when(repository.findPayment(6)).thenReturn(Optional.of(payment));

        org.junit.jupiter.api.Assertions.assertThrows(
            BusinessException.class,
            () -> new PaymentService(repository).updateStatus(6, new Requests.PaymentStatus("APROBADO"))
        );
    }

    @Test
    void rejectsRejectingAnAlreadyRejectedPayment() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class);

        Payment payment = new Payment();
        payment.setIdReserva(2);
        payment.setEstado("RECHAZADO");

        when(repository.findPayment(6)).thenReturn(Optional.of(payment));

        org.junit.jupiter.api.Assertions.assertThrows(
            BusinessException.class,
            () -> new PaymentService(repository).updateStatus(6, new Requests.PaymentStatus("RECHAZADO"))
        );
    }
}
