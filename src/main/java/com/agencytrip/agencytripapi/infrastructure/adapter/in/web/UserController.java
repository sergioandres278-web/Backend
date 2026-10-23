package com.agencytrip.agencytripapi.infrastructure.adapter.in.web;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.agencytrip.agencytripapi.application.dto.Requests;
import com.agencytrip.agencytripapi.application.service.UserService;
import com.agencytrip.agencytripapi.domain.model.User;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Usuarios", description = "Gestión de cuentas, autenticación y perfiles de usuarios.")
@RestController
@RequestMapping("/api/Usuarios")
public class UserController {

    private final UserService service;

    public UserController(UserService s) {
        service = s;
    }

    @Operation(
        summary = "Registrar usuario",
        description = "Registra una nueva cuenta de cliente y protege su contraseña mediante BCrypt."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuario registrado correctamente"),
        @ApiResponse(responseCode = "400", description = "Los datos de registro no cumplen las validaciones")
    })
    @PostMapping("/registro")
    public Map<String, Object> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Información requerida para crear la cuenta del usuario.", required = true)
            @Valid @RequestBody Requests.Register r) {

        User u = service.register(r);

        return Map.of(
            "mensaje", "Usuario registrado correctamente.",
            "usuario", ApiResponses.userAdmin(u)
        );
    }

    @Operation(
        summary = "Iniciar sesión",
        description = "Valida credenciales y devuelve un JWT con vigencia de dos horas."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @ApiResponse(responseCode = "200", description = "Inicio de sesión realizado correctamente"),
        @ApiResponse(responseCode = "400", description = "Las credenciales no cumplen las validaciones"),
        @ApiResponse(responseCode = "401", description = "Correo o contraseña incorrectos")
    })
    @PostMapping("/login")
    public Map<String, Object> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Credenciales de acceso del usuario.", required = true)
            @Valid @RequestBody Requests.Login r) {

        var x = service.login(r);

        return Map.of(
            "mensaje", "Inicio de sesión exitoso.",
            "token", x.token(),
            "usuario", ApiResponses.userAuth(x.user(), x.role())
        );
    }

    @Operation(
        summary = "Consultar perfil autenticado",
        description = "Obtiene la información disponible en el token JWT del usuario autenticado.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @ApiResponse(responseCode = "200", description = "Perfil obtenido correctamente"),
        @ApiResponse(responseCode = "401", description = "Se requiere un token JWT válido")
    })
    @GetMapping("/perfil")
    public Map<String, Object> profile(
            @AuthenticationPrincipal Jwt jwt) {

        return Map.of(
            "mensaje", "Perfil obtenido correctamente.",
            "usuario", jwt.getSubject(),
            "correo", jwt.getClaimAsString("email"),
            "rol", jwt.getClaimAsStringList("roles").getFirst()
        );
    }

    @Operation(
        summary = "Listar usuarios activos",
        description = "Obtiene las cuentas de usuario activas. Requiere el rol ADMINISTRADOR.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado de usuarios obtenido correctamente"),
        @ApiResponse(responseCode = "401", description = "Se requiere un token JWT válido"),
        @ApiResponse(responseCode = "403", description = "El usuario no cuenta con el rol ADMINISTRADOR")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<com.agencytrip.agencytripapi.infrastructure.adapter.in.web.ApiResponses.UserAdmin> users() {

        return service.users()
                .stream()
                .map(ApiResponses::userAdmin)
                .toList();
    }

    @Operation(
        summary = "Actualizar usuario",
        description = "Actualiza la información de una cuenta de usuario. Requiere el rol ADMINISTRADOR.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuario actualizado correctamente"),
        @ApiResponse(responseCode = "400", description = "Los datos de actualización no cumplen las validaciones"),
        @ApiResponse(responseCode = "401", description = "Se requiere un token JWT válido"),
        @ApiResponse(responseCode = "403", description = "El usuario no cuenta con el rol ADMINISTRADOR"),
        @ApiResponse(responseCode = "404", description = "El usuario no existe")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Map<String, Object> update(
            @Parameter(description = "Identificador del usuario a actualizar.", required = true) @PathVariable int id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Información actualizada de la cuenta de usuario.", required = true)
            @Valid @RequestBody Requests.UserUpdate r,
            @AuthenticationPrincipal Jwt jwt) {

        int authUserId = extractUserId(jwt);
        User u = service.update(id, r, authUserId);

        return Map.of(
            "mensaje", "Usuario actualizado correctamente.",
            "usuario", ApiResponses.userAdmin(u)
        );
    }

    @Operation(
        summary = "Desactivar usuario",
        description = "Cambia el estado de una cuenta de usuario a inactivo. Requiere el rol ADMINISTRADOR.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuario desactivado correctamente"),
        @ApiResponse(responseCode = "400", description = "El usuario ya está desactivado"),
        @ApiResponse(responseCode = "401", description = "Se requiere un token JWT válido"),
        @ApiResponse(responseCode = "403", description = "El usuario no cuenta con el rol ADMINISTRADOR"),
        @ApiResponse(responseCode = "404", description = "El usuario no existe")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Map<String, String> disable(
            @Parameter(description = "Identificador del usuario a desactivar.", required = true) @PathVariable int id,
            @AuthenticationPrincipal Jwt jwt) {

        int authUserId = extractUserId(jwt);
        service.disable(id, authUserId);

        return Map.of(
            "mensaje", "Usuario desactivado correctamente."
        );
    }

    private static int extractUserId(Jwt jwt) {
        if (jwt == null || jwt.getClaim("userId") == null) {
            throw new com.agencytrip.agencytripapi.application.service.BusinessException(
                org.springframework.http.HttpStatus.UNAUTHORIZED,
                "Token JWT inválido: no contiene identificador de usuario."
            );
        }
        return ((Number) jwt.getClaim("userId")).intValue();
    }
}
