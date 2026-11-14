# AgencyTrip CO — Backend

[![Backend CI](https://github.com/sergioandres278-web/Backend/actions/workflows/backend-ci.yml/badge.svg?branch=DEV)](https://github.com/sergioandres278-web/Backend/actions/workflows/backend-ci.yml)

API REST principal del Sistema Integral de Gestión y Planificación de Servicios Turísticos **AgencyTrip CO**. Gestiona usuarios, autenticación, catálogos, paquetes, reservas, pagos y operaciones administrativas.

## Componentes relacionados

- [Frontend Angular](https://github.com/sergioandres278-web/Frontend): interfaz web consumidora de la API.
- [Chatbot API](https://github.com/sergioandres278-web/Chatbot): asistente independiente integrado desde el frontend.

## Tecnologías y arquitectura

- Java 21, Spring Boot 4.0.8 y Maven Wrapper.
- Spring Web MVC, Validation, Data JPA, MySQL 8 y Actuator.
- Spring Security, JWT, OAuth2 Resource Server y SpringDoc OpenAPI.
- JUnit, Spring Test y JaCoCo.

El proyecto mantiene separación de estilo hexagonal:

```text
infrastructure/adapter/in/web  -> Controllers REST
application/                   -> Casos de uso y DTOs
domain/                        -> Modelo y puertos
infrastructure/adapter/out/    -> Persistencia y configuración
```

## API y seguridad

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Rutas principales: `Registro`, `Usuarios`, `Destinos`, `Hoteles`, `Transportes`, `Paquetes`, `Reservas`, `Pagos`, `Roles` y `Admin`, bajo el prefijo `/api`. Las operaciones protegidas requieren JWT; las administrativas requieren el rol `ADMINISTRADOR`.

## Requisitos y configuración

- JDK 21 y MySQL 8.
- Base local `agencia_viajes`.

Use `.env.example` solo como referencia. Nunca publique un `.env` real.

```text
DB_URL=jdbc:mysql://localhost:3306/agencia_viajes
DB_USERNAME=<usuario-local>
DB_PASSWORD=<secreto-local>
JWT_SECRET=<secreto-JWT-seguro>
JWT_ISSUER=AgencyTripAPI
JWT_AUDIENCE=AgencyTripAngular
PORT=8080
```

Los perfiles públicos están en `src/main/resources/environments/`. Active `dev`, `pre` o `prod` con `SPRING_PROFILES_ACTIVE`.

## Ejecución, pruebas y build

```powershell
.\mvnw.cmd spring-boot:run
.\mvnw.cmd clean test
.\mvnw.cmd clean package
```

## GitHub Actions y ambientes

[Backend CI](.github/workflows/backend-ci.yml) se ejecuta en `DEV`, `PRE`, `PROD` y `main`: configura Java 21 y MySQL efímero, ejecuta pruebas Maven, genera el JAR y publica reportes Surefire y JaCoCo. SonarQube es opcional y solo se activa con secretos configurados en GitHub.

Las ramas se mantienen separadas; el workflow no promueve ambientes ni despliega infraestructura.

## Arquitectura del sistema

El diagrama de integración entre Frontend, Nginx, Backend, Chatbot y MySQL está disponible en el repositorio de Frontend: [arquitectura general de AgencyTrip CO](https://github.com/sergioandres278-web/Frontend/blob/DEV/docs/architecture.md).

## Seguridad

`.gitignore` excluye `.env`, certificados, claves privadas, almacenes de claves y respaldos locales. No publique contraseñas, tokens OAuth, secretos JWT ni cadenas de conexión reales.
