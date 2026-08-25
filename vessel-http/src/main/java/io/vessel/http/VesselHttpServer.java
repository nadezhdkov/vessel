package io.vessel.http;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Thin wrapper over {@link com.sun.net.httpserver.HttpServer}: turns raw
 * exchanges into {@link HttpRequest}/{@link HttpResponse}, asks a
 * {@link Router} what to do with them, and adds CORS on top when enabled.
 * If a later milestone swaps the JDK server for something else (Netty,
 * Undertow), no handler written against {@link HttpRequest}/{@link HttpResponse}
 * needs to change.
 *
 * <p>By default the underlying {@code HttpServer} dispatches every exchange
 * sequentially on a single thread (the JDK's own default when no executor is
 * set). {@link #withVirtualThreads()} (M11) opts into one virtual thread per
 * request instead — see that method's Javadoc for a real caveat about
 * {@code Container} before turning it on.
 */
public final class VesselHttpServer {

    private final HttpServer delegate;
    private final Router router = new Router();
    private CorsConfig corsConfig;

    private VesselHttpServer(HttpServer delegate) {
        this.delegate = delegate;
        this.delegate.createContext("/", this::dispatch);
    }

    public static VesselHttpServer create(int port) {
        return create(port, "localhost");
    }

    public static VesselHttpServer create(int port, String host) {
        Objects.requireNonNull(host, "host must not be null");
        try {
            var address = new InetSocketAddress(host, port);
            return new VesselHttpServer(HttpServer.create(address, 0));
        } catch (IOException e) {
            throw new VesselHttpException(
                    "failed to create HTTP server at %s:%d: %s".formatted(host, port, e.getMessage()), e);
        }
    }

    public VesselHttpServer get(String path, Handler handler) {
        router.register(new Route(HttpMethod.GET, path, handler));
        return this;
    }

    public VesselHttpServer post(String path, Handler handler) {
        router.register(new Route(HttpMethod.POST, path, handler));
        return this;
    }

    public VesselHttpServer put(String path, Handler handler) {
        router.register(new Route(HttpMethod.PUT, path, handler));
        return this;
    }

    public VesselHttpServer delete(String path, Handler handler) {
        router.register(new Route(HttpMethod.DELETE, path, handler));
        return this;
    }

    /** Registers an arbitrary route directly — the hook M6's ControllerScanner wires into. */
    public VesselHttpServer register(Route route) {
        router.register(route);
        return this;
    }

    public VesselHttpServer enableCors(CorsConfig config) {
        this.corsConfig = Objects.requireNonNull(config, "config must not be null");
        return this;
    }

    /**
     * Switches request dispatch from the JDK default (one thread, sequential)
     * to one virtual thread per exchange, via {@link Executors#newVirtualThreadPerTaskExecutor()}.
     * Must be called before {@link #start()}.
     *
     * <p><b>Real caveat, not a hypothetical one:</b> {@code io.vessel.core.Container}'s
     * internal caches (singletons, {@code @PostConstruct}/{@code @PreDestroy}
     * lookup) are plain, unsynchronized {@code HashMap}s — safe under this
     * concurrent dispatch only because {@code Vessel.start()} (M10) already
     * calls {@code container.resolveAll()} before {@code start()} is ever
     * reached, which eagerly builds and caches every registered bean (and
     * warms both lifecycle caches for every registered class) on a single
     * thread first; submitting the resulting request-handling tasks to this
     * executor afterward establishes a happens-before edge, so every virtual
     * thread sees a fully-built, no-longer-mutated set of caches. Bypass that
     * eager warm-up — build a {@code Container} by hand and never call
     * {@code resolveAll()} before serving requests — and this same setup
     * becomes a genuine, unsynchronized data race.
     */
    public VesselHttpServer withVirtualThreads() {
        delegate.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        return this;
    }

    public VesselHttpServer start() {
        delegate.start();
        return this;
    }

    public void stop() {
        delegate.stop(0);
    }

    public int port() {
        return delegate.getAddress().getPort();
    }

    public List<Route> routes() {
        return router.routes();
    }

    private void dispatch(HttpExchange exchange) {
        try {
            var request = readRequest(exchange);

            if (corsConfig != null && request.method() == HttpMethod.OPTIONS) {
                writePreflightResponse(exchange);
                return;
            }

            var response = respond(request);

            if (corsConfig != null) {
                response = response.withHeader("Access-Control-Allow-Origin", corsConfig.allowedOrigin());
            }

            writeResponse(exchange, response);
        } catch (IOException e) {
            throw new VesselHttpException("failed to process request: " + e.getMessage(), e);
        } finally {
            exchange.close();
        }
    }

    private HttpResponse respond(HttpRequest request) {
        return switch (router.match(request.method(), request.path())) {
            case RouteResult.Matched matched ->
                    matched.handler().handle(request.withPathVariables(matched.pathVariables()));
            case RouteResult.MethodNotAllowed notAllowed -> {
                var allowedNames = notAllowed.allowedMethods().stream()
                        .map(HttpMethod::name)
                        .collect(Collectors.joining(", "));
                yield HttpResponse.status(HttpStatus.METHOD_NOT_ALLOWED,
                                "method %s not allowed for %s — allowed methods: %s"
                                        .formatted(request.method(), request.path(), allowedNames))
                        .withHeader("Allow", allowedNames);
            }
            case RouteResult.NotFound notFound -> HttpResponse.status(HttpStatus.NOT_FOUND, notFound.message());
        };
    }

    private HttpRequest readRequest(HttpExchange exchange) throws IOException {
        var method = HttpMethod.fromString(exchange.getRequestMethod());
        var path = exchange.getRequestURI().getPath();
        var queryParams = parseQueryParams(exchange.getRequestURI().getQuery());
        var headers = flattenHeaders(exchange.getRequestHeaders());
        var body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return new HttpRequest(method, path, headers, queryParams, Map.of(), body);
    }

    private void writeResponse(HttpExchange exchange, HttpResponse response) throws IOException {
        response.headers().forEach((name, value) -> exchange.getResponseHeaders().set(name, value));
        var body = response.body();
        exchange.sendResponseHeaders(response.status().code(), body.length == 0 ? -1 : body.length);
        if (body.length > 0) {
            exchange.getResponseBody().write(body);
        }
    }

    private void writePreflightResponse(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", corsConfig.allowedOrigin());
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.sendResponseHeaders(HttpStatus.NO_CONTENT.code(), -1);
    }

    private static Map<String, String> flattenHeaders(Headers headers) {
        var flattened = new LinkedHashMap<String, String>();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                flattened.put(entry.getKey(), entry.getValue().get(0));
            }
        }
        return flattened;
    }

    private static Map<String, String> parseQueryParams(String query) {
        if (query == null || query.isBlank()) {
            return Map.of();
        }
        var params = new LinkedHashMap<String, String>();
        for (String pair : query.split("&")) {
            int separator = pair.indexOf('=');
            if (separator < 0) {
                params.put(urlDecode(pair), "");
            } else {
                params.put(urlDecode(pair.substring(0, separator)), urlDecode(pair.substring(separator + 1)));
            }
        }
        return params;
    }

    private static String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
