# GUÍA OFICIAL DE CONTENERIZACIÓN CON DOCKER — AGENCYTRIP CO

Esta guía describe la arquitectura de contenerización, configuración, construcción y despliegue del sistema integral **AgencyTrip CO** utilizando **Docker** y **Docker Compose**.

---

## 1. DESCRIPCIÓN DE COMPONENTES

El ecosistema AgencyTrip CO se compone de cuatro piezas tecnológicas coordinadas:

| Servicio | Contenedor | Tecnología | Puerto Interno | Puerto Host | Descripción |
| :--- | :--- | :--- | :---: | :---: | :--- |
| **Frontend** | `agencytrip-frontend` | Angular + Nginx (Alpine) | `80` | `4200` | Interfaz de usuario SPA y Reverse Proxy hacia los servicios backend. |
| **API Backend** | `agencytrip-api` | Spring Boot 4.0.8 (Java 21) | `8080` | `8080` | Gestión de paquetes, destinos, reservas, pagos, seguridad Spring Security y JWT. |
| **Chatbot API**| `agencytrip-chatbot` | Spring Boot (Java 21) | `8081` | `8081` | Motor de asistencia conversacional, procesamiento de criterios turísticos y consultas HTTP a la API. |
| **Base de Datos**| MySQL Server 8.4 | MySQL Relacional | `3306` | `3306` | Persistencia transaccional de la base de datos `agencia_viajes`. |

---

## 2. REQUISITOS PREVIOS

1. **Sistema Operativo:** Windows 10/11 con WSL2 (o Linux / macOS con Docker Engine).
2. **Docker Desktop:** Versión 4.x o superior en ejecución (compatible con Docker 25+ y Docker Compose v2 / v5+).
3. **Base de Datos MySQL:** MySQL Server 8.4 instalado en el equipo anfitrión escuchando en el puerto `3306` con la base de datos `agencia_viajes` y los datos precargados.
4. **Shell:** Windows PowerShell o PowerShell 7.

---

## 3. ESTRUCTURA DE ARCHIVOS DE CONTENERIZACIÓN

```text
C:\Agencia de Viajes\
├── compose.yaml                                     # Orquestador principal de Docker Compose
├── .env                                             # Variables de entorno activas (no versionar)
├── agencytrip-api\
│   ├── Dockerfile                                   # Imagen de ejecución Java 21 JRE
│   ├── .dockerignore                                # Exclusiones de contexto de construcción
│   ├── .env.example                                 # Plantilla de variables de entorno
│   ├── README-DOCKER.md                             # Esta documentación técnica
│   └── compose.yaml                                 # Manifiesto espejo de respaldo
├── agencytrip-chatbot-api\
│   └── agencytrip-chatbot-api\
│       ├── Dockerfile                               # Imagen Java 21 optimizada (G1GC)
│       ├── .dockerignore                            # Exclusiones de contexto
│       └── test-chat.ps1                            # Utilidad de prueba para el endpoint conversacional
└── agencia-viajes-frontend\
    ├── Dockerfile                                   # Construcción multietapa (Node 24 -> Nginx)
    ├── .dockerignore                                # Exclusiones de node_modules y dist
    └── nginx\
        └── nginx.conf                               # Servidor web y reverse proxy
```

---

## 4. VARIABLES DE ENTORNO Y CONFIGURACIÓN LOCAL

Antes de iniciar la aplicación, debe existir un archivo `.env` en la raíz donde se ejecuta `docker compose`. Puede generarlo a partir de `.env.example`:

```powershell
Copy-Item ".\agencytrip-api\.env.example" ".\.env"
```

### Variables requeridas:

| Variable | Descripción | Valor recomendado / Ejemplo |
| :--- | :--- | :--- |
| `DB_HOST` | Host de conexión a MySQL | `host.docker.internal` (para MySQL en Windows) |
| `DB_PORT` | Puerto de MySQL | `3306` |
| `DB_NAME` | Nombre de la base de datos | `agencia_viajes` |
| `DB_USERNAME` | Usuario de MySQL con privilegios | `REPLACE_WITH_LOCAL_DB_USERNAME` |
| `DB_PASSWORD` | Contraseña del usuario MySQL | `REPLACE_WITH_LOCAL_DB_PASSWORD` |
| `JWT_SECRET` | Clave secreta HMAC-SHA de 256 bits | `REPLACE_WITH_A_SECURE_RANDOM_SECRET` |
| `JWT_ISSUER` | Emisor de los tokens JWT | `AgencyTripAPI` |
| `JWT_AUDIENCE` | Audiencia esperada del frontend | `AgencyTripAngular` |
| `PORT` | Puerto interno de la API principal | `8080` |
| `AGENCYTRIP_API_URL` | URL interna para el chatbot | `http://agencytrip-api:8080` |
| `CHATBOT_CORS_ALLOWED_ORIGIN` | Origen CORS autorizado para chatbot | `http://localhost:4200` |

