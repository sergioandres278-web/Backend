package com.agencytrip.agencytripapi.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.agencytrip.agencytripapi.application.dto.Requests;
import com.agencytrip.agencytripapi.domain.model.Reservation;
import com.agencytrip.agencytripapi.domain.port.out.TravelRepositoryPort;

class PaymentAuthorizationTests {
    @Test void forbidsPaymentsForAnotherUsersReservation() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class);
        Reservation reservation = new Reservation(); reservation.setIdUsuario(4);
        when(repository.findReservation(1)).thenReturn(Optional.of(reservation));
        BusinessException exception = assertThrows(BusinessException.class, () -> new PaymentService(repository).create(5, new Requests.Payment(1, "PSE")));
        assertEquals(HttpStatus.FORBIDDEN, exception.status());
    }
}
