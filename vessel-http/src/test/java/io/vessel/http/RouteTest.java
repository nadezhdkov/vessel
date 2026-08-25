package io.vessel.http;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteTest {

    @Test
    void rejectsAPathThatDoesNotStartWithASlash() {
        var exception = assertThrows(VesselHttpException.class,
                () -> new Route(HttpMethod.GET, "users", request -> HttpResponse.ok("hi")));

        assertTrue(exception.getMessage().contains("route path must start with '/': 'users'"));
    }

    @Test
    void rejectsANullMethod() {
        assertThrows(NullPointerException.class,
                () -> new Route(null, "/users", request -> HttpResponse.ok("hi")));
    }

    @Test
    void rejectsANullHandler() {
        assertThrows(NullPointerException.class,
                () -> new Route(HttpMethod.GET, "/users", null));
    }
}
