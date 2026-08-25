package io.vessel.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvironmentTypeConversionTest {

    enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }

    private Environment environmentOf(Map<String, String> values) {
        return new Environment(List.of(key -> Optional.ofNullable(values.get(key))));
    }

    @Test
    void convertsEachSupportedTypeFromItsRawStringForm() {
        var values = new LinkedHashMap<String, String>();
        values.put("port", "8080");
        values.put("maxConnections", "100000");
        values.put("loadFactor", "0.75");
        values.put("debug", "true");
        values.put("timeout", "30s");
        values.put("level", "warn");
        var environment = environmentOf(values);

        assertEquals(8080, environment.get("port", Integer.class));
        assertEquals(100000L, environment.get("maxConnections", Long.class));
        assertEquals(0.75, environment.get("loadFactor", Double.class));
        assertEquals(Boolean.TRUE, environment.get("debug", Boolean.class));
        assertEquals(Duration.ofSeconds(30), environment.get("timeout", Duration.class));
        assertEquals(LogLevel.WARN, environment.get("level", LogLevel.class));
    }

    @Test
    void durationAcceptsEveryUnitSuffixAndDefaultsToMillisecondsWithNone() {
        var values = new LinkedHashMap<String, String>();
        values.put("a", "500ns");
        values.put("b", "10us");
        values.put("c", "250ms");
        values.put("d", "45m");
        values.put("e", "2h");
        values.put("f", "1d");
        values.put("g", "500");
        var environment = environmentOf(values);

        assertEquals(Duration.ofNanos(500), environment.get("a", Duration.class));
        assertEquals(Duration.ofNanos(10_000), environment.get("b", Duration.class));
        assertEquals(Duration.ofMillis(250), environment.get("c", Duration.class));
        assertEquals(Duration.ofMinutes(45), environment.get("d", Duration.class));
        assertEquals(Duration.ofHours(2), environment.get("e", Duration.class));
        assertEquals(Duration.ofDays(1), environment.get("f", Duration.class));
        assertEquals(Duration.ofMillis(500), environment.get("g", Duration.class));
    }

    @Test
    void getWithDefaultOnlyAppliesTheDefaultWhenTheKeyIsAbsent() {
        var environment = environmentOf(Map.of("port", "9090"));

        assertEquals(9090, environment.get("port", Integer.class, 1234));
        assertEquals(1234, environment.get("missing.port", Integer.class, 1234));
    }

    @Test
    void rejectsANonNumericValueForANumericType() {
        var environment = environmentOf(Map.of("port", "not-a-number"));

        var exception = assertThrows(VesselConfigException.class, () -> environment.get("port", Integer.class));

        assertTrue(exception.getMessage().contains("property 'port'"));
        assertTrue(exception.getMessage().contains("'not-a-number'"));
        assertTrue(exception.getMessage().contains("Integer"));
    }

    @Test
    void rejectsABooleanValueThatIsNeitherTrueNorFalse() {
        var environment = environmentOf(Map.of("debug", "yes"));

        var exception = assertThrows(VesselConfigException.class, () -> environment.get("debug", Boolean.class));

        assertTrue(exception.getMessage().contains("'yes'"));
        assertTrue(exception.getMessage().contains("true' or 'false"));
    }

    @Test
    void rejectsAMalformedDurationString() {
        var environment = environmentOf(Map.of("timeout", "thirty seconds"));

        var exception = assertThrows(VesselConfigException.class, () -> environment.get("timeout", Duration.class));

        assertTrue(exception.getMessage().contains("'thirty seconds'"));
        assertTrue(exception.getMessage().contains("not a valid duration"));
    }

    @Test
    void rejectsAnEnumValueThatIsNotAConstantAndListsTheValidOnes() {
        var environment = environmentOf(Map.of("level", "VERBOSE"));

        var exception = assertThrows(VesselConfigException.class, () -> environment.get("level", LogLevel.class));

        assertTrue(exception.getMessage().contains("'VERBOSE'"));
        assertTrue(exception.getMessage().contains("DEBUG"));
        assertTrue(exception.getMessage().contains("INFO"));
        assertTrue(exception.getMessage().contains("WARN"));
        assertTrue(exception.getMessage().contains("ERROR"));
    }

    @Test
    void rejectsAnUnsupportedTargetType() {
        var environment = environmentOf(Map.of("value", "42"));

        var exception = assertThrows(VesselConfigException.class, () -> environment.get("value", Object.class));

        assertTrue(exception.getMessage().contains("unsupported type 'Object'"));
    }
}
