package io.vessel.http;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpMethodTest {

    @Test
    void parsesAKnownMethodCaseInsensitively() {
        assertEquals(HttpMethod.GET, HttpMethod.fromString("get"));
        assertEquals(HttpMethod.POST, HttpMethod.fromString("POST"));
    }

    @Test
    void rejectsAnUnknownMethodName() {
        var exception = assertThrows(VesselHttpException.class, () -> HttpMethod.fromString("TRACE"));

        assertTrue(exception.getMessage().contains("unknown HTTP method: 'TRACE'"));
        assertTrue(exception.getMessage().contains("GET"));
    }

    @Test
    void rejectsANullOrBlankMethodName() {
        var exception = assertThrows(VesselHttpException.class, () -> HttpMethod.fromString("  "));

        assertTrue(exception.getMessage().contains("HTTP method must not be null or blank"));
    }
}
