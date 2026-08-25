package io.vessel.http;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Immutable outbound HTTP response. Headers are stored case-insensitively —
 * {@code withHeader("Content-Type", ...)} and a later
 * {@code withHeader("content-type", ...)} overwrite the same entry, per the
 * HTTP spec.
 */
public final class HttpResponse {

    private static final byte[] EMPTY_BODY = new byte[0];

    private final HttpStatus status;
    private final Map<String, String> headers;
    private final byte[] body;

    private HttpResponse(HttpStatus status, Map<String, String> headers, byte[] body) {
        this.status = status;
        this.headers = headers;
        this.body = body;
    }

    public static HttpResponse ok(String body) {
        return status(HttpStatus.OK, body);
    }

    public static HttpResponse ok(byte[] body) {
        return status(HttpStatus.OK, body);
    }

    public static HttpResponse created(String body) {
        return status(HttpStatus.CREATED, body);
    }

    public static HttpResponse noContent() {
        return status(HttpStatus.NO_CONTENT, EMPTY_BODY);
    }

    public static HttpResponse status(HttpStatus status, String body) {
        Objects.requireNonNull(status, "status must not be null");
        var bytes = body == null ? EMPTY_BODY : body.getBytes(StandardCharsets.UTF_8);
        return status(status, bytes).withHeader("Content-Type", "text/plain; charset=utf-8");
    }

    public static HttpResponse status(HttpStatus status, byte[] body) {
        Objects.requireNonNull(status, "status must not be null");
        return new HttpResponse(status, emptyHeaders(), body == null ? EMPTY_BODY : body);
    }

    public HttpStatus status() {
        return status;
    }

    public Map<String, String> headers() {
        return headers;
    }

    public byte[] body() {
        return body;
    }

    /** Returns a copy of this response with the given header added or overwritten. */
    public HttpResponse withHeader(String name, String value) {
        Objects.requireNonNull(name, "header name must not be null");
        Objects.requireNonNull(value, "header value must not be null");
        var merged = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
        merged.putAll(headers);
        merged.put(name, value);
        return new HttpResponse(status, Collections.unmodifiableSortedMap(merged), body);
    }

    private static Map<String, String> emptyHeaders() {
        return Collections.unmodifiableSortedMap(new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
    }
}
