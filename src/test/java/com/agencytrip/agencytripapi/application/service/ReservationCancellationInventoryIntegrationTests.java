package com.agencytrip.agencytripapi.application.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.agencytrip.agencytripapi.application.dto.Requests;
import com.agencytrip.agencytripapi.domain.model.Destination;
import com.agencytrip.agencytripapi.domain.model.Hotel;
import com.agencytrip.agencytripapi.domain.model.PackageTrip;
import com.agencytrip.agencytripapi.domain.model.Reservation;
import com.agencytrip.agencytripapi.domain.model.Transport;
import com.agencytrip.agencytripapi.domain.model.User;
import com.agencytrip.agencytripapi.domain.port.out.TravelRepositoryPort;

/**
 * PRUEBA DE INTEGRACIÓN 2: CANCELACIÓN Y RESTITUCIÓN DE CUPOS
 *
 * Flujo probado:
 * 1. Estado inicial con cupos conocidos en un paquete.
 * 2. Creación de reserva confirmada (consumiendo cupos).
 * 3. Verificación de cupos restantes.
 * 4. Solicitud de cancelación de la reserva (estado CANCELADA).
 * 5. Verificación de que los cupos del paquete se restituyen exactamente por la cantidad cancelada.
 * 6. Verificación de idempotencia: un segundo intento de cancelación no restituye cupos dos veces ni genera inconsistencias.
 * 7. Verificación del estado final de la reserva como CANCELADA.
 * 8. Verificación de que no se permite reactivar una reserva cancelada sin pasar por el flujo correspondiente.
 * 9. Verificación de que cancelar una reserva PENDIENTE no altera cupos de forma errónea.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReservationCancellationInventoryIntegrationTests {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private TravelRepositoryPort travelRepository;

    private User testUser;
    private Destination testDestination;
    private Hotel testHotel;
    private Transport testTransport;

    @BeforeEach
    void setUp() {
        testUser = travelRepository.findActiveUsers().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Se requiere un usuario activo en test"));
        testDestination = travelRepository.findActiveDestinations().stream().findFirst()
                .orElseGet(() -> {
                    Destination d = new Destination();
                    d.setNombre("Destino Test");
                    d.setPais("Colombia");
                    d.setEstado(true);
                    return travelRepository.saveDestination(d);
                });
        testHotel = travelRepository.findActiveHotels().stream().findFirst()
                .orElseGet(() -> {
                    Hotel h = new Hotel();
                    h.setNombre("Hotel Test");
                    h.setEstado(true);
                    return travelRepository.saveHotel(h);
                });
        testTransport = travelRepository.findActiveTransports().stream().findFirst()
                .orElseGet(() -> {
                    Transport t = new Transport();
                    t.setTipo("AVIÓN");
                    t.setEstado(true);
                    return travelRepository.saveTransport(t);
                });
    }

    private PackageTrip createTestPackage(int cupos) {
        PackageTrip pkg = new PackageTrip();
        pkg.setNombre("CANCEL_INV_" + UUID.randomUUID().toString().substring(0, 8));
        pkg.setDescripcion("Paquete de prueba para restitución de cupos en cancelación");
        pkg.setPrecio(new BigDecimal("180000.00"));
        pkg.setFechaSalida(LocalDateTime.now().plusDays(20));
        pkg.setFechaRegreso(LocalDateTime.now().plusDays(25));
        pkg.setCupos(cupos);
        pkg.setIdDestino(testDestination.getIdDestino());
        pkg.setIdHotel(testHotel.getIdHotel());
        pkg.setIdTransporte(testTransport.getIdTransporte());
        pkg.setEstado(true);

        return travelRepository.savePackage(pkg);
    }

    @Test
    @DisplayName("Restitución exacta de cupos al cancelar reserva confirmada, validando idempotencia y bloqueo de reactivación")
    void testConfirmedReservationCancellationRestoresCapacityWithIdempotence() {
        // 1. Estado inicial con cupos conocidos (12 cupos)
        final int initialCupos = 12;
        final int bookedPersons = 4;
        PackageTrip pkg = createTestPackage(initialCupos);
        final int packageId = pkg.getIdPaquete();

        // Crear reserva en PENDIENTE
        Reservation pending = reservationService.create(
                testUser.getIdUsuario(),
                new Requests.Reservation(packageId, bookedPersons)
        );
        assertNotNull(pending.getIdReserva());
        final int reservationId = pending.getIdReserva();

        // 2. Confirmación de reserva (consumiendo 4 cupos)
        Reservation confirmed = reservationService.changeStatus(
                reservationId,
                new Requests.ReservationStatus("CONFIRMADA")
        );
        assertEquals("CONFIRMADA", confirmed.getEstado());

        // 3. Verificación de cupos restantes en base de datos (12 - 4 = 8)
        PackageTrip pkgAfterConfirm = travelRepository.findPackage(packageId).orElseThrow();
        assertEquals(8, pkgAfterConfirm.getCupos(), "Tras confirmar la reserva, los cupos deben ser exactamente 8");

        // 4. Solicitud de cancelación de la reserva
        Reservation cancelled = reservationService.changeStatus(
                reservationId,
                new Requests.ReservationStatus("CANCELADA")
        );

        // 5. Verificación de estado CANCELADA y restitución exacta de cupos (8 + 4 = 12)
        assertEquals("CANCELADA", cancelled.getEstado(), "El estado de la reserva debe ser CANCELADA");

        PackageTrip pkgAfterCancel = travelRepository.findPackage(packageId).orElseThrow();
        assertEquals(initialCupos, pkgAfterCancel.getCupos(),
                "Al cancelar la reserva confirmada, los cupos deben restituirse exactamente a su valor inicial (12)");

        // 6. Verificación de idempotencia: un segundo intento de cancelación no debe restituir cupos dos veces
        assertThrows(BusinessException.class, () ->
                reservationService.changeStatus(reservationId, new Requests.ReservationStatus("CANCELADA")),
                "Un segundo intento de cancelación debe ser rechazado como operación inválida"
        );

        PackageTrip pkgAfterDuplicateCancel = travelRepository.findPackage(packageId).orElseThrow();
        assertEquals(initialCupos, pkgAfterDuplicateCancel.getCupos(),
                "La idempotencia debe garantizar que los cupos no se restituyan dos veces (siguen siendo 12)");

        // 7. Verificación del estado final en base de datos
        Reservation dbRes = travelRepository.findReservation(reservationId).orElseThrow();
        assertEquals("CANCELADA", dbRes.getEstado());

        // 8. Verificación de que no se permite reactivar una reserva cancelada
        assertThrows(BusinessException.class, () ->
                reservationService.changeStatus(reservationId, new Requests.ReservationStatus("CONFIRMADA")),
                "No debe permitirse reactivar una reserva cancelada a CONFIRMADA"
        );

        assertThrows(BusinessException.class, () ->
                reservationService.changeStatus(reservationId, new Requests.ReservationStatus("PENDIENTE")),
                "No debe permitirse reactivar una reserva cancelada a PENDIENTE"
        );

        // Los cupos siguen inmutables
        PackageTrip pkgFinal = travelRepository.findPackage(packageId).orElseThrow();
        assertEquals(initialCupos, pkgFinal.getCupos(), "Los cupos deben mantenerse en 12");
    }

    @Test
    @DisplayName("Cancelar una reserva PENDIENTE no debe alterar los cupos del paquete turístico")
    void testPendingReservationCancellationDoesNotAlterCapacity() {
        final int initialCupos = 15;
        final int bookedPersons = 3;
        PackageTrip pkg = createTestPackage(initialCupos);
        final int packageId = pkg.getIdPaquete();

        // 1. Crear reserva PENDIENTE
        Reservation pending = reservationService.create(
                testUser.getIdUsuario(),
                new Requests.Reservation(packageId, bookedPersons)
        );
        final int resId = pending.getIdReserva();

        // Los cupos no se habían descontado
        PackageTrip pkgWhilePending = travelRepository.findPackage(packageId).orElseThrow();
        assertEquals(initialCupos, pkgWhilePending.getCupos());

        // 2. Cancelar la reserva PENDIENTE
        Reservation cancelled = reservationService.changeStatus(
                resId,
                new Requests.ReservationStatus("CANCELADA")
        );
        assertEquals("CANCELADA", cancelled.getEstado());

        // 3. Los cupos deben permanecer exactamente iguales (15), sin incremento indebido
        PackageTrip pkgAfterPendingCancel = travelRepository.findPackage(packageId).orElseThrow();
        assertEquals(initialCupos, pkgAfterPendingCancel.getCupos(),
                "Cancelar una reserva PENDIENTE no debe sumar cupos artificialmente");
    }
}
