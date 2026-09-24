package com.agencytrip.agencytripapi.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal; import java.time.LocalDateTime;

/** Contratos HTTP compatibles con los nombres JSON de la API .NET. */
public final class Requests {
    private Requests() {}
    @Schema(description = "Datos requeridos para registrar un cliente.") public record Register(@Schema(example = "Ana") @NotBlank String nombre, @Schema(example = "Pérez") @NotBlank String apellido, @Schema(example = "ana@example.com") @NotBlank @Email String correo, @Schema(description = "Contraseña en texto plano; se almacena con BCrypt.", example = "ClaveSegura123") @NotBlank String password) {}
    @Schema(description = "Credenciales de acceso.") public record Login(@Schema(example = "ana@example.com") @NotBlank @Email String correo, @Schema(example = "ClaveSegura123") @NotBlank String password) {}
    @Schema(description = "Datos editables de un usuario.") public record UserUpdate(@NotBlank String nombre, @NotBlank String apellido, @NotBlank @Email String correo, @Schema(example = "2") @NotNull Integer idRol) {}
    @Schema(description = "Datos de un destino turístico.") public record Destination(@NotBlank String nombre, @NotBlank String pais, String descripcion, @Schema(description = "URL pública de imagen") String imagen) {}
    @Schema(description = "Datos de un hotel.") public record Hotel(@NotBlank String nombre, String direccion, String ciudad, @Schema(example = "4") Integer categoria, String descripcion) {}
    @Schema(description = "Datos de un transporte.") public record Transport(@Schema(example = "AEREO") @NotBlank String tipo, String empresa, String descripcion) {}
    @Schema(description = "Datos de un paquete turístico.") public record PackageTrip(@NotBlank String nombre, String descripcion, @Schema(example = "125000") @NotNull @Positive BigDecimal precio, @NotNull LocalDateTime fechaSalida, @NotNull LocalDateTime fechaRegreso, @Schema(example = "10") @Positive int cupos, @NotNull Integer idDestino, Integer idTransporte, Integer idHotel) {}
    @Schema(description = "Solicitud para crear una reserva.") public record Reservation(@NotNull Integer idPaquete, @Schema(example = "2") @Positive int cantidadPersonas) {}
    @Schema(description = "Cambio de estado de una reserva.") public record ReservationStatus(@Schema(example = "CANCELADA", allowableValues = {"PENDIENTE", "CONFIRMADA", "CANCELADA"}) @NotBlank String estado) {}
    @Schema(description = "Solicitud de pago de una reserva.") public record Payment(@NotNull Integer idReserva, @Schema(example = "PSE", allowableValues = {"PSE", "TARJETA", "TRANSFERENCIA"}) @NotBlank String metodoPago) {}
    @Schema(description = "Cambio de estado de un pago.") public record PaymentStatus(@Schema(example = "APROBADO", allowableValues = {"PENDIENTE", "APROBADO", "RECHAZADO"}) @NotBlank String estado) {}
}
