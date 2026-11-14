package com.agencytrip.agencytripapi;

import com.agencytrip.agencytripapi.infrastructure.config.TestDatabaseSafetyGuard;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TestDatabaseSafetyGuardTests {

    @Test
    @DisplayName("1. Permite la ejecucion unicamente si el catalogo es exactamente 'agencia_viajes_test'")
    void allowsWhenCatalogIsExactlyTestDatabase() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData meta = mock(DatabaseMetaData.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getCatalog()).thenReturn("agencia_viajes_test");
        when(connection.getMetaData()).thenReturn(meta);
        when(meta.getURL()).thenReturn("jdbc:mysql://localhost:3306/agencia_viajes_test");

        TestDatabaseSafetyGuard guard = new TestDatabaseSafetyGuard();

        assertDoesNotThrow(() -> guard.validateDataSource(dataSource));
    }

    @Test
    @DisplayName("2. Bloquea la ejecucion si el catalogo es la base de datos principal 'agencia_viajes'")
    void blocksWhenCatalogIsProductionOrDev() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData meta = mock(DatabaseMetaData.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getCatalog()).thenReturn("agencia_viajes");
        when(connection.getMetaData()).thenReturn(meta);
        when(meta.getURL()).thenReturn("jdbc:mysql://localhost:3306/agencia_viajes");

        TestDatabaseSafetyGuard guard = new TestDatabaseSafetyGuard();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> guard.validateDataSource(dataSource));
        assertTrue(ex.getMessage().contains("DE SEGURIDAD ACTIVADA"));
        assertTrue(ex.getMessage().contains("agencia_viajes"));
    }

    @Test
    @DisplayName("3. Bloquea la ejecucion si el catalogo es una base de datos distinta (ej. 'produccion' u 'otra_base')")
    void blocksWhenCatalogIsDifferentDatabase() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData meta = mock(DatabaseMetaData.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getCatalog()).thenReturn("otra_base_datos");
        when(connection.getMetaData()).thenReturn(meta);
        when(meta.getURL()).thenReturn("jdbc:mysql://localhost:3306/otra_base_datos");

        TestDatabaseSafetyGuard guard = new TestDatabaseSafetyGuard();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> guard.validateDataSource(dataSource));
        assertTrue(ex.getMessage().contains("DE SEGURIDAD ACTIVADA"));
        assertTrue(ex.getMessage().contains("otra_base_datos"));
    }

    @Test
    @DisplayName("4. Bloquea y reporta error si la conexion no esta disponible (lanza SQLException)")
    void blocksWhenConnectionIsNotAvailable() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("Communications link failure: Connection refused"));

        TestDatabaseSafetyGuard guard = new TestDatabaseSafetyGuard();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> guard.validateDataSource(dataSource));
        assertTrue(ex.getMessage().contains("No se pudo verificar el aislamiento"));
        assertTrue(ex.getMessage().contains("Connection refused"));
    }

    @Test
    @DisplayName("5. Rechaza adecuadamente un DataSource nulo")
    void rejectsNullDataSource() {
        TestDatabaseSafetyGuard guard = new TestDatabaseSafetyGuard();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> guard.validateDataSource(null));
        assertTrue(ex.getMessage().contains("El DataSource suministrado es nulo"));
    }

    @Test
    @DisplayName("6. Genera y verifica hash BCrypt compatible para contrasenas de usuarios sinteticos")
    void generatesAndVerifiesBcryptHash() {
        org.springframework.security.crypto.password.PasswordEncoder encoder =
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

        String rawPassword = "ClaveSegura123";
        String hash = encoder.encode(rawPassword);

        assertTrue(encoder.matches(rawPassword, hash), "El hash generado debe coincidir con la contrasena en texto plano");
        org.junit.jupiter.api.Assertions.assertFalse(encoder.matches("ClaveErroneaInvalida", hash), "Un intento con contrasena erronea no debe coincidir con el hash");
    }
}