> [!CAUTION]
> **Seguridad:** Nunca incluya credenciales reales, tokens o secretos en repositorios públicos de Git. El archivo `.env` debe permanecer ignorado en `.gitignore`.

---

## 5. CONSTRUCCIÓN Y DESPLIEGUE

### 5.1. Validación de la sintaxis del Compose
Valide que no existan errores de estructura ni rutas faltantes:

```powershell
docker compose config
```

### 5.2. Construcción de imágenes
Construya las imágenes de los tres servicios respetando las dependencias:

```powershell
docker compose build
```

* El frontend compilará Angular en una etapa Node.js y transferirá los artefactos a Nginx Alpine.
* El backend principal y el chatbot empaquetarán sus archivos JAR ejecutables sobre imágenes base oficiales de `eclipse-temurin:21-jre`.

### 5.3. Inicio de los servicios en segundo plano
Inicie los tres contenedores de forma ordenada:

```powershell
docker compose up -d
```

Gracias a las directivas `healthcheck` y `depends_on: condition: service_healthy`:
1. Primero arranca `agencytrip-api` y espera a que la base de datos responda y el endpoint `/api/Paquetes` sea saludable.
2. Luego arranca `agencytrip-chatbot`, que se conecta de inmediato a la API saludable.
3. Finalmente arranca `agencytrip-frontend`, garantizando que ningún usuario reciba errores de conexión al cargar la página.

### 5.4. Comprobación del estado de los contenedores

```powershell
docker compose ps
```

Debe observar el estado **`(healthy)`** en los tres contenedores:
```text
NAME                  IMAGE                      STATUS                        PORTS
agencytrip-api        agenciadeviajes-api        Up (healthy)                  0.0.0.0:8080->8080/tcp
agencytrip-chatbot    agenciadeviajes-chatbot    Up (healthy)                  0.0.0.0:8081->8081/tcp
agencytrip-frontend   agenciadeviajes-frontend   Up (healthy)                  0.0.0.0:4200->80/tcp
```

### 5.5. Consulta de registros (Logs)

* Ver todos los logs en tiempo real:
  ```powershell
  docker compose logs -f
  ```
* Ver logs de un servicio específico:
  ```powershell
  docker compose logs -f api
  docker compose logs -f chatbot
  docker compose logs -f frontend
  ```

### 5.6. Detención de los servicios

Para detener los contenedores conservando las imágenes y los volúmenes:

```powershell
docker compose down
```

> [!WARNING]
> **REGLA CRÍTICA:** No ejecute `docker compose down -v`. La bandera `-v` elimina volúmenes de datos. Para proteger la información de la base de datos, utilice únicamente `docker compose down`.

---

## 6. VERIFICACIÓN Y PRUEBAS DE FUNCIONAMIENTO

Una vez iniciados los servicios, ejecute las siguientes comprobaciones desde PowerShell:

### 1. Frontend Angular (Nginx)
```powershell
curl.exe -i http://localhost:4200/
```
* **Respuesta esperada:** `HTTP/1.1 200 OK` con el HTML principal de Angular (`<app-root></app-root>`).

### 2. Archivos estáticos de Angular
```powershell
curl.exe -I http://localhost:4200/main-IW2MY2AY.js
curl.exe -I http://localhost:4200/styles-XSGAZWZ7.css
```
* **Respuesta esperada:** `HTTP/1.1 200 OK`.

### 3. API Principal (Destinos y Paquetes vía Nginx)
```powershell
curl.exe -s http://localhost:4200/api/Destinos
curl.exe -s http://localhost:4200/api/Paquetes
```
* **Respuesta esperada:** Listados JSON con 12 destinos y 36 paquetes activos provistos desde MySQL.

