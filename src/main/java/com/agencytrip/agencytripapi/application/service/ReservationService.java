package com.agencytrip.agencytripapi.application.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

import com.agencytrip.agencytripapi.application.dto.Requests;
import com.agencytrip.agencytripapi.domain.model.PackageTrip;
import com.agencytrip.agencytripapi.domain.model.Reservation;
import com.agencytrip.agencytripapi.domain.port.out.TravelRepositoryPort;

/**
 * Caso de uso para la gestión de reservas.
 *
 * Las reservas se crean en estado PENDIENTE.
 * Los cupos solamente se consumen cuando el pago es confirmado.
 */
public class ReservationService {

    private final TravelRepositoryPort repo;

    public ReservationService(TravelRepositoryPort repo) {
        this.repo = repo;
    }

    public List<Reservation> all() {
        return repo.findReservations();
    }

    public List<Reservation> byUser(int id) {
        return repo.findReservationsByUser(id);
    }

    @Transactional
    public Reservation create(
        int userId,
        Requests.Reservation r
    ) {

        PackageTrip p = repo.findPackage(r.idPaquete())
            .filter(PackageTrip::isEstado)
            .orElseThrow(() ->
                CatalogService.notFound(
                    "El paquete no existe o no está disponible."
                )
            );

        /*
         * Se mantiene esta validación para evitar crear una reserva
         * con más personas que los cupos disponibles.
         *
         * IMPORTANTE:
         * Aquí NO se descuentan los cupos.
         */
        if (r.cantidadPersonas() > p.getCupos()) {
            throw CatalogService.bad(
                "No hay suficientes cupos disponibles."
            );
        }

        Reservation reservation = new Reservation();

        reservation.setIdUsuario(userId);
        reservation.setIdPaquete(p.getIdPaquete());
        reservation.setFechaReserva(LocalDateTime.now());
        reservation.setCantidadPersonas(r.cantidadPersonas());

        reservation.setPrecioTotal(
            p.getPrecio().multiply(
                BigDecimal.valueOf(
                    r.cantidadPersonas()
                )
            )
        );

        reservation.setEstado("PENDIENTE");

        /*
         * No se modifica p.getCupos() aquí.
         *
         * Los cupos solamente se descuentan cuando
         * el usuario confirma el pago.
         */
        return repo.saveReservation(reservation);
    }

    @Transactional
    public Reservation changeStatus(
        int id,
        Requests.ReservationStatus r
    ) {

        Reservation reservation =
            repo.findReservationForUpdate(id)
                .or(() -> repo.findReservation(id))
                .orElseThrow(() ->
                    CatalogService.notFound(
                        "La reserva no existe."
                    )
                );

        String status =
            r.estado().toUpperCase();

        if (!Set.of(
            "PENDIENTE",
            "CONFIRMADA",
            "CANCELADA"
        ).contains(status)) {

            throw CatalogService.bad(
                "Estado no válido."
            );
        }

        if (
            "CANCELADA".equals(status) &&
            "CANCELADA".equals(reservation.getEstado())
        ) {

            throw CatalogService.bad(
                "La reserva ya está cancelada."
            );
        }

        if (
            "CONFIRMADA".equals(status) &&
            "CONFIRMADA".equals(reservation.getEstado())
        ) {

            throw CatalogService.bad(
                "La reserva ya está confirmada."
            );
        }

        if (
            "CANCELADA".equals(reservation.getEstado()) &&
            !"CANCELADA".equals(status)
        ) {

            throw CatalogService.bad(
                "No se puede reactivar una reserva cancelada."
            );
        }

        /*
         * Si una reserva pasa a CONFIRMADA desde PENDIENTE,
         * se validan y descuentan los cupos del paquete.
         */
        if (
            "CONFIRMADA".equals(status) &&
            "PENDIENTE".equals(reservation.getEstado())
        ) {

            PackageTrip p =
                repo.findPackageForUpdate(
                    reservation.getIdPaquete()
                ).or(() -> repo.findPackage(reservation.getIdPaquete()))
                .orElseThrow(() ->
                    CatalogService.bad(
                        "No se pudo obtener el paquete asociado."
                    )
                );

            if (!p.isEstado()) {
                throw CatalogService.bad(
                    "El paquete asociado está desactivado."
                );
            }

            if (reservation.getCantidadPersonas() > p.getCupos()) {
                throw CatalogService.bad(
                    "No hay suficientes cupos disponibles para confirmar esta reserva."
                );
            }

            p.setCupos(
                p.getCupos() -
                reservation.getCantidadPersonas()
            );

            repo.savePackage(p);
        }

        /*
         * Los cupos se devuelven cuando una reserva que estaba
         * CONFIRMADA pasa a CANCELADA o vuelve a PENDIENTE.
         */
        if (
            "CONFIRMADA".equals(reservation.getEstado()) &&
            ("CANCELADA".equals(status) || "PENDIENTE".equals(status))
        ) {

            PackageTrip p =
                repo.findPackageForUpdate(
                    reservation.getIdPaquete()
                ).or(() -> repo.findPackage(reservation.getIdPaquete()))
                .orElseThrow(() ->
                    CatalogService.bad(
                        "No se pudo obtener el paquete asociado."
                    )
                );

            p.setCupos(
                p.getCupos() +
                reservation.getCantidadPersonas()
            );

            repo.savePackage(p);
        }

        reservation.setEstado(status);

        return repo.saveReservation(reservation);
    }
}