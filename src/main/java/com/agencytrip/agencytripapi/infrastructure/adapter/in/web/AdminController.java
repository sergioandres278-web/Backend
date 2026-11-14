package com.agencytrip.agencytripapi.infrastructure.adapter.in.web;

import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.agencytrip.agencytripapi.application.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Administración", description = "Operaciones exclusivas para usuarios con rol ADMINISTRADOR.")
@RestController
public class AdminController {

    private final UserService users;

    public AdminController(UserService users) {
        this.users = users;
    }

    @Operation(
            summary = "Listar roles del sistema",
            description = "Obtiene los roles disponibles para la gestión de usuarios.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Roles obtenidos correctamente"),
            @ApiResponse(responseCode = "401", description = "Se requiere un token JWT válido"),
            @ApiResponse(responseCode = "403", description = "El usuario no cuenta con el rol ADMINISTRADOR")
    })
    @GetMapping("/api/Roles")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Object roles() {
        return users.roles();
    }

    @Operation(
            summary = "Consultar panel administrativo",
            description = "Confirma el acceso autorizado al panel de administración.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Acceso autorizado al panel administrativo"),
            @ApiResponse(responseCode = "401", description = "Se requiere un token JWT válido"),
            @ApiResponse(responseCode = "403", description = "El usuario no cuenta con el rol ADMINISTRADOR")
    })
    @GetMapping("/api/Admin/panel")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Object panel() {
        return Map.of("mensaje", "Acceso autorizado al panel de administrador.", "rol", "ADMINISTRADOR");
    }
}
