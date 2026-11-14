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
import com.agencytrip.agencytripapi.application.service.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Pagos", description = "Registro y consulta de pagos asociados a reservas turísticas.")
@RestController
@RequestMapping("/api/Pagos")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) { this.service = service; }

    @Operation(summary = "Consultar pagos", description = "Un cliente obtiene únicamente sus pagos; los demás roles pueden consultar todos los pagos.", security = @SecurityRequirement(name = "bearerAuth"))
    @io.swagger.v3.oas.annotations.responses.ApiResponses({ @ApiResponse(responseCode = "200", description = "Pagos obtenidos correctamente"), @ApiResponse(responseCode = "401", description = "Se requiere un token JWT válido") })
    @GetMapping
    public Object list(@AuthenticationPrincipal Jwt jwt) {
        return (ReservationController.role(jwt).equals("CLIENTE") ? service.byUser(ReservationController.id(jwt)) : service.all()).stream().map(ApiResponses::payment).toList();
    }

    @Operation(summary = "Registrar pago", description = "Registra el pago de una reserva propia, evita pagos aprobados duplicados y actualiza el estado de la reserva.", security = @SecurityRequirement(name = "bearerAuth"))
    @io.swagger.v3.oas.annotations.responses.ApiResponses({ @ApiResponse(responseCode = "200", description = "Pago registrado correctamente"), @ApiResponse(responseCode = "400", description = "Los datos no son válidos, el método de pago no es permitido o la reserva ya tiene un pago aprobado"), @ApiResponse(responseCode = "401", description = "Se requiere un token JWT válido"), @ApiResponse(responseCode = "403", description = "La reserva no pertenece al usuario autenticado"), @ApiResponse(responseCode = "404", description = "La reserva no existe") })
    @PostMapping
    public Object create(@AuthenticationPrincipal Jwt jwt, @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Información del pago, incluida la reserva y el método de pago.", required = true) @Valid @RequestBody Requests.Payment request) {
        var result = service.create(ReservationController.id(jwt), request);
        return Map.of("mensaje", "Pago realizado correctamente.", "pago", ApiResponses.payment(result.payment()), "reserva", ApiResponses.reservation(result.reservation()));
    }

    @Operation(summary = "Cambiar estado de pago", description = "Actualiza el estado de un pago y sincroniza el estado de su reserva asociada. Requiere el rol ADMINISTRADOR.", security = @SecurityRequirement(name = "bearerAuth"))
    @io.swagger.v3.oas.annotations.responses.ApiResponses({ @ApiResponse(responseCode = "200", description = "Estado del pago actualizado correctamente"), @ApiResponse(responseCode = "400", description = "El estado indicado no es válido"), @ApiResponse(responseCode = "401", description = "Se requiere un token JWT válido"), @ApiResponse(responseCode = "403", description = "El usuario no cuenta con el rol ADMINISTRADOR"), @ApiResponse(responseCode = "404", description = "El pago no existe") })
    @PutMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Object update(@Parameter(description = "Identificador del pago cuyo estado se actualizará.", required = true) @PathVariable int id, @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Nuevo estado del pago.", required = true) @Valid @RequestBody Requests.PaymentStatus request) {
        var result = service.updateStatus(id, request);
        return Map.of("mensaje", "Estado del pago actualizado correctamente.", "pago", ApiResponses.payment(result.payment()), "reserva", ApiResponses.reservation(result.reservation()));
    }
}
