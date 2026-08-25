package io.vessel.web;

import io.vessel.core.Container;
import io.vessel.http.VesselHttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControllerScannerDispatcherIntegrationTest {

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void aGetMethodReturningAPlainListIsSerializedAsAJsonArray() throws IOException, InterruptedException {
        var server = bootServer();
        try {
            var response = get(server, "/users");

            assertEquals(200, response.statusCode());
            assertEquals("application/json; charset=utf-8", response.headers().firstValue("Content-Type").orElseThrow());
            assertEquals("[{\"id\":1,\"name\":\"Alice\"},{\"id\":2,\"name\":\"Bob\"}]", response.body());
        } finally {
            server.stop();
        }
    }

    @Test
    void aPathVariableIsExtractedConvertedAndPassedToTheMethod() throws IOException, InterruptedException {
        var server = bootServer();
        try {
            var response = get(server, "/users/42");

            assertEquals(200, response.statusCode());
            assertEquals("{\"id\":42,\"name\":\"Alice\"}", response.body());
        } finally {
            server.stop();
        }
    }

    @Test
    void aResponseTReturnValueControlsItsOwnStatusEvenWithoutResponseStatus() throws IOException, InterruptedException {
        var server = bootServer();
        try {
            var response = get(server, "/users/99");

            assertEquals(404, response.statusCode());
        } finally {
            server.stop();
        }
    }

    @Test
    void aRequestBodyIsDeserializedAndResponseStatusSetsTheStatusCode() throws IOException, InterruptedException {
        var server = bootServer();
        try {
            var httpRequest = HttpRequest.newBuilder(URI.create("http://localhost:" + server.port() + "/users"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"id\":7,\"name\":\"Carol\"}"))
                    .build();

            var response = client.send(httpRequest, BodyHandlers.ofString());

            assertEquals(201, response.statusCode());
            assertEquals("{\"id\":7,\"name\":\"Carol\"}", response.body());
        } finally {
            server.stop();
        }
    }

    @Test
    void aSpecificExceptionHandlerProducesAStandardizedErrorBody() throws IOException, InterruptedException {
        var server = bootServer();
        try {
            var response = get(server, "/users/notfound-demo");

            assertEquals(404, response.statusCode());
            assertTrue(response.body().contains("\"status\":404"));
            assertTrue(response.body().contains("\"error\":\"USER_NOT_FOUND\""));
            assertTrue(response.body().contains("\"message\":\"user 42 not found\""));
            assertTrue(response.body().contains("\"timestamp\":"));
        } finally {
            server.stop();
        }
    }

    @Test
    void theMostSpecificHandlerWinsOverAMoreGenericOneOnTheSameController() throws IOException, InterruptedException {
        var server = bootServer();
        try {
            var response = get(server, "/users/broken");

            assertEquals(500, response.statusCode());
            assertTrue(response.body().contains("\"error\":\"ILLEGAL_STATE\""));
            assertTrue(response.body().contains("\"message\":\"boom\""));
        } finally {
            server.stop();
        }
    }

    @Test
    void theGenericHandlerCatchesWhateverNoSpecificHandlerMatches() throws IOException, InterruptedException {
        var server = bootServer();
        try {
            var response = get(server, "/users/generic-error");

            assertEquals(500, response.statusCode());
            assertTrue(response.body().contains("\"error\":\"INTERNAL_ERROR\""));
            // deliberately generic — the real exception message ("something else entirely") never leaks
            assertTrue(response.body().contains("\"message\":\"Unexpected error\""));

            // the server must still be able to answer another request afterwards
            var followUp = get(server, "/users");
            assertEquals(200, followUp.statusCode());
        } finally {
            server.stop();
        }
    }

    @Test
    void aControllerWithNoExceptionHandlersStillFallsBackToTheBareSafetyNet() throws IOException, InterruptedException {
        var container = new Container();
        var server = VesselHttpServer.create(0);
        ControllerScanner.scan("io.vessel.web.errorfixtures.bare", container, server);
        server.start();
        try {
            var response = get(server, "/bare/explode");

            assertEquals(500, response.statusCode());
            assertTrue(response.body().contains("no handler catches this"));
        } finally {
            server.stop();
        }
    }

    @Test
    void aBrokenExceptionHandlerDoesNotCrashTheExchangeEither() throws IOException, InterruptedException {
        var container = new Container();
        var server = VesselHttpServer.create(0);
        ControllerScanner.scan("io.vessel.web.errorfixtures.brokenhandler", container, server);
        server.start();
        try {
            var response = get(server, "/broken-handler/explode");

            assertEquals(500, response.statusCode());
            assertTrue(response.body().contains("the handler itself is broken"));
        } finally {
            server.stop();
        }
    }

    @Test
    void twoExceptionHandlersForTheSameTypeFailAtScanTimeNotAtRequestTime() {
        var container = new Container();
        var server = VesselHttpServer.create(0);

        var exception = assertThrows(VesselWebException.class,
                () -> ControllerScanner.scan("io.vessel.web.errorfixtures.duplicatehandler", container, server));

        assertTrue(exception.getMessage().contains("duplicate @ExceptionHandler for type 'IllegalStateException'"));
    }

    private VesselHttpServer bootServer() {
        var container = new Container();
        var server = VesselHttpServer.create(0);
        ControllerScanner.scan("io.vessel.web.fixtures", container, server);
        server.start();
        return server;
    }

    private java.net.http.HttpResponse<String> get(VesselHttpServer server, String path)
            throws IOException, InterruptedException {
        var httpRequest = HttpRequest.newBuilder(URI.create("http://localhost:" + server.port() + path)).GET().build();
        return client.send(httpRequest, BodyHandlers.ofString());
    }
}
