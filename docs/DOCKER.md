# Guía de Operación con Docker — AgencyTrip CO

Esta guía contiene las instrucciones y comandos verificados para gestionar los contenedores de **AgencyTrip CO** utilizando Docker Compose.

---

## 1. Arquitectura de Servicios

El sistema se compone de tres servicios orquestados por Docker Compose y una base de datos MySQL local:

| Servicio | Tecnología | Puerto Contenedor | Puerto Host | Descripción |
| :--- | :--- | :--- | :--- | :--- |
| **frontend** | Angular + Nginx | 80 | `4200` | Aplicación web y Reverse Proxy para APIs |
| **api** | Spring Boot (Java 21) | 8080 | `8080` | API principal del sistema y persistencia |
| **chatbot** | Spring Boot (Java 21) | 8081 | `8081` | Asistente virtual y recomendaciones |
| **mysql** | MySQL 8.4 (Local Host) | 3306 | `3306` | Base de datos `agencia_viajes` en máquina host |

---

## 2. Requisitos Previos y Variables de Entorno

Antes de iniciar los servicios, asegúrese de que:
1. **Docker Desktop** esté en ejecución.
2. La instancia local de **MySQL 8.4** esté iniciada en el puerto 3306 y contenga la base de datos `agencia_viajes`.

### Configuración de Variables de Entorno

El archivo `compose.yaml` requiere que se definan las siguientes variables de entorno:

* `DB_USERNAME`: Usuario de la base de datos MySQL.
* `DB_PASSWORD`: Contraseña de la base de datos MySQL.
* `JWT_SECRET`: Clave secreta para la firma y validación de tokens JWT.
* `JWT_ISSUER` *(opcional, por defecto: `AgencyTripAPI`)*.
* `JWT_AUDIENCE` *(opcional, por defecto: `AgencyTripAngular`)*.

Puede definirlas en su sesión de PowerShell:

```powershell
$env:DB_USERNAME = "tu_usuario"
$env:DB_PASSWORD = "tu_password"
$env:JWT_SECRET = "tu_clave_secreta_jwt"
```

O crear un archivo `.env` en la raíz del proyecto (`C:\Agencia de Viajes\.env`) basándose en la plantilla [.env.example](../agencytrip-api/.env.example).

> **Aviso de seguridad:** Nunca guarde contraseñas ni claves secretas reales en archivos versionados por Git.

---

## 3. Comandos de Gestión del Ciclo de Vida

Ejecute estos comandos desde el directorio principal (`C:\Agencia de Viajes`) o especificando la ruta al archivo `-f "C:\Agencia de Viajes\compose.yaml"`.

### A. Iniciar los servicios

Para construir las imágenes (si no existen) y levantar todos los contenedores en segundo plano:

```powershell
docker compose up -d
```

### B. Consultar el estado de los contenedores

Para comprobar qué servicios están activos y sus puertos mapeados:

```powershell
docker compose ps
```

### C. Ver registros (logs) en tiempo real

Ver registros de todos los servicios:
```powershell
docker compose logs -f
```

Ver registros de un servicio específico:
```powershell
# API principal
docker compose logs -f --tail=100 api

# Chatbot
docker compose logs -f --tail=100 chatbot

# Frontend / Nginx
docker compose logs -f --tail=100 frontend
```

### D. Detener los servicios

* **Pausar/Detener contenedores (conservando su estado):**
  ```powershell
  docker compose stop
  ```

* **Detener y remover contenedores:**
  ```powershell
  docker compose down
  ```

> **Importante:** No ejecute `docker compose down -v`. La base de datos corre en el host, pero evitar banderas destructivas protege cualquier volumen futuro.

### E. Volver a iniciar los servicios

* Si se usó `docker compose stop`:
  ```powershell
  docker compose start
  ```

* Si se usó `docker compose down`:
  ```powershell
  docker compose up -d
  ```

### F. Reconstruir un servicio específico

Cuando se actualice el código fuente de un componente:

1. **Reconstruir la API principal:**
   *(Requiere compilar primero el JAR con `./mvnw clean package -DskipTests`)*
   ```powershell
   docker compose build api
   docker compose up -d --no-deps api
   ```

2. **Reconstruir el Chatbot:**
   *(Requiere compilar primero el JAR en su directorio correspondiente)*
   ```powershell
   docker compose build chatbot
   docker compose up -d --no-deps chatbot
   ```

3. **Reconstruir el Frontend:**
   *(El Dockerfile compila internamente con Node 24 y Nginx)*
   ```powershell
   docker compose build frontend
   docker compose up -d --no-deps frontend
   ```

---

## 4. Endpoints de Verificación Rápida

Una vez levantados los contenedores, puede verificar el funcionamiento en los siguientes puntos de acceso:

| Verificación | URL | Método | Resultado esperado |
| :--- | :--- | :--- | :--- |
| **Frontend Web** | `http://localhost:4200` | GET | HTTP 200 (HTML Angular) |
| **Catálogo Destinos** | `http://localhost:4200/api/Destinos` | GET | HTTP 200 (JSON lista de destinos) |
| **Chatbot Health** | `http://localhost:4200/api/chatbot/health` | GET | HTTP 200 (`"estado": "ACTIVO"`) |
| **Chatbot Conversar** | `http://localhost:4200/api/chatbot/conversar` | POST | HTTP 200 (Respuesta del asistente) |
