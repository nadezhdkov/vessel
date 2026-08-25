package io.vessel.http;

import java.util.Map;
import java.util.Set;

/**
 * The outcome of {@link Router#match(HttpMethod, String)}. None of these
 * are exceptions — a request not matching a route is an expected, everyday
 * HTTP outcome (404/405), not a program bug, so it's modeled as data the
 * caller turns into a response, not a thrown error.
 */
public sealed interface RouteResult {

    record Matched(Handler handler, Map<String, String> pathVariables) implements RouteResult {
    }

    record MethodNotAllowed(Set<HttpMethod> allowedMethods) implements RouteResult {
    }

    record NotFound(String message) implements RouteResult {
    }
}
