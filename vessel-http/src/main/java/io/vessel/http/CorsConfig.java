package io.vessel.http;

import io.vessel.http.annotation.Server;

import java.util.Objects;

/**
 * Minimal CORS configuration — just the allowed origin. Allowed methods and
 * headers are fixed by {@link VesselHttpServer}'s preflight handling rather
 * than made configurable here; a deliberately small surface, not a
 * full-featured CORS implementation.
 */
public record CorsConfig(String allowedOrigin) {

    public CorsConfig {
        Objects.requireNonNull(allowedOrigin, "allowedOrigin must not be null");
    }

    /** Extracts CORS configuration from an already-obtained {@code @Server} instance. */
    public static CorsConfig from(Server server) {
        Objects.requireNonNull(server, "server must not be null");
        return new CorsConfig(server.corsOrigin());
    }
}
