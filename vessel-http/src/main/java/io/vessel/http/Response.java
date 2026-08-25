package io.vessel.http;

/**
 * A controller-level response: status plus a typed body. Distinct from
 * {@link HttpResponse}, which is the raw wire-level representation — the web
 * layer (M6+) turns a {@code Response<T>} into an {@code HttpResponse} by
 * serializing {@code T}. Vessel's answer to {@code ResponseEntity<T>}.
 */
public final class Response<T> {

    private final HttpStatus status;
    private final T body;

    private Response(HttpStatus status, T body) {
        this.status = status;
        this.body = body;
    }

    public static <T> Response<T> ok(T body) {
        return new Response<>(HttpStatus.OK, body);
    }

    public static <T> Response<T> created(T body) {
        return new Response<>(HttpStatus.CREATED, body);
    }

    public static <T> Response<T> noContent() {
        return new Response<>(HttpStatus.NO_CONTENT, null);
    }

    public static <T> Response<T> status(HttpStatus status, T body) {
        return new Response<>(status, body);
    }

    public HttpStatus status() {
        return status;
    }

    public T body() {
        return body;
    }
}
