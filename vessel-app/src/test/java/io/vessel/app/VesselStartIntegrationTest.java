package io.vessel.app;

import io.vessel.app.fixtures.TestApplication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end tests against a server actually started through {@code
 * Vessel.start()} — real HTTP client, real random port, exercising the
 * whole chain: {@code @Server} → {@code Configuration} → {@code Container}
 * → component scan → controller scan → eager graph validation → bound
 * socket — the full-stack, real-request coverage that unit tests in the
 * other modules deliberately don't attempt.
 */
class VesselStartIntegrationTest {

    private final HttpClient client = HttpClient.newHttpClient();
    private io.vessel.http.VesselHttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void startBootsAWorkingServerOnAnEphemeralPortWithComponentAndControllerScanWired() throws IOException, InterruptedException {
        server = Vessel.start(TestApplication.class);

        assertTrue(server.port() > 0);

        var response = get("/greeting");

        assertEquals(200, response.statusCode());
        assertEquals("\"Hello, Vessel Test App!\"", response.body());
    }

    @Test
    void environmentItselfIsInjectableAsAnOrdinaryConstructorDependencyNotJustThroughAtValue() throws IOException, InterruptedException {
        server = Vessel.start(TestApplication.class);

        var response = get("/config/app-name");

        assertEquals(200, response.statusCode());
        assertEquals("\"Vessel Test App\"", response.body());
    }

    @Test
    void constructorInjectionAndAtValueResolveTogetherInTheSameConstructor() throws IOException, InterruptedException {
        server = Vessel.start(TestApplication.class);

        // AppInfo's constructor mixes a normal @Inject-resolved bean (GreetingService)
        // with two @Value-resolved parameters (name, retries) — /greeting only exercises
        // the String one, so this call proves the int one (retries) converted correctly too,
        // by way of the controller not blowing up building AppInfo in the first place.
        var response = get("/greeting");

        assertEquals(200, response.statusCode());
    }

    @Test
    void anExceptionHandlerRegisteredThroughControllerScanStillProducesAStandardizedErrorBody() throws IOException, InterruptedException {
        server = Vessel.start(TestApplication.class);

        var response = get("/demo/99");

        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("demo item 99 not found"));
    }

    @Test
    void aNormalRouteStillWorksAlongsideOneThatThrows() throws IOException, InterruptedException {
        server = Vessel.start(TestApplication.class);

        var response = get("/demo/7");

        assertEquals(200, response.statusCode());
        assertEquals("\"demo item 7\"", response.body());
    }

    @Test
    void corsFromAtServerIsWiredUpAsARealPreflightResponse() throws IOException, InterruptedException {
        server = Vessel.start(TestApplication.class);

        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + server.port() + "/greeting"))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .build();
        var response = client.send(request, BodyHandlers.ofString());

        assertEquals("http://localhost:3000", response.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());
    }

    @Test
    void aSystemPropertyOverridesTheValueFromApplicationPropertiesEndToEnd() throws IOException, InterruptedException {
        System.setProperty("app.name", "Overridden Name");
        try {
            server = Vessel.start(TestApplication.class);

            var response = get("/greeting");

            assertEquals("\"Hello, Overridden Name!\"", response.body());
        } finally {
            System.clearProperty("app.name");
        }
    }

    private java.net.http.HttpResponse<String> get(String path) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + server.port() + path)).GET().build();
        return client.send(request, BodyHandlers.ofString());
    }
}
