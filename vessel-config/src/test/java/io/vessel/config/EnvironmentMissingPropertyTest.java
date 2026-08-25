package io.vessel.config;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvironmentMissingPropertyTest {

    private final Environment empty = new Environment(List.of(key -> Optional.empty()));

    @Test
    void getReturnsAnEmptyOptionalForAKeyNoSourceHas() {
        assertTrue(empty.get("does.not.exist").isEmpty());
    }

    @Test
    void getWithADefaultValueReturnsItWhenTheKeyIsAbsent() {
        assertEquals("fallback", empty.get("does.not.exist", "fallback"));
    }

    @Test
    void requireNamesTheMissingKeyAndTheSourcesItChecked() {
        var exception = assertThrows(VesselConfigException.class, () -> empty.require("server.port"));

        assertTrue(exception.getMessage().contains("'server.port'"));
        assertTrue(exception.getMessage().contains("system properties"));
        assertTrue(exception.getMessage().contains("environment variables"));
        assertTrue(exception.getMessage().contains("properties file"));
    }

    @Test
    void getWithATypeAndNoDefaultAlsoThrowsWhenTheKeyIsAbsent() {
        var exception = assertThrows(VesselConfigException.class, () -> empty.get("server.port", Integer.class));

        assertTrue(exception.getMessage().contains("'server.port'"));
    }
}
