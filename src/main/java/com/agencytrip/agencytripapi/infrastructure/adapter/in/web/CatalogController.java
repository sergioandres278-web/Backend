package com.agencytrip.agencytripapi.infrastructure.adapter.in.web;

import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.agencytrip.agencytripapi.application.dto.Requests;
import com.agencytrip.agencytripapi.application.service.CatalogService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
public class CatalogController {

    private final CatalogService service;

    public CatalogController(CatalogService service) {
        this.service = service;
    }

    @Tag(name = "Destinos", description = "Catálogo de destinos turísticos disponibles.")
    @Operation(summary = "Consultar destinos turísticos", description = "Obtiene el listado de destinos turísticos activos registrados en el sistema.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Listado de destinos obtenido correctamente"))
    @GetMapping("/api/Destinos")
    public Object destinations() { return service.destinations(); }

    @Tag(name = "Destinos", description = "Catálogo de destinos turísticos disponibles.")
    @Operation(summary = "Crear destino turístico", description = "Registra un destino turístico activo. Requiere el rol ADMINISTRADOR.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Destino creado correctamente"), @ApiResponse(responseCode = "400", description = "Los datos del destino no cumplen las validaciones"), @ApiResponse(responseCode = "401", description = "Se requiere un token JWT válido"), @ApiResponse(responseCode = "403", description = "El usuario no cuenta con el rol ADMINISTRADOR") })
    @PostMapping("/api/Destinos") @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Object createDestination(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Datos del destino turístico a registrar.", required = true) @Valid @RequestBody Requests.Destination request) { return Map.of("mensaje", "Destino creado correctamente.", "destino", service.createDestination(request)); }

    @Tag(name = "Destinos", description = "Catálogo de destinos turísticos disponibles.")
    @Operation(summary = "Actualizar destino turístico", description = "Actualiza la información de un destino existente. Requiere el rol ADMINISTRADOR.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Destino actualizado correctamente"), @ApiResponse(responseCode = "400", description = "Los datos del destino no cumplen las validaciones"), @ApiResponse(responseCode = "401", description = "Se requiere un token JWT válido"), @ApiResponse(responseCode = "403", description = "El usuario no cuenta con el rol ADMINISTRADOR"), @ApiResponse(responseCode = "404", description = "El destino no existe") })
    @PutMapping("/api/Destinos/{id}") @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Object updateDestination(@Parameter(description = "Identificador del destino a actualizar.", required = true) @PathVariable int id, @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Datos actualizados del destino turístico.", required = true) @Valid @RequestBody Requests.Destination request) { return Map.of("mensaje", "Destino actualizado correctamente.", "destino", service.updateDestination(id, request)); }

    @Tag(name = "Destinos", description = "Catálogo de destinos turísticos disponibles.")
    @Operation(summary = "Desactivar destino turístico", description = "Cambia el estado de un destino a inactivo. Requiere el rol ADMINISTRADOR.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Destino desactivado correctamente"), @ApiResponse(responseCode = "401", description = "Se requiere un token JWT válido"), @ApiResponse(responseCode = "403", description = "El usuario no cuenta con el rol ADMINISTRADOR"), @ApiResponse(responseCode = "404", description = "El destino no existe") })
    @DeleteMapping("/api/Destinos/{id}") @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Object disableDestination(@Parameter(description = "Identificador del destino a desactivar.", required = true) @PathVariable int id) { service.disableDestination(id); return Map.of("mensaje", "Destino desactivado correctamente."); }

    @Tag(name = "Hoteles", description = "Catálogo de hoteles disponibles para los paquetes turísticos.")
    @Operation(summary = "Consultar hoteles", description = "Obtiene el listado de hoteles activos registrados en el sistema.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Listado de hoteles obtenido correctamente"))
    @GetMapping("/api/Hoteles") public Object hotels() { return service.hotels(); }

    @Tag(name = "Hoteles", description = "Catálogo de hoteles disponibles para los paquetes turísticos.")
    @Operation(summary = "Crear hotel", description = "Registra un hotel activo para su uso en paquetes turísticos. Requiere el rol ADMINISTRADOR.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Hotel creado correctamente"), @ApiResponse(responseCode = "400", description = "Los datos del hotel no cumplen las validaciones"), @ApiResponse(responseCode = "401", description = "Se requiere un token JWT válido"), @ApiResponse(responseCode = "403", description = "El usuario no cuenta con el rol ADMINISTRADOR") })
    @PostMapping("/api/Hoteles") @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Object createHotel(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Datos del hotel a registrar.", required = true) @Valid @RequestBody Requests.Hotel request) { return Map.of("mensaje", "Hotel creado correctamente.", "hotel", service.createHotel(request)); }

    @Tag(name = "Hoteles", description = "Catálogo de hoteles disponibles para los paquetes turísticos.")
    @Operation(summary = "Actualizar hotel", description = "Actualiza la información de un hotel activo. Requiere el rol ADMINISTRADOR.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Hotel actualizado correctamente"), @ApiResponse(responseCode = "400", description = "Los datos del hotel no cumplen las validaciones o el hotel está desactivado"), @ApiResponse(responseCode = "401", description = "Se requiere un token JWT válido"), @ApiResponse(responseCode = "403", description = "El usuario no cuenta con el rol ADMINISTRADOR"), @ApiResponse(responseCode = "404", description = "El hotel no existe") })
    @PutMapping("/api/Hoteles/{id}") @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Object updateHotel(@Parameter(description = "Identificador del hotel a actualizar.", required = true) @PathVariable int id, @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Datos actualizados del hotel.", required = true) @Valid @RequestBody Requests.Hotel request) { return Map.of("mensaje", "Hotel actualizado correctamente.", "hotel", service.updateHotel(id, request)); }

    @Tag(name = "Hoteles", description = "Catálogo de hoteles disponibles para los paquetes turísticos.")
    @Operation(summary = "Desactivar hotel", description = "Cambia el estado de un hotel a inactivo. Requiere el rol ADMINISTRADOR.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Hotel desactivado correctamente"), @ApiResponse(responseCode = "400", description = "El hotel ya está desactivado"), @ApiResponse(responseCode = "401", description = "Se requiere un token JWT válido"), @ApiResponse(responseCode = "403", description = "El usuario no cuenta con el rol ADMINISTRADOR"), @ApiResponse(responseCode = "404", description = "El hotel no existe") })
    @DeleteMapping("/api/Hoteles/{id}") @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Object disableHotel(@Parameter(description = "Identificador del hotel a desactivar.", required = true) @PathVariable int id) { service.disableHotel(id); return Map.of("mensaje", "Hotel desactivado correctamente."); }

    @Tag(name = "Transportes", description = "Catálogo de medios de transporte disponibles para los paquetes turísticos.")
    @Operation(summary = "Consultar transportes", description = "Obtiene el listado de transportes activos registrados en el sistema.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Listado de transportes obtenido correctamente"))
    @GetMapping("/api/Transportes") public Object transports() { return service.transports(); }

    @Tag(name = "Transportes", description = "Catálogo de medios de transporte disponibles para los paquetes turísticos.")
    @Operation(summary = "Crear transporte", description = "Registra un transporte activo para su uso en paquetes turísticos. Requiere el rol ADMINISTRADOR.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Transporte creado correctamente"), @ApiResponse(responseCode = "400", description = "Los datos del transporte no cumplen las validaciones"), @ApiResponse(responseCode = "401", description = "Se requiere un token JWT válido"), @ApiResponse(responseCode = "403", description = "El usuario no cuenta con el rol ADMINISTRADOR") })
    @PostMapping("/api/Transportes") @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Object createTransport(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Datos del transporte a registrar.", required = true) @Valid @RequestBody Requests.Transport request) { return Map.of("mensaje", "Transporte creado correctamente.", "transporte", service.createTransport(request)); }

    @Tag(name = "Transportes", description = "Catálogo de medios de transporte disponibles para los paquetes turísticos.")
    @Operation(summary = "Actualizar transporte", description = "Actualiza la información de un transporte activo. Requiere el rol ADMINISTRADOR.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Transporte actualizado correctamente"), @ApiResponse(responseCode = "400", description = "Los datos del transporte no cumplen las validaciones o el transporte está desactivado"), @ApiResponse(responseCode = "401", description = "Se requiere un token JWT válido"), @ApiResponse(responseCode = "403", description = "El usuario no cuenta con el rol ADMINISTRADOR"), @ApiResponse(responseCode = "404", description = "El transporte no existe") })
    @PutMapping("/api/Transportes/{id}") @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Object updateTransport(@Parameter(description = "Identificador del transporte a actualizar.", required = true) @PathVariable int id, @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Datos actualizados del transporte.", required = true) @Valid @RequestBody Requests.Transport request) { return Map.of("mensaje", "Transporte actualizado correctamente.", "transporte", service.updateTransport(id, request)); }

    @Tag(name = "Transportes", description = "Catálogo de medios de transporte disponibles para los paquetes turísticos.")
    @Operation(summary = "Desactivar transporte", description = "Cambia el estado de un transporte a inactivo. Requiere el rol ADMINISTRADOR.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Transporte desactivado correctamente"), @ApiResponse(responseCode = "400", description = "El transporte ya está desactivado"), @ApiResponse(responseCode = "401", description = "Se requiere un token JWT válido"), @ApiResponse(responseCode = "403", description = "El usuario no cuenta con el rol ADMINISTRADOR"), @ApiResponse(responseCode = "404", description = "El transporte no existe") })
    @DeleteMapping("/api/Transportes/{id}") @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Object disableTransport(@Parameter(description = "Identificador del transporte a desactivar.", required = true) @PathVariable int id) { service.disableTransport(id); return Map.of("mensaje", "Transporte desactivado correctamente."); }
}
