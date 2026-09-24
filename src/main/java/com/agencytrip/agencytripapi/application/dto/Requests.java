package com.agencytrip.agencytripapi.application.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal; import java.time.LocalDateTime;

/** Contratos HTTP compatibles con los nombres JSON de la API .NET. */
public final class Requests {
    private Requests() {}
    public record Register(@NotBlank String nombre, @NotBlank String apellido, @NotBlank @Email String correo, @NotBlank String password) {}
    public record Login(@NotBlank @Email String correo, @NotBlank String password) {}
    public record UserUpdate(@NotBlank String nombre, @NotBlank String apellido, @NotBlank @Email String correo, @NotNull Integer idRol) {}
    public record Destination(@NotBlank String nombre, @NotBlank String pais, String descripcion, String imagen) {}
    public record Hotel(@NotBlank String nombre, String direccion, String ciudad, Integer categoria, String descripcion) {}
    public record Transport(@NotBlank String tipo, String empresa, String descripcion) {}
    public record PackageTrip(@NotBlank String nombre, String descripcion, @NotNull @Positive BigDecimal precio, @NotNull LocalDateTime fechaSalida, @NotNull LocalDateTime fechaRegreso, @Positive int cupos, @NotNull Integer idDestino, Integer idTransporte, Integer idHotel) {}
    public record Reservation(@NotNull Integer idPaquete, @Positive int cantidadPersonas) {}
    public record ReservationStatus(@NotBlank String estado) {}
    public record Payment(@NotNull Integer idReserva, @NotBlank String metodoPago) {}
    public record PaymentStatus(@NotBlank String estado) {}
}
