package io.vessel.examples;

import io.vessel.app.Vessel;
import io.vessel.http.VesselHttpServer;
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
 * Proves the example app the README points to is actually functional, not
 * just compiling — starts it for real (on an ephemeral port, via the
 * "server.port" system property override, so this never fights a real
 * instance someone might have running on 8080) and exercises every route.
 */
class HelloApplicationSmokeTest {

    private final HttpClient client = HttpClient.newHttpClient();
    private VesselHttpServer server;

    @AfterEach
    void stopServer() {
        System.clearProperty("server.port");
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void bootsAndServesEveryRouteFromTheExampleApp() throws IOException, InterruptedException {
        System.setProperty("server.port", "0");
        server = Vessel.start(HelloApplication.class);

        var users = get("/users");
        assertEquals(200, users.statusCode());
        assertTrue(users.body().contains("Alice"));
        assertTrue(users.body().contains("Bob"));

        var alice = get("/users/1");
        assertEquals(200, alice.statusCode());
        assertEquals("{\"id\":1,\"name\":\"Alice\",\"email\":\"alice@example.com\"}", alice.body());

        var missing = get("/users/99");
        assertEquals(404, missing.statusCode());
        assertTrue(missing.body().contains("USER_NOT_FOUND"));
        assertTrue(missing.body().contains("User 99 not found"));

        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + server.port() + "/users"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"id\":3,\"name\":\"Charlie\",\"email\":\"charlie@example.com\"}"))
                .build();
        var created = client.send(request, BodyHandlers.ofString());
        assertEquals(201, created.statusCode());
        assertEquals("{\"id\":3,\"name\":\"Charlie\",\"email\":\"charlie@example.com\"}", created.body());

        var usersAfterCreate = get("/users");
        assertTrue(usersAfterCreate.body().contains("Charlie"));
    }

    private java.net.http.HttpResponse<String> get(String path) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + server.port() + path)).GET().build();
        return client.send(request, BodyHandlers.ofString());
    }
}