### 4. Healthcheck del Chatbot vía Nginx
```powershell
curl.exe -s http://localhost:4200/api/chatbot/health
```
* **Respuesta esperada:** `{"mensaje":"La API externa del chatbot funciona correctamente.","estado":"ACTIVO","servicio":"AgencyTrip CO Chatbot API"}`.

### 5. Prueba de Conversación del Chatbot
```powershell
$body = @{ mensaje = "Hola, busco paquetes a Cartagena"; contexto = @() } | ConvertTo-Json -Compress
Invoke-RestMethod -Uri "http://localhost:4200/api/chatbot/conversar" -Method Post -ContentType "application/json; charset=utf-8" -Body $body | Select-Object -ExpandProperty respuesta
```
* **Respuesta esperada:** Recomendación en tiempo real del paquete real "Cartagena Confort" o "Cartagena Flexible" con hotel y transporte.

### 6. Seguridad del Backend Principal
```powershell
curl.exe -I http://localhost:8080/api/Destinos
```
* **Respuesta esperada:** `HTTP/1.1 401 Unauthorized` con cabecera `WWW-Authenticate: Bearer` (Valida que Spring Security protege los recursos no públicos).

---

## 7. POLÍTICA DE CONEXIÓN CON MYSQL Y PROTECCIÓN DE DATOS

La persistencia de AgencyTrip CO opera sobre la base de datos relacional `agencia_viajes`.

### Modo Actual (Host Gateway):
Por defecto, `compose.yaml` define:
```yaml
extra_hosts:
  - "host.docker.internal:host-gateway"
environment:
  DB_URL: jdbc:mysql://host.docker.internal:3306/agencia_viajes
```
Esto permite que los contenedores se comuniquen con el servicio MySQL 8.4 que ya corre en el host Windows, protegiendo al 100% los registros existentes (13 destinos, 36 paquetes, 82 reservas y 14 pagos).

### Modo Autónomo (MySQL en Contenedor Opcional):
Si en un entorno de laboratorio se requiere contenerizar también MySQL, se puede activar un servicio `mysql` en `compose.yaml`:
```yaml
  mysql:
    image: mysql:8.4
    container_name: agencytrip-mysql
    restart: unless-stopped
    environment:
      MYSQL_DATABASE: agencia_viajes
      MYSQL_USER: REPLACE_WITH_LOCAL_DB_USERNAME
      MYSQL_PASSWORD: REPLACE_WITH_LOCAL_DB_PASSWORD
      MYSQL_ROOT_PASSWORD: REPLACE_WITH_LOCAL_DB_ROOT_PASSWORD
    volumes:
      - mysql_data:/var/lib/mysql
      - ./mysql-init:/docker-entrypoint-initdb.d:ro
    ports:
      - "3306:3306"
```
Y apuntar `DB_URL: jdbc:mysql://mysql:3306/agencia_viajes`.

---

## 8. RESOLUCIÓN DE PROBLEMAS FRECUENTES

| Problema observado | Causa raíz | Solución |
| :--- | :--- | :--- |
| **`ERR_EMPTY_RESPONSE` al abrir localhost:4200** | Procesos zombi de Docker Desktop en Windows retienen puertos antiguos o sockets WSL2 huérfanos. | Detener procesos viejos de `com.docker.backend.exe`, ejecutar `wsl --shutdown` y reiniciar Docker Desktop. |
| **`container agencytrip-api is unhealthy`** | La API no logra conectarse a MySQL o `$env:DB_URL` del host contiene `localhost` en vez de `host.docker.internal`. | Verificar que MySQL en el host esté activo en el puerto 3306 y que el archivo `.env` use `DB_HOST=host.docker.internal`. |
| **Chatbot responde catálogo de respaldo en vez de datos reales** | La variable `AGENCYTRIP_API_URL` apunta a una dirección inalcanzable. | Asegurar que `AGENCYTRIP_API_URL=http://agencytrip-api:8080` esté definida en `compose.yaml`. |
| **`Missing required variable DB_USERNAME`** | El archivo `.env` no existe en la carpeta donde se ejecutó `docker compose`. | Copiar `.env.example` como `.env` en la raíz del proyecto antes de ejecutar `docker compose up`. |
