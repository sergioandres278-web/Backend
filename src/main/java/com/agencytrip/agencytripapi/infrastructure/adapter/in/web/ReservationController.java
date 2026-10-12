package com.agencytrip.agencytripapi.infrastructure.adapter.in.web;

import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.agencytrip.agencytripapi.application.dto.Requests;
import com.agencytrip.agencytripapi.application.service.ReservationService;
import com.agencytrip.agencytripapi.domain.model.Reservation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Reservas", description = "Gestión de reservas de paquetes turísticos.")
@RestController
@RequestMapping("/api/Reservas")
public class ReservationController {

    private final ReservationService service;

    public ReservationController(ReservationService service) { this.service = service; }

    @Operation(summary = "Consultar reservas", description = "Un cliente obtiene únicamente sus reservas; los demás roles pueden consultar todas las reservas.", security = @SecurityRequirement(name = "bearerAuth"))
    @io.swagger.v3.oas.annotations.responses.ApiResponses({ @ApiResponse(responseCode = "200", description = "Reservas obtenidas correctamente"), @ApiResponse(responseCode = "401", description = "Se requiere un token JWT válido") })
    @GetMapping
    public Object list(@AuthenticationPrincipal Jwt jwt) {
        return (role(jwt).equals("CLIENTE") ? service.byUser(id(jwt)) : service.all()).stream().map(ApiResponses::reservation).toList();
    }

    @Operation(summary = "Crear reserva", description = "Crea una reserva en estado PENDIENTE verificando la disponibilidad de cupos y calculando el precio total. Los cupos se descuentan al confirmar el pago.", security = @SecurityRequirement(name = "bearerAuth"))
    @io.swagger.v3.oas.annotations.responses.ApiResponses({ @ApiResponse(responseCode = "200", description = "Reserva creada correctamente"), @ApiResponse(responseCode = "400", description = "Los datos no son válidos o no hay cupos suficientes"), @ApiResponse(responseCode = "401", description = "Se requiere un token JWT válido"), @ApiResponse(responseCode = "404", description = "El paquete no existe o no está disponible") })
    @PostMapping
    public Object create(@AuthenticationPrincipal Jwt jwt, @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Datos de la reserva, incluido el paquete y la cantidad de personas.", required = true) @Valid @RequestBody Requests.Reservation request) {
        Reservation reservation = service.create(id(jwt), request);
        return Map.of("mensaje", "Reserva creada correctamente.", "reserva", ApiResponses.reservation(reservation));
    }

    @Operation(summary = "Cambiar estado de reserva", description = "Actualiza el estado de una reserva. Al cancelarla, devuelve sus cupos al paquete asociado. Requiere el rol ADMINISTRADOR.", security = @SecurityRequirement(name = "bearerAuth"))
    @io.swagger.v3.oas.annotations.responses.ApiResponses({ @ApiResponse(responseCode = "200", description = "Estado de la reserva actualizado correctamente"), @ApiResponse(responseCode = "400", description = "El estado indicado no es válido o la reserva ya está cancelada"), @ApiResponse(responseCode = "401", description = "Se requiere un token JWT válido"), @ApiResponse(responseCode = "403", description = "El usuario no cuenta con el rol ADMINISTRADOR"), @ApiResponse(responseCode = "404", description = "La reserva no existe") })
    @PutMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Object status(@Parameter(description = "Identificador de la reserva cuyo estado se actualizará.", required = true) @PathVariable int id, @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Nuevo estado de la reserva.", required = true) @Valid @RequestBody Requests.ReservationStatus request) {
        service.changeStatus(id, request);
        return Map.of("mensaje", "Estado de la reserva actualizado correctamente.");
    }

    static int id(Jwt jwt) { return ((Number) jwt.getClaim("userId")).intValue(); }
    static String role(Jwt jwt) { return jwt.getClaimAsStringList("roles").getFirst(); }
}
