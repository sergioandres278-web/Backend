package com.agencytrip.agencytripapi.infrastructure.adapter.in.web;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.agencytrip.agencytripapi.application.dto.Requests;
import com.agencytrip.agencytripapi.application.service.UserService;
import com.agencytrip.agencytripapi.domain.model.User;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Registro", description = "Creación de cuentas de clientes para AgencyTrip CO.")
@RestController
@RequestMapping("/api/Registro")
public class RegistrationController {

    private final UserService service;

    public RegistrationController(UserService service) {
        this.service = service;
    }

    @Operation(
            summary = "Registrar un cliente",
            description = "Crea una cuenta activa con rol CLIENTE y almacena la contraseña mediante BCrypt.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario registrado correctamente"),
            @ApiResponse(responseCode = "400", description = "Los datos de registro no cumplen las validaciones")
    })
    @PostMapping
    public Map<String, Object> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Información necesaria para crear la cuenta del cliente.", required = true)
            @Valid @RequestBody Requests.Register request) {
        User user = service.register(request);
        return Map.of(
                "mensaje", "Usuario registrado correctamente.",
                "usuario", ApiResponses.userAuth(user, "CLIENTE"));
    }
}
