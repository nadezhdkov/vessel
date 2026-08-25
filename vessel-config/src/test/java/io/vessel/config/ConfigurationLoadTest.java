package io.vessel.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurationLoadTest {

    @Test
    void loadReadsEveryPropertyFromTheGivenClasspathResource() {
        var environment = Configuration.load("test-config.properties");

        assertEquals("vessel-test", environment.require("app.name"));
        assertEquals(9090, environment.get("app.port", Integer.class));
        assertEquals(Boolean.TRUE, environment.get("app.debug", Boolean.class));
        assertEquals(3, environment.get("app.retries", Integer.class));
    }

    @Test
    void aMissingPropertiesFileIsTreatedAsAnEmptySourceRatherThanAnError() {
        var environment = Configuration.load("this-file-does-not-exist.properties");

        assertTrue(environment.get("anything").isEmpty());
    }

    @Test
    void loadWithNoArgumentsDefaultsToApplicationPropertiesOnTheClasspathWithoutThrowing() {
        // No application.properties ships with this module's tests, so this
        // only proves the default resource name resolves to an empty (not
        // failing) source rather than exercising real file content.
        var environment = Configuration.load();

        assertNotNull(environment);
        assertTrue(environment.get("anything.at.all").isEmpty());
    }
}
