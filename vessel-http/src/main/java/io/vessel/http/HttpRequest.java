package io.vessel.http;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Immutable inbound HTTP request. {@code pathVariables} is populated by
 * {@link Router#match(HttpMethod, String)} via {@link #withPathVariables},
 * after the request has already been built from the wire — a request is
 * fully readable before routing ever runs, path variables are just attached
 * once known.
 */
public final class HttpRequest {

    private final HttpMethod method;
    private final String path;
    private final Map<String, String> headers;
    private final Map<String, String> queryParams;
    private final Map<String, String> pathVariables;
    private final String body;

    public HttpRequest(HttpMethod method, String path, Map<String, String> headers,
                        Map<String, String> queryParams, Map<String, String> pathVariables, String body) {
        this.method = Objects.requireNonNull(method, "method must not be null");
        this.path = Objects.requireNonNull(path, "path must not be null");
        this.headers = caseInsensitive(headers);
        this.queryParams = Map.copyOf(queryParams);
        this.pathVariables = Map.copyOf(pathVariables);
        this.body = body == null ? "" : body;
    }

    public HttpMethod method() {
        return method;
    }

    public String path() {
        return path;
    }

    public Map<String, String> headers() {
        return headers;
    }

    /** Header lookup is case-insensitive, per the HTTP spec. */
    public Optional<String> header(String name) {
        return Optional.ofNullable(headers.get(name));
    }

    public Map<String, String> queryParams() {
        return queryParams;
    }

    public Optional<String> queryParam(String name) {
        return Optional.ofNullable(queryParams.get(name));
    }

    public Map<String, String> pathVariables() {
        return pathVariables;
    }

    public Optional<String> pathVariable(String name) {
        return Optional.ofNullable(pathVariables.get(name));
    }

    public String body() {
        return body;
    }

    /** Returns a copy of this request with the given path variables attached. */
    public HttpRequest withPathVariables(Map<String, String> pathVariables) {
        return new HttpRequest(method, path, headers, queryParams, pathVariables, body);
    }

    private static Map<String, String> caseInsensitive(Map<String, String> source) {
        var map = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
        map.putAll(source);
        return Collections.unmodifiableSortedMap(map);
    }
}
