package com.agencytrip.agencytripapi.application.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import com.agencytrip.agencytripapi.application.dto.Requests;
import com.agencytrip.agencytripapi.domain.model.PackageTrip;
import com.agencytrip.agencytripapi.domain.model.Payment;
import com.agencytrip.agencytripapi.domain.model.Reservation;
import com.agencytrip.agencytripapi.domain.port.out.TravelRepositoryPort;

public class PaymentService {

    private final TravelRepositoryPort repo;

    public PaymentService(TravelRepositoryPort repo) {
        this.repo = repo;
    }

    public List<Payment> all() {
        return repo.findPayments();
    }

    public List<Payment> byUser(int userId) {
        return repo.findPaymentsByUser(userId);
    }

    @Transactional
    public PaymentResult create(
        int userId,
        Requests.Payment r
    ) {

        Reservation reservation =
            repo.findReservation(r.idReserva())
                .orElseThrow(() ->
                    CatalogService.notFound(
                        "La reserva no existe."
                    )
                );

        if (
            !reservation.getIdUsuario().equals(userId)
        ) {

            throw new BusinessException(
                HttpStatus.FORBIDDEN,
                "No puede realizar un pago para esta reserva."
            );
        }

        if (
            "CANCELADA".equalsIgnoreCase(
                reservation.getEstado()
            )
        ) {

            throw CatalogService.bad(
                "No se puede realizar el pago de una reserva cancelada."
            );
        }

        if (
            repo.hasApprovedPayment(
                r.idReserva()
            )
        ) {

            throw CatalogService.bad(
                "Esta reserva ya tiene un pago aprobado."
            );
        }

        String method =
            r.metodoPago().toUpperCase();

        if (
            !Set.of(
                "PSE",
                "TARJETA",
                "TRANSFERENCIA"
            ).contains(method)
        ) {

            throw CatalogService.bad(
                "Método de pago no válido."
            );
        }

        /*
         * Buscar nuevamente el paquete justo antes de confirmar
         * el pago.
         *
         * Esto es importante porque los cupos pueden haber cambiado
         * mientras la reserva estaba pendiente.
         */
        PackageTrip paquete =
            repo.findPackage(
                reservation.getIdPaquete()
            ).orElseThrow(() ->
                CatalogService.notFound(
                    "El paquete asociado a la reserva no existe."
                )
            );

        /*
         * Los cupos se validan y descuentan SOLAMENTE
         * en el momento de confirmar el pago.
         */
        if (
            reservation.getCantidadPersonas() >
            paquete.getCupos()
        ) {

            throw CatalogService.bad(
                "No hay suficientes cupos disponibles para confirmar esta reserva."
            );
        }

        /*
         * Descontar cupos al confirmar el pago.
         */
        paquete.setCupos(
            paquete.getCupos() -
            reservation.getCantidadPersonas()
        );

        repo.savePackage(paquete);

        /*
         * Registrar el pago como aprobado.
         */
        Payment p = new Payment();

        p.setIdReserva(
            reservation.getIdReserva()
        );

        p.setFechaPago(
            LocalDateTime.now()
        );

        p.setMonto(
            reservation.getPrecioTotal()
        );

        p.setMetodoPago(method);

        p.setEstado("APROBADO");

        /*
         * El pago aprobado confirma la reserva.
         */
        reservation.setEstado("CONFIRMADA");

        repo.saveReservation(reservation);

        return new PaymentResult(
            repo.savePayment(p),
            reservation
        );
    }

    @Transactional
    public PaymentResult updateStatus(
        int id,
        Requests.PaymentStatus r
    ) {

        Payment p =
            repo.findPayment(id)
                .orElseThrow(() ->
                    CatalogService.notFound(
                        "El pago no existe."
                    )
                );

        String status =
            r.estado().toUpperCase();

        if (
            !Set.of(
                "PENDIENTE",
                "APROBADO",
                "RECHAZADO"
            ).contains(status)
        ) {

            throw CatalogService.bad(
                "Estado no válido. Use PENDIENTE, APROBADO o RECHAZADO."
            );
        }

        String estadoAnterior =
            p.getEstado();

        /*
         * Si un pago pasa a APROBADO desde otro estado,
         * también debemos consumir los cupos.
         *
         * Esto cubre el caso administrativo de cambiar
         * manualmente el estado de un pago.
         */
        if (
            "APROBADO".equals(status) &&
            !"APROBADO".equalsIgnoreCase(
                estadoAnterior
            )
        ) {

            if (repo.hasApprovedPayment(p.getIdReserva())) {
                throw CatalogService.bad(
                    "Esta reserva ya tiene un pago aprobado."
                );
            }

            Reservation reservation =
                repo.findReservation(
                    p.getIdReserva()
                ).orElseThrow(() ->
                    CatalogService.notFound(
                        "La reserva asociada al pago no existe."
                    )
                );

            PackageTrip paquete =
                repo.findPackage(
                    reservation.getIdPaquete()
                ).orElseThrow(() ->
                    CatalogService.notFound(
                        "El paquete asociado a la reserva no existe."
                    )
                );

            if (
                reservation.getCantidadPersonas() >
                paquete.getCupos()
            ) {

                throw CatalogService.bad(
                    "No hay suficientes cupos disponibles para aprobar este pago."
                );
            }

            paquete.setCupos(
                paquete.getCupos() -
                reservation.getCantidadPersonas()
            );

            repo.savePackage(paquete);

            reservation.setEstado("CONFIRMADA");

            repo.saveReservation(reservation);
        }

        Reservation reservation =
            repo.findReservation(
                p.getIdReserva()
            ).orElse(null);

        /*
         * Si un pago que estaba APROBADO pasa a RECHAZADO o PENDIENTE,
         * y la reserva estaba CONFIRMADA, se devuelven los cupos
         * al paquete asociado.
         */
        if (
            "APROBADO".equalsIgnoreCase(estadoAnterior) &&
            !"APROBADO".equals(status)
        ) {

            if (
                reservation != null &&
                "CONFIRMADA".equalsIgnoreCase(reservation.getEstado())
            ) {

                PackageTrip paquete =
                    repo.findPackage(
                        reservation.getIdPaquete()
                    ).orElse(null);

                if (paquete != null) {
                    paquete.setCupos(
                        paquete.getCupos() +
                        reservation.getCantidadPersonas()
                    );
                    repo.savePackage(paquete);
                }
            }
        }

        /*
         * Si el pago es rechazado o queda pendiente,
         * la reserva vuelve a estado PENDIENTE.
         */
        if (reservation != null) {

            if ("RECHAZADO".equals(status)) {
                reservation.setEstado("PENDIENTE");
            }

            if ("PENDIENTE".equals(status)) {
                reservation.setEstado("PENDIENTE");
            }

            repo.saveReservation(reservation);
        }

        p.setEstado(status);

        return new PaymentResult(
            repo.savePayment(p),
            reservation
        );
    }

    public record PaymentResult(
        Payment payment,
        Reservation reservation
    ) {}
}