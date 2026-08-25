package io.vessel.http;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves {@code withVirtualThreads()} actually changes dispatch behavior —
 * not just that it doesn't crash. Two requests that each sleep 300ms:
 * handled sequentially (the JDK default), the pair takes ~600ms; handled one
 * virtual thread per exchange, the pair takes ~300ms, since both are in
 * flight at once.
 */
class VirtualThreadsConcurrencyTest {

    private static final Duration HANDLER_SLEEP = Duration.ofMillis(300);

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void withoutVirtualThreadsTwoRequestsAreHandledOneAfterTheOther() throws Exception {
        var server = VesselHttpServer.create(0).get("/slow", this::slowHandler);
        server.start();
        try {
            long elapsedMillis = timeTwoConcurrentRequests(server);

            assertTrue(elapsedMillis >= HANDLER_SLEEP.toMillis() * 2,
                    "expected sequential dispatch to take at least %dms, took %dms"
                            .formatted(HANDLER_SLEEP.toMillis() * 2, elapsedMillis));
        } finally {
            server.stop();
        }
    }

    @Test
    void withVirtualThreadsTwoRequestsAreHandledConcurrently() throws Exception {
        var server = VesselHttpServer.create(0).get("/slow", this::slowHandler).withVirtualThreads();
        server.start();
        try {
            long elapsedMillis = timeTwoConcurrentRequests(server);

            assertTrue(elapsedMillis < HANDLER_SLEEP.toMillis() * 2,
                    "expected concurrent dispatch to take less than %dms, took %dms"
                            .formatted(HANDLER_SLEEP.toMillis() * 2, elapsedMillis));
        } finally {
            server.stop();
        }
    }

    private HttpResponse slowHandler(io.vessel.http.HttpRequest request) {
        try {
            Thread.sleep(HANDLER_SLEEP);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return HttpResponse.ok("done");
    }

    private long timeTwoConcurrentRequests(VesselHttpServer server) throws InterruptedException, ExecutionException {
        try (var executor = Executors.newFixedThreadPool(2)) {
            long start = System.nanoTime();
            var futures = List.of(
                    executor.submit(() -> get(server, "/slow")),
                    executor.submit(() -> get(server, "/slow")));
            for (var future : futures) {
                future.get();
            }
            return (System.nanoTime() - start) / 1_000_000;
        }
    }

    private java.net.http.HttpResponse<String> get(VesselHttpServer server, String path)
            throws IOException, InterruptedException {
        var request = java.net.http.HttpRequest.newBuilder(URI.create("http://localhost:" + server.port() + path)).GET().build();
        return client.send(request, BodyHandlers.ofString());
    }
}
