package io.vessel.http;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse.BodyHandlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VesselHttpServerRouterTest {

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void extractsPathVariablesAndPassesThemToTheHandler() throws IOException, InterruptedException {
        var server = VesselHttpServer.create(0)
                .get("/users/{id}", request -> HttpResponse.ok("user " + request.pathVariable("id").orElseThrow()));
        server.start();
        try {
            var response = get(server, "/users/42");

            assertEquals(200, response.statusCode());
            assertEquals("user 42", response.body());
        } finally {
            server.stop();
        }
    }

    @Test
    void prefersAnExactRouteOverAParameterizedOneForTheSameRequest() throws IOException, InterruptedException {
        var server = VesselHttpServer.create(0)
                .get("/users/{id}", request -> HttpResponse.ok("by id"))
                .get("/users/me", request -> HttpResponse.ok("me"));
        server.start();
        try {
            var response = get(server, "/users/me");

            assertEquals("me", response.body());
        } finally {
            server.stop();
        }
    }

    @Test
    void returns405WithAnAllowHeaderWhenThePathExistsForAnotherMethod() throws IOException, InterruptedException {
        var server = VesselHttpServer.create(0)
                .get("/users", request -> HttpResponse.ok("list"))
                .post("/users", request -> HttpResponse.created("created"));
        server.start();
        try {
            var httpRequest = java.net.http.HttpRequest.newBuilder(
                            URI.create("http://localhost:" + server.port() + "/users"))
                    .method("DELETE", java.net.http.HttpRequest.BodyPublishers.noBody())
                    .build();

            var response = client.send(httpRequest, BodyHandlers.ofString());

            assertEquals(405, response.statusCode());
            var allowHeader = response.headers().firstValue("Allow").orElseThrow();
            assertTrue(allowHeader.contains("GET"));
            assertTrue(allowHeader.contains("POST"));
        } finally {
            server.stop();
        }
    }

    @Test
    void returns404WithARichBodyWhenNoRouteMatchesAtAll() throws IOException, InterruptedException {
        var server = VesselHttpServer.create(0)
                .get("/api/users", request -> HttpResponse.ok("list"))
                .get("/api/users/{id}", request -> HttpResponse.ok("one"));
        server.start();
        try {
            var response = get(server, "/api/users/42/orders");

            assertEquals(404, response.statusCode());
            assertTrue(response.body().contains("routes registered for /api:"));
            assertTrue(response.body().contains("did you mean: GET /api/users/{id} ?"));
        } finally {
            server.stop();
        }
    }

    private java.net.http.HttpResponse<String> get(VesselHttpServer server, String path)
            throws IOException, InterruptedException {
        var httpRequest = java.net.http.HttpRequest.newBuilder(URI.create("http://localhost:" + server.port() + path))
                .GET()
                .build();
        return client.send(httpRequest, BodyHandlers.ofString());
    }
}
