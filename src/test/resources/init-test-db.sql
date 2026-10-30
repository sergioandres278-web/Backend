-- =============================================================================
-- AGENCYTRIP CO: Script DDL y Semilla para 'agencia_viajes_test'
-- NOTA: Archivo preparado para revisión técnica. NO EJECUTAR SIN AUTORIZACIÓN.
-- Base de datos aislada exclusiva para pruebas de integración y E2E.
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `agencia_viajes_test`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE `agencia_viajes_test`;

-- 1. Tabla: roles
CREATE TABLE IF NOT EXISTS `roles` (
  `id_rol` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(50) NOT NULL,
  PRIMARY KEY (`id_rol`),
  UNIQUE KEY `uk_roles_nombre` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 2. Tabla: destinos
CREATE TABLE IF NOT EXISTS `destinos` (
  `id_destino` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(100) NOT NULL,
  `pais` varchar(100) NOT NULL,
  `descripcion` text DEFAULT NULL,
  `imagen` varchar(255) DEFAULT NULL,
  `estado` tinyint(1) DEFAULT 1,
  PRIMARY KEY (`id_destino`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 3. Tabla: hoteles
CREATE TABLE IF NOT EXISTS `hoteles` (
  `id_hotel` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(150) NOT NULL,
  `direccion` varchar(255) DEFAULT NULL,
  `ciudad` varchar(100) DEFAULT NULL,
  `categoria` int DEFAULT NULL,
  `descripcion` text DEFAULT NULL,
  `estado` tinyint(1) DEFAULT 1,
  PRIMARY KEY (`id_hotel`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 4. Tabla: transportes
CREATE TABLE IF NOT EXISTS `transportes` (
  `id_transporte` int NOT NULL AUTO_INCREMENT,
  `tipo` varchar(50) NOT NULL,
  `empresa` varchar(100) DEFAULT NULL,
  `descripcion` text DEFAULT NULL,
  `estado` tinyint(1) DEFAULT 1,
  PRIMARY KEY (`id_transporte`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 5. Tabla: paquetes
CREATE TABLE IF NOT EXISTS `paquetes` (
  `id_paquete` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(150) NOT NULL,
  `descripcion` text DEFAULT NULL,
  `precio` decimal(12,2) NOT NULL,
  `fecha_salida` date NOT NULL,
  `fecha_regreso` date NOT NULL,
  `cupos` int NOT NULL,
  `id_destino` int NOT NULL,
  `estado` tinyint(1) DEFAULT 1,
  `id_transporte` int DEFAULT NULL,
  `id_hotel` int DEFAULT NULL,
  PRIMARY KEY (`id_paquete`),
  KEY `fk_paquete_destino` (`id_destino`),
  KEY `fk_paquete_hotel` (`id_hotel`),
  KEY `fk_paquete_transporte` (`id_transporte`),
  CONSTRAINT `fk_paquete_destino` FOREIGN KEY (`id_destino`) REFERENCES `destinos` (`id_destino`),
  CONSTRAINT `fk_paquete_hotel` FOREIGN KEY (`id_hotel`) REFERENCES `hoteles` (`id_hotel`),
  CONSTRAINT `fk_paquete_transporte` FOREIGN KEY (`id_transporte`) REFERENCES `transportes` (`id_transporte`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 6. Tabla: usuarios
CREATE TABLE IF NOT EXISTS `usuarios` (
  `id_usuario` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(100) NOT NULL,
  `apellido` varchar(100) NOT NULL,
  `correo` varchar(150) NOT NULL,
  `password` varchar(255) NOT NULL,
  `id_rol` int NOT NULL,
  `fecha_registro` datetime DEFAULT CURRENT_TIMESTAMP,
  `estado` tinyint(1) DEFAULT 1,
  PRIMARY KEY (`id_usuario`),
  UNIQUE KEY `uk_usuarios_correo` (`correo`),
  KEY `fk_usuario_rol` (`id_rol`),
  CONSTRAINT `fk_usuario_rol` FOREIGN KEY (`id_rol`) REFERENCES `roles` (`id_rol`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 7. Tabla: reservas
CREATE TABLE IF NOT EXISTS `reservas` (
  `id_reserva` int NOT NULL AUTO_INCREMENT,
  `id_usuario` int NOT NULL,
  `id_paquete` int NOT NULL,
  `fecha_reserva` datetime DEFAULT CURRENT_TIMESTAMP,
  `cantidad_personas` int NOT NULL,
  `precio_total` decimal(12,2) NOT NULL,
  `estado` varchar(30) DEFAULT 'PENDIENTE',
  PRIMARY KEY (`id_reserva`),
  KEY `fk_reserva_paquete` (`id_paquete`),
  KEY `fk_reserva_usuario` (`id_usuario`),
  CONSTRAINT `fk_reserva_paquete` FOREIGN KEY (`id_paquete`) REFERENCES `paquetes` (`id_paquete`),
  CONSTRAINT `fk_reserva_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 8. Tabla: pagos
CREATE TABLE IF NOT EXISTS `pagos` (
  `id_pago` int NOT NULL AUTO_INCREMENT,
  `id_reserva` int NOT NULL,
  `fecha_pago` datetime DEFAULT CURRENT_TIMESTAMP,
  `monto` decimal(12,2) NOT NULL,
  `metodo_pago` varchar(50) NOT NULL,
  `estado` varchar(30) DEFAULT 'PENDIENTE',
  PRIMARY KEY (`id_pago`),
  KEY `fk_pago_reserva` (`id_reserva`),
  CONSTRAINT `fk_pago_reserva` FOREIGN KEY (`id_reserva`) REFERENCES `reservas` (`id_reserva`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =============================================================================
-- DATOS SINTÉTICOS INICIALES (100% SEGUROS, SIN DATOS PERSONALES NI REALES)
-- =============================================================================

-- Roles del sistema (identificadores 1 y 2 requeridos por la lógica del backend)
INSERT INTO `roles` (`id_rol`, `nombre`) VALUES
  (1, 'ADMINISTRADOR'),
  (2, 'CLIENTE')
ON DUPLICATE KEY UPDATE `nombre` = VALUES(`nombre`);

-- Destino sintético de prueba
INSERT INTO `destinos` (`id_destino`, `nombre`, `pais`, `descripcion`, `imagen`, `estado`) VALUES
  (1, 'Cartagena Test', 'Colombia', 'Destino sintético para pruebas de integración', 'cartagena-test.jpg', 1)
ON DUPLICATE KEY UPDATE `nombre` = VALUES(`nombre`), `estado` = VALUES(`estado`);

-- Hotel sintético de prueba
INSERT INTO `hoteles` (`id_hotel`, `nombre`, `direccion`, `ciudad`, `categoria`, `descripcion`, `estado`) VALUES
  (1, 'Hotel Caribe Test', 'Calle 100 # 1-1', 'Cartagena', 5, 'Hotel de prueba para integración', 1)
ON DUPLICATE KEY UPDATE `nombre` = VALUES(`nombre`), `estado` = VALUES(`estado`);

-- Transporte sintético de prueba
INSERT INTO `transportes` (`id_transporte`, `tipo`, `empresa`, `descripcion`, `estado`) VALUES
  (1, 'AEREO', 'Aerolínea Test CO', 'Transporte aéreo de prueba', 1)
ON DUPLICATE KEY UPDATE `tipo` = VALUES(`tipo`), `estado` = VALUES(`estado`);

-- Paquete sintético con cupos suficientes para pruebas concurrentes y E2E
INSERT INTO `paquetes` (`id_paquete`, `nombre`, `descripcion`, `precio`, `fecha_salida`, `fecha_regreso`, `cupos`, `id_destino`, `estado`, `id_transporte`, `id_hotel`) VALUES
  (1, 'Paquete Sol y Playa Test', 'Paquete de prueba para suite de integración', 1500000.00, '2026-12-01', '2026-12-31', 50, 1, 1, 1, 1)
ON DUPLICATE KEY UPDATE `cupos` = VALUES(`cupos`), `estado` = VALUES(`estado`);

-- Usuarios sintéticos de prueba
-- Contraseña 'ClaveSegura123' cifrada con BCryptPasswordEncoder real: $2a$10$FhBfE70UAkAqLTtSZ7T6m.NQitN.1I6f9TSvuwkqTEZxQqqGjYyLW
INSERT INTO `usuarios` (`id_usuario`, `nombre`, `apellido`, `correo`, `password`, `id_rol`, `estado`) VALUES
  (1, 'Admin', 'Sintético', 'admin.test@agencytrip.com', '$2a$10$FhBfE70UAkAqLTtSZ7T6m.NQitN.1I6f9TSvuwkqTEZxQqqGjYyLW', 1, 1),
  (2, 'Cliente', 'Sintético', 'cliente.test@agencytrip.com', '$2a$10$FhBfE70UAkAqLTtSZ7T6m.NQitN.1I6f9TSvuwkqTEZxQqqGjYyLW', 2, 1)
ON DUPLICATE KEY UPDATE `correo` = VALUES(`correo`), `password` = VALUES(`password`), `estado` = VALUES(`estado`);

-- Paquete sin cupos para validar el rechazo por cupos insuficientes (Prueba 6.1)
INSERT INTO `paquetes` (`id_paquete`, `nombre`, `descripcion`, `precio`, `fecha_salida`, `fecha_regreso`, `cupos`, `id_destino`, `estado`, `id_transporte`, `id_hotel`) VALUES
  (2, 'Paquete Agotado Test', 'Paquete de prueba sin cupos disponibles para validar el rechazo del sistema', 2000000.00, '2026-12-01', '2026-12-31', 0, 1, 1, 1, 1)
ON DUPLICATE KEY UPDATE `cupos` = VALUES(`cupos`), `estado` = VALUES(`estado`);
