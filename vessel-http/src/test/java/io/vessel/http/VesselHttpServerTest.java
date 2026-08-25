package io.vessel.http;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VesselHttpServerTest {

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void respondsToARegisteredGetRoute() throws IOException, InterruptedException {
        var server = VesselHttpServer.create(0).get("/hello", request -> HttpResponse.ok("Hello World"));
        server.start();
        try {
            var response = get(server, "/hello");

            assertEquals(200, response.statusCode());
            assertEquals("Hello World", response.body());
        } finally {
            server.stop();
        }
    }

    @Test
    void returns404WithAnInformativeBodyForAnUnregisteredPath() throws IOException, InterruptedException {
        var server = VesselHttpServer.create(0);
        server.start();
        try {
            var response = get(server, "/nope");

            assertEquals(404, response.statusCode());
            assertTrue(response.body().contains("GET"));
            assertTrue(response.body().contains("/nope"));
        } finally {
            server.stop();
        }
    }

    @Test
    void passesQueryParamsHeadersAndBodyThroughToTheHandler() throws IOException, InterruptedException {
        var server = VesselHttpServer.create(0).post("/echo", request -> HttpResponse.ok(
                request.queryParam("name").orElse("?") + ":" + request.header("X-Trace").orElse("?") + ":" + request.body()));
        server.start();
        try {
            var httpRequest = java.net.http.HttpRequest.newBuilder(
                            URI.create("http://localhost:" + server.port() + "/echo?name=Alice"))
                    .header("X-Trace", "abc123")
                    .POST(BodyPublishers.ofString("payload"))
                    .build();

            var response = client.send(httpRequest, BodyHandlers.ofString());

            assertEquals("Alice:abc123:payload", response.body());
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
