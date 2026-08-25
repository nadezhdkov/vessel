package io.vessel.http;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse.BodyHandlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorsTest {

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void addsAllowOriginHeaderToActualResponsesWhenCorsIsEnabled() throws IOException, InterruptedException {
        var server = VesselHttpServer.create(0)
                .get("/hello", request -> HttpResponse.ok("hi"))
                .enableCors(new CorsConfig("http://localhost:3000"));
        server.start();
        try {
            var request = java.net.http.HttpRequest.newBuilder(
                            URI.create("http://localhost:" + server.port() + "/hello"))
                    .GET()
                    .build();

            var response = client.send(request, BodyHandlers.ofString());

            assertEquals("http://localhost:3000", response.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());
        } finally {
            server.stop();
        }
    }

    @Test
    void respondsToAPreflightOptionsRequestWithCorsHeadersAndNoContent() throws IOException, InterruptedException {
        var server = VesselHttpServer.create(0)
                .get("/hello", request -> HttpResponse.ok("hi"))
                .enableCors(new CorsConfig("http://localhost:3000"));
        server.start();
        try {
            var request = java.net.http.HttpRequest.newBuilder(
                            URI.create("http://localhost:" + server.port() + "/hello"))
                    .method("OPTIONS", java.net.http.HttpRequest.BodyPublishers.noBody())
                    .build();

            var response = client.send(request, BodyHandlers.ofString());

            assertEquals(204, response.statusCode());
            assertEquals("http://localhost:3000", response.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());
            assertTrue(response.headers().firstValue("Access-Control-Allow-Methods").orElseThrow().contains("GET"));
        } finally {
            server.stop();
        }
    }

    @Test
    void doesNotAddCorsHeadersWhenCorsIsNotEnabled() throws IOException, InterruptedException {
        var server = VesselHttpServer.create(0).get("/hello", request -> HttpResponse.ok("hi"));
        server.start();
        try {
            var request = java.net.http.HttpRequest.newBuilder(
                            URI.create("http://localhost:" + server.port() + "/hello"))
                    .GET()
                    .build();

            var response = client.send(request, BodyHandlers.ofString());

            assertTrue(response.headers().firstValue("Access-Control-Allow-Origin").isEmpty());
        } finally {
            server.stop();
        }
    }
}
