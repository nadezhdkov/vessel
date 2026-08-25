package io.vessel.web;

/**
 * The body an {@code @ExceptionHandler} method returns to describe what
 * went wrong. Deliberately just these two fields — {@code status} (already
 * expressed via {@code Response.status(...)}) and {@code timestamp} aren't
 * the handler author's concern; {@link ResponseWriter} adds them
 * automatically when it writes an {@code ErrorResponse}, producing a
 * standardized four-field error body ({@code status}, {@code error},
 * {@code message}, {@code timestamp}) for every handled exception.
 */
public record ErrorResponse(String error, String message) {
}
