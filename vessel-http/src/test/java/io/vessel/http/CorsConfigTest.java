package io.vessel.http;

import io.vessel.http.annotation.Server;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CorsConfigTest {

    @Server(enableCors = true, corsOrigin = "http://localhost:3000")
    static class Annotated {
    }

    @Test
    void extractsTheOriginFromAServerAnnotationInstance() {
        var server = Annotated.class.getAnnotation(Server.class);

        var corsConfig = CorsConfig.from(server);

        assertEquals("http://localhost:3000", corsConfig.allowedOrigin());
    }

    @Test
    void rejectsANullOrigin() {
        assertThrows(NullPointerException.class, () -> new CorsConfig(null));
    }
}
