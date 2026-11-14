package com.agencytrip.agencytripapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.Test;

class ConfigurationProfilesTests {

    @Test
    void commonConfigurationImportsEveryEnvironmentProfile() throws IOException {
        Properties common = load("application.properties");

        String imports = common.getProperty("spring.config.import");

        assertTrue(imports.contains("environments/application-dev.properties"));
        assertTrue(imports.contains("environments/application-pre.properties"));
        assertTrue(imports.contains("environments/application-prod.properties"));
    }

    @Test
    void environmentResourcesActivateTheirCorrespondingProfiles() throws IOException {
        assertProfile("environments/application-dev.properties", "dev", "true");
        assertProfile("environments/application-pre.properties", "pre", "false");
        assertProfile("environments/application-prod.properties", "prod", "false");
    }

    private void assertProfile(String resource, String profile, String showSql) throws IOException {
        Properties properties = load(resource);

        assertEquals(profile, properties.getProperty("spring.config.activate.on-profile"));
        assertEquals(showSql, properties.getProperty("spring.jpa.show-sql"));
    }

    private Properties load(String resource) throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            Properties properties = new Properties();
            properties.load(input);
            return properties;
        }
    }
}
