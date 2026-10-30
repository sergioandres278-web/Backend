package com.agencytrip.agencytripapi.infrastructure.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Salvaguarda estricta de aislamiento de base de datos para pruebas.
 *
 * Aplica una lista blanca rigida (whitelisting):
 * La conexion efectiva DEBE pertenecer unica y exclusivamente a 'agencia_viajes_test'.
 *
 * Se ejecuta automaticamente mediante BeanPostProcessor en el momento en que se instancia el DataSource,
 * impidiendo que JPA/Hibernate o cualquier repositorio interactue con una base de datos incorrecta.
 */
@Component
@Profile("test")
public class TestDatabaseSafetyGuard implements BeanPostProcessor, ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TestDatabaseSafetyGuard.class);
    public static final String REQUIRED_TEST_DATABASE = "agencia_viajes_test";

    private final DataSource dataSource;

    @Autowired
    public TestDatabaseSafetyGuard(@Autowired(required = false) DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public TestDatabaseSafetyGuard() {
        this(null);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource ds) {
            validateDataSource(ds);
        }
        return bean;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (dataSource != null) {
            validateDataSource(dataSource);
        }
    }

    public void validateDataSource(DataSource ds) {
        if (ds == null) {
            throw new IllegalArgumentException("PROTECCION DE SEGURIDAD ACTIVADA: El DataSource suministrado es nulo.");
        }

        try (Connection connection = ds.getConnection()) {
            if (connection == null) {
                throw new IllegalStateException("PROTECCION DE SEGURIDAD ACTIVADA: La conexion obtenida del DataSource es nula.");
            }

            String catalog = connection.getCatalog();
            DatabaseMetaData metaData = connection.getMetaData();
            String url = (metaData != null) ? metaData.getURL() : null;

            log.info("TestDatabaseSafetyGuard: Validando catalogo efectivo de conexion para pruebas: '{}' (URL: '{}')", catalog, url);

            if (catalog == null || !REQUIRED_TEST_DATABASE.equalsIgnoreCase(catalog.trim())) {
                throw new IllegalStateException(
                    "PROTECCION DE SEGURIDAD ACTIVADA: Se ha bloqueado la ejecucion porque el catalogo efectivo '"
                    + catalog + "' no coincide con la base de datos de pruebas requerida ('" + REQUIRED_TEST_DATABASE + "'). "
                    + "Por seguridad absoluta, no se permite ejecutar pruebas contra la base de datos principal ni contra ninguna base distinta a '"
                    + REQUIRED_TEST_DATABASE + "'."
                );
            }

            if (url != null && !url.toLowerCase().contains(REQUIRED_TEST_DATABASE.toLowerCase())) {
                throw new IllegalStateException(
                    "PROTECCION DE SEGURIDAD ACTIVADA: La URL JDBC configurada ('" + url + "') "
                    + "no contiene el identificador de la base de datos de pruebas requerida ('" + REQUIRED_TEST_DATABASE + "')."
                );
            }

            log.info("TestDatabaseSafetyGuard: Aislamiento verificado con exito en catalogo '{}'.", catalog);
        } catch (SQLException e) {
            throw new IllegalStateException(
                "PROTECCION DE SEGURIDAD ACTIVADA: No se pudo verificar el aislamiento porque ocurrio un error al obtener la conexion JDBC con el servidor de base de datos. "
                + "Detalle: " + e.getMessage(), e
            );
        }
    }
}