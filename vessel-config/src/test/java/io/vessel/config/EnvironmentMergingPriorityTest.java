package io.vessel.config;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvironmentMergingPriorityTest {

    private PropertySource sourceOf(Map<String, String> values) {
        return key -> Optional.ofNullable(values.get(key));
    }

    @Test
    void theFirstSourceInPriorityOrderWinsWhenMultipleDefineTheSameKey() {
        var highestPriority = sourceOf(Map.of("server.port", "9999"));
        var lowestPriority = sourceOf(Map.of("server.port", "8080", "server.host", "localhost"));
        var environment = new Environment(List.of(highestPriority, lowestPriority));

        assertEquals("9999", environment.require("server.port"));
        assertEquals("localhost", environment.require("server.host"));
    }

    @Test
    void aKeyMissingFromEveryHigherPrioritySourceFallsThroughToTheNextOne() {
        var systemLevel = sourceOf(Map.of());
        var envLevel = sourceOf(Map.of());
        var fileLevel = sourceOf(Map.of("app.name", "vessel"));
        var environment = new Environment(List.of(systemLevel, envLevel, fileLevel));

        assertEquals("vessel", environment.require("app.name"));
    }

    @Test
    void realSystemPropertiesActuallyOverrideTheFileSourceThroughConfigurationLoad() {
        System.setProperty("vessel.config.priority.test", "from-system-property");
        try {
            var environment = Configuration.load("test-config.properties");

            assertEquals("from-system-property", environment.require("vessel.config.priority.test"));
        } finally {
            System.clearProperty("vessel.config.priority.test");
        }
    }

    @Test
    void realEnvironmentVariablesAreReadUsingTheRelaxedUppercaseUnderscoreConvention() {
        var realPathValue = System.getenv("PATH");
        var environment = Configuration.load("nonexistent-for-this-test.properties");

        if (realPathValue != null) {
            assertEquals(realPathValue, environment.require("path"));
        } else {
            assertTrue(environment.get("path").isEmpty());
        }
    }
}
