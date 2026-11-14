package com.agencytrip.agencytripapi.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.agencytrip.agencytripapi.application.dto.Requests;
import com.agencytrip.agencytripapi.domain.model.PackageTrip;
import com.agencytrip.agencytripapi.domain.model.Reservation;
import com.agencytrip.agencytripapi.domain.port.out.TravelRepositoryPort;

@ExtendWith(MockitoExtension.class)
class ReservationCancellationTest {

    @Mock
    private TravelRepositoryPort repository;

    @InjectMocks
    private ReservationService reservationService;

    @Test
    void cancelsConfirmedReservationAndRestoresCapacity() {
        // Given – a confirmed reservation
        int reservationId = 8;
        int userId = 3;
        int packageId = 15;
        int persons = 4;
        Reservation reservation = new Reservation();
        reservation.setIdReserva(reservationId);
        reservation.setIdUsuario(userId);
        reservation.setIdPaquete(packageId);
        reservation.setCantidadPersonas(persons);
        reservation.setEstado("CONFIRMADA");

        // Package with capacity after reservation was confirmed (original 10, now 6)
        PackageTrip packageTrip = new PackageTrip();
        packageTrip.setIdPaquete(packageId);
        packageTrip.setCupos(6); // capacity reduced already
        packageTrip.setEstado(true);
        packageTrip.setPrecio(java.math.BigDecimal.ONE);

        // Mock repository behavior
        when(repository.findReservationForUpdate(reservationId)).thenReturn(Optional.of(reservation));
        when(repository.findPackageForUpdate(packageId)).thenReturn(Optional.of(packageTrip));
        when(repository.savePackage(any(PackageTrip.class))).thenAnswer(i -> i.getArgument(0));
        when(repository.saveReservation(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

        // When – cancel reservation
        Reservation result = reservationService.changeStatus(reservationId, new Requests.ReservationStatus("CANCELADA"));

        // Then – reservation state updated
        assertEquals("CANCELADA", result.getEstado());
        // Capacity must be increased by the number of persons
        ArgumentCaptor<PackageTrip> captor = ArgumentCaptor.forClass(PackageTrip.class);
        verify(repository).savePackage(captor.capture());
        PackageTrip savedPackage = captor.getValue();
        assertEquals(10, savedPackage.getCupos(), "Capacity should be restored to original amount");
        // Reservation should be saved with new state
        verify(repository).saveReservation(any(Reservation.class));
    }

    @Test
    void cancelsAlreadyCancelledReservationThrows() {
        // Given – a reservation already cancelled
        int reservationId = 9;
        Reservation reservation = new Reservation();
        reservation.setIdReserva(reservationId);
        reservation.setEstado("CANCELADA");

        when(repository.findReservationForUpdate(reservationId)).thenReturn(Optional.of(reservation));


        // When/Then – attempting to cancel again throws BusinessException
        assertThrows(BusinessException.class,
                () -> reservationService.changeStatus(reservationId, new Requests.ReservationStatus("CANCELADA")));
        // No package updates should occur
        verify(repository, never()).savePackage(any());
        verify(repository, never()).saveReservation(any());
    }
}
