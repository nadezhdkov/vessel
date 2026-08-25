package io.vessel.web;

import io.vessel.http.HttpResponse;
import io.vessel.http.HttpStatus;
import io.vessel.web.json.JsonSerializer;

import java.time.Instant;

/**
 * Turns a controller's return value into an {@link HttpResponse}: JSON body
 * plus the right {@code Content-Type}. {@code null} bodies (e.g.
 * {@code Response.noContent()}) get an empty body and no JSON at all —
 * there's nothing to serialize. An {@link ErrorResponse} body is enriched
 * into the standardized four-field error shape ({@code status}, {@code error},
 * {@code message}, {@code timestamp}) right here, not by the handler author.
 */
public final class ResponseWriter {

    private ResponseWriter() {
    }

    public static HttpResponse write(HttpStatus status, Object body) {
        if (body == null) {
            return HttpResponse.status(status, (byte[]) null);
        }
        var wireBody = body instanceof ErrorResponse errorResponse
                ? new StandardErrorBody(status.code(), errorResponse.error(), errorResponse.message(), Instant.now().toString())
                : body;
        var json = JsonSerializer.serialize(wireBody);
        return HttpResponse.status(status, json).withHeader("Content-Type", "application/json; charset=utf-8");
    }

    private record StandardErrorBody(int status, String error, String message, String timestamp) {
    }
}
