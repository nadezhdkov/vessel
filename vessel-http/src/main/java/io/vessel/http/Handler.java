package io.vessel.http;

/**
 * User code that turns a request into a response. Vessel's own name for
 * what the JDK calls {@code HttpHandler} — deliberately not reused, since
 * that one works against {@code HttpExchange}, not Vessel's immutable
 * {@link HttpRequest}/{@link HttpResponse}.
 */
@FunctionalInterface
public interface Handler {

    HttpResponse handle(HttpRequest request);
}
