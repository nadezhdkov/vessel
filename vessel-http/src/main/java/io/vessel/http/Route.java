package io.vessel.http;

import java.util.Objects;

/**
 * A single routing entry: which method and path pattern invoke which
 * handler. {@code path} may contain {@code {name}} segments — see
 * {@link Router} for how those are matched and extracted.
 */
public record Route(HttpMethod method, String path, Handler handler) {

    public Route {
        Objects.requireNonNull(method, "method must not be null");
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(handler, "handler must not be null");
        if (!path.startsWith("/")) {
            throw new VesselHttpException(
                    "route path must start with '/': '%s'".formatted(path));
        }
    }

    boolean hasPathVariables() {
        return path.indexOf('{') >= 0;
    }
}
