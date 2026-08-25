package io.vessel.web;

import io.vessel.core.Container;
import io.vessel.http.VesselHttpServer;
import io.vessel.web.middlewarefixtures.basic.BasicMiddlewareController;
import io.vessel.web.middlewarefixtures.shortcircuit.ShortCircuitController;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiddlewareIntegrationTest {

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void beforeRunsAheadOfTheRouteMethodAndAfterCanAddAResponseHeader() throws IOException, InterruptedException {
        BasicMiddlewareController.beforeCallCount.set(0);
        BasicMiddlewareController.afterCallCount.set(0);
        var server = bootServer("io.vessel.web.middlewarefixtures.basic");
        try {
            var response = get(server, "/basic/hello");

            assertEquals(200, response.statusCode());
            assertEquals("\"hello\"", response.body());
            assertEquals(1, BasicMiddlewareController.beforeCallCount.get());
            assertEquals(1, BasicMiddlewareController.afterCallCount.get());
            assertEquals("ran", response.headers().firstValue("X-After").orElseThrow());
        } finally {
            server.stop();
        }
    }

    @Test
    void aBeforeThatReturnsAResponseShortCircuitsBothTheRouteMethodAndAfter() throws IOException, InterruptedException {
        ShortCircuitController.routeCallCount.set(0);
        ShortCircuitController.afterCallCount.set(0);
        var server = bootServer("io.vessel.web.middlewarefixtures.shortcircuit");
        try {
            var response = get(server, "/protected/resource");

            assertEquals(401, response.statusCode());
            assertEquals("no token provided", response.body());
            assertEquals(0, ShortCircuitController.routeCallCount.get());
            assertEquals(0, ShortCircuitController.afterCallCount.get());
        } finally {
            server.stop();
        }
    }

    @Test
    void aBrokenBeforeProducesA500WithoutCrashingTheServer() throws IOException, InterruptedException {
        var container = new Container();
        var server = VesselHttpServer.create(0);
        ControllerScanner.scan("io.vessel.web.middlewarefixtures.broken", container, server);
        server.start();
        try {
            var response = get(server, "/broken-before/resource");

            assertEquals(500, response.statusCode());
            assertTrue(response.body().contains("before middleware exploded"));

            var followUp = get(server, "/broken-after/resource");
            assertEquals(500, followUp.statusCode());
        } finally {
            server.stop();
        }
    }

    @Test
    void aBrokenAfterAlsoProducesA500WithoutCrashingTheServer() throws IOException, InterruptedException {
        var container = new Container();
        var server = VesselHttpServer.create(0);
        ControllerScanner.scan("io.vessel.web.middlewarefixtures.broken", container, server);
        server.start();
        try {
            var response = get(server, "/broken-after/resource");

            assertEquals(500, response.statusCode());
            assertTrue(response.body().contains("after middleware exploded"));

            // the server survives — a normal endpoint elsewhere still answers fine
            var followUp = get(server, "/broken-before/resource");
            assertEquals(500, followUp.statusCode());
        } finally {
            server.stop();
        }
    }

    @Test
    void twoAtBeforeMethodsOnTheSameControllerFailAtScanTimeNotAtRequestTime() {
        var container = new Container();
        var server = VesselHttpServer.create(0);

        var exception = assertThrows(VesselWebException.class,
                () -> ControllerScanner.scan("io.vessel.web.errorfixtures.duplicatebefore", container, server));

        assertTrue(exception.getMessage().contains("duplicate @Before"));
    }

    private VesselHttpServer bootServer(String basePackage) {
        var container = new Container();
        var server = VesselHttpServer.create(0);
        ControllerScanner.scan(basePackage, container, server);
        server.start();
        return server;
    }

    private java.net.http.HttpResponse<String> get(VesselHttpServer server, String path)
            throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + server.port() + path)).GET().build();
        return client.send(request, BodyHandlers.ofString());
    }
}
