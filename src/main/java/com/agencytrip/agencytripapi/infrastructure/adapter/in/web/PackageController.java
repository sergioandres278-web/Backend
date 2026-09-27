package com.agencytrip.agencytripapi.infrastructure.adapter.in.web;

import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.agencytrip.agencytripapi.application.dto.Requests;
import com.agencytrip.agencytripapi.application.service.PackageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Paquetes", description = "Gestión de paquetes turísticos y sus servicios asociados.")
@RestController
@RequestMapping("/api/Paquetes")
public class PackageController {

    private final PackageService service;

    public PackageController(PackageService service) { this.service = service; }

    @Operation(summary = "Consultar paquetes turísticos", description = "Obtiene el listado de paquetes turísticos activos disponibles.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Listado de paquetes obtenido correctamente"))
    @GetMapping
    public Object list() { return service.list(); }

    @Operation(summary = "Crear paquete turístico", description = "Registra un paquete con destino y, cuando corresponda, hotel y transporte asociados. Requiere el rol ADMINISTRADOR.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Paquete creado correctamente"), @ApiResponse(responseCode = "400", description = "Los datos son inválidos o un servicio asociado no está disponible"), @ApiResponse(responseCode = "401", description = "Se requiere un token JWT válido"), @ApiResponse(responseCode = "403", description = "El usuario no cuenta con el rol ADMINISTRADOR") })
    @PostMapping @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Object create(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Información del paquete turístico a registrar.", required = true) @Valid @RequestBody Requests.PackageTrip request) { return Map.of("mensaje", "Paquete creado correctamente.", "paquete", service.create(request)); }

    @Operation(summary = "Actualizar paquete turístico", description = "Actualiza un paquete activo y sus referencias a servicios turísticos. Requiere el rol ADMINISTRADOR.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Paquete actualizado correctamente"), @ApiResponse(responseCode = "400", description = "Los datos son inválidos, el paquete está desactivado o un servicio asociado no está disponible"), @ApiResponse(responseCode = "401", description = "Se requiere un token JWT válido"), @ApiResponse(responseCode = "403", description = "El usuario no cuenta con el rol ADMINISTRADOR"), @ApiResponse(responseCode = "404", description = "El paquete no existe") })
    @PutMapping("/{id}") @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Object update(@Parameter(description = "Identificador del paquete a actualizar.", required = true) @PathVariable int id, @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Información actualizada del paquete turístico.", required = true) @Valid @RequestBody Requests.PackageTrip request) { return Map.of("mensaje", "Paquete actualizado correctamente.", "paquete", service.update(id, request)); }

    @Operation(summary = "Desactivar paquete turístico", description = "Cambia el estado de un paquete a inactivo. Requiere el rol ADMINISTRADOR.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Paquete desactivado correctamente"), @ApiResponse(responseCode = "400", description = "El paquete ya está desactivado"), @ApiResponse(responseCode = "401", description = "Se requiere un token JWT válido"), @ApiResponse(responseCode = "403", description = "El usuario no cuenta con el rol ADMINISTRADOR"), @ApiResponse(responseCode = "404", description = "El paquete no existe") })
    @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Object disable(@Parameter(description = "Identificador del paquete a desactivar.", required = true) @PathVariable int id) { service.disable(id); return Map.of("mensaje", "Paquete desactivado correctamente."); }
}
