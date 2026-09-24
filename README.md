# AgencyTrip CO — Backend

Backend definitivo de AgencyTrip CO. Expone una API REST para usuarios, roles, destinos, paquetes, hoteles, transportes, reservas, pagos y el panel administrativo.

## Tecnologías

- Java 21
- Spring Boot 4.0.8
- Maven
- Spring Web MVC, Data JPA, Validation y Security
- JWT como OAuth2 Resource Server
- MySQL 8

## Arquitectura

El proyecto usa arquitectura hexagonal. La lógica de negocio se mantiene fuera de los controladores y del adaptador de persistencia.

```text
Angular
   ↓
REST API
   ↓
Application
   ↓
Domain
   ↓
Ports
   ↓
Persistence Adapter
   ↓
MySQL
```

Estructura principal:

```text
src/main/java/com/agencytrip/agencytripapi/
├── domain/                 # Modelos y puertos de salida
├── application/            # Casos de uso, DTO y puertos
└── infrastructure/         # Adaptadores web, persistencia y configuración
```

## Requisitos

- JDK 21
- MySQL 8 con la base de datos existente `agencia_viajes`
- Variables de entorno configuradas

## Variables de entorno

No almacenes credenciales, secretos JWT ni tokens en Git. Usa `.env.example` únicamente como referencia de nombres.

```text
DB_URL=
DB_USERNAME=
DB_PASSWORD=
JWT_SECRET=
PORT=8080
```

La aplicación usa `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` y `JWT_SECRET`. El puerto predeterminado es `8080` y puede ajustarse con `PORT`.

## Ambientes

La configuración pública se encuentra en:

- `application.properties`
- `application-dev.properties`
- `application-pre.properties`
- `application-prod.properties`

Activa un perfil con `SPRING_PROFILES_ACTIVE`, por ejemplo `dev`, `pre` o `prod`. Las credenciales se siguen tomando exclusivamente del entorno.

## Ejecutar

En PowerShell, desde la raíz del proyecto:

```powershell
.\mvnw.cmd spring-boot:run
```

La API queda disponible en `http://localhost:8080` de forma predeterminada.

## Pruebas y compilación

```powershell
.\mvnw.cmd clean package
```

## Endpoints principales

- `POST /api/Registro`
- `POST /api/Usuarios/login`
- `GET /api/Usuarios/perfil`
- `GET /api/Destinos`
- `GET /api/Paquetes`
- `GET /api/Hoteles`
- `GET /api/Transportes`
- `GET` y `POST /api/Reservas`
- `GET` y `POST /api/Pagos`
- `GET /api/Roles`
- `GET /api/Admin/panel`

Los endpoints administrativos y las operaciones autenticadas requieren un JWT válido con el rol correspondiente. CORS está configurado para el frontend Angular local en `http://localhost:4200`.
