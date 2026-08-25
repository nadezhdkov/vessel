package io.vessel.http.annotation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ServerAnnotationTest {

    @Server
    static class WithDefaults {
    }

    @Server(port = 9000, host = "0.0.0.0", enableCors = true, corsOrigin = "http://localhost:3000")
    static class WithCustomValues {
    }

    @Test
    void defaultsMatchThePrdBaseline() {
        var server = WithDefaults.class.getAnnotation(Server.class);

        assertEquals(8080, server.port());
        assertEquals("localhost", server.host());
        assertFalse(server.enableCors());
        assertEquals("*", server.corsOrigin());
    }

    @Test
    void readsCustomValuesDeclaredOnTheAnnotation() {
        var server = WithCustomValues.class.getAnnotation(Server.class);

        assertEquals(9000, server.port());
        assertEquals("0.0.0.0", server.host());
        assertEquals("http://localhost:3000", server.corsOrigin());
    }
}
