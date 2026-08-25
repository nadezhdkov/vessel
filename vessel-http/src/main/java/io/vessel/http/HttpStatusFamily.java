package io.vessel.http;

/**
 * Categorizes HTTP status codes into standard families.
 *
 * <p>Usage: {@code HttpStatus.NOT_FOUND.family()} returns {@code CLIENT_ERROR}.
 */
public enum HttpStatusFamily {
    INFORMATIONAL,
    SUCCESS,
    REDIRECTION,
    CLIENT_ERROR,
    SERVER_ERROR
}
