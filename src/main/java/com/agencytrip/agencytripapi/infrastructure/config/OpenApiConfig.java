package com.agencytrip.agencytripapi.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

/** Central OpenAPI metadata for the REST adapter. */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "AgencyTrip CO API",
                description = "Sistema Integral de Gestión y Planificación de Servicios Turísticos",
                version = "1.0"
        ),
        tags = {
                @Tag(name = "Usuarios", description = "Autenticación, perfil y gestión de usuarios"),
                @Tag(name = "Registro", description = "Creación de cuentas de clientes"),
                @Tag(name = "Destinos", description = "Catálogo de destinos turísticos"),
                @Tag(name = "Paquetes", description = "Paquetes turísticos y cupos"),
                @Tag(name = "Hoteles", description = "Catálogo de hoteles"),
                @Tag(name = "Transportes", description = "Catálogo de transportes"),
                @Tag(name = "Reservas", description = "Reservas, cupos y cancelaciones"),
                @Tag(name = "Pagos", description = "Pagos asociados a reservas"),
                @Tag(name = "Administración", description = "Roles y panel administrativo")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER,
        description = "Pegue un JWT válido obtenido en /api/Usuarios/login."
)
public class OpenApiConfig {
}
