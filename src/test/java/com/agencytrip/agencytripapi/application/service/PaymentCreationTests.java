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
}