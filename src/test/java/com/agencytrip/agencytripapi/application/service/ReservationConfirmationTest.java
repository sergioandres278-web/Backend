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
class ReservationConfirmationTest {

    @Mock
    private TravelRepositoryPort repository;

    @InjectMocks
    private ReservationService reservationService;

    @Test
    void confirmsPendingReservationAndUpdatesCapacity() {
        // Given – pending reservation
        int reservationId = 5;
        int userId = 2;
        int packageId = 12;
        int persons = 4;
        Reservation reservation = new Reservation();
        reservation.setIdReserva(reservationId);
        reservation.setIdUsuario(userId);
        reservation.setIdPaquete(packageId);
        reservation.setCantidadPersonas(persons);
        reservation.setEstado("PENDIENTE");

        // Package with enough capacity
        PackageTrip packageTrip = new PackageTrip();
        packageTrip.setIdPaquete(packageId);
        packageTrip.setCupos(10);
        packageTrip.setEstado(true);
        packageTrip.setPrecio(java.math.BigDecimal.ONE);

        // Mock repository behavior
        when(repository.findReservationForUpdate(reservationId)).thenReturn(Optional.of(reservation));
        when(repository.findPackageForUpdate(packageId)).thenReturn(Optional.of(packageTrip));
        when(repository.savePackage(any(PackageTrip.class))).thenAnswer(i -> i.getArgument(0));
        when(repository.saveReservation(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

        // When – confirm reservation
        Reservation result = reservationService.changeStatus(reservationId, new Requests.ReservationStatus("CONFIRMADA"));

        // Then – reservation status updated
        assertEquals("CONFIRMADA", result.getEstado());
        // Capacity must be reduced by the number of persons
        ArgumentCaptor<PackageTrip> captor = ArgumentCaptor.forClass(PackageTrip.class);
        verify(repository).savePackage(captor.capture());
        PackageTrip savedPackage = captor.getValue();
        assertEquals(10 - persons, savedPackage.getCupos());
        // Reservation should be saved with the new state
        verify(repository).saveReservation(any(Reservation.class));
    }
}
