package com.agencytrip.agencytripapi.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.agencytrip.agencytripapi.application.dto.Requests;
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
}
