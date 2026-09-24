package com.agencytrip.agencytripapi.infrastructure.adapter.in.web;

import com.agencytrip.agencytripapi.domain.model.Payment;
import com.agencytrip.agencytripapi.domain.model.Reservation;
import com.agencytrip.agencytripapi.domain.model.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** HTTP response shapes. Domain objects are never serialized directly when they contain credentials or aggregates. */
final class ApiResponses {
    private ApiResponses() {}

    static UserAuth userAuth(User user, String role) {
        return new UserAuth(user.getIdUsuario(), user.getNombre(), user.getApellido(), user.getCorreo(), role);
    }

    static UserAdmin userAdmin(User user) {
        return new UserAdmin(user.getIdUsuario(), user.getNombre(), user.getApellido(), user.getCorreo(),
                user.getIdRol(), user.getRol() == null ? null : user.getRol().getNombre(), user.getFechaRegistro());
    }

    static ReservationView reservation(Reservation reservation) {
        String client = reservation.getUsuario() == null ? null
                : reservation.getUsuario().getNombre() + " " + reservation.getUsuario().getApellido();
        String packageName = reservation.getPaquete() == null ? null : reservation.getPaquete().getNombre();
        String destination = reservation.getPaquete() == null || reservation.getPaquete().getDestino() == null ? null
                : reservation.getPaquete().getDestino().getNombre();
        return new ReservationView(reservation.getIdReserva(), reservation.getFechaReserva(),
                reservation.getCantidadPersonas(), reservation.getPrecioTotal(), reservation.getEstado(),
                client, packageName, destination);
    }

    static PaymentView payment(Payment payment) {
        return new PaymentView(payment.getIdPago(), payment.getIdReserva(), payment.getFechaPago(), payment.getMonto(),
                payment.getMetodoPago(), payment.getEstado());
    }

    record UserAuth(Integer idUsuario, String nombre, String apellido, String correo, String rol) {}
    record UserAdmin(Integer idUsuario, String nombre, String apellido, String correo, Integer idRol, String rol,
                     LocalDateTime fechaRegistro) {}
    record ReservationView(Integer idReserva, LocalDateTime fechaReserva, int cantidadPersonas, BigDecimal precioTotal,
                           String estado, String cliente, String paquete, String destino) {}
    record PaymentView(Integer idPago, Integer idReserva, LocalDateTime fechaPago, BigDecimal monto,
                       String metodoPago, String estado) {}
}
