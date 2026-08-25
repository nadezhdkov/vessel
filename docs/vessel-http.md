# vessel-http

Pure HTTP primitives — no dependency injection, no annotations processed automatically. Depends on nothing else in Vessel; it's a leaf of the module graph.

```java
import io.vessel.http.*;
```

## Programmatic server

The whole API works standalone, without `@Server` or `vessel-app`:

```java
var server = VesselHttpServer.create(8080)
        .get("/hello", request -> HttpResponse.ok("Hello, World!"))
        .start();
```

`VesselHttpServer.create(port)` defaults to host `"localhost"`; `create(port, host)` picks both explicitly. Port `0` asks the OS for a free ephemeral port — useful for tests:

```java
var server = VesselHttpServer.create(0);
server.start();
int actualPort = server.port(); // the OS-assigned port, not 0
```

Register routes with `.get(path, handler)` / `.post(...)` / `.put(...)` / `.delete(...)`, or register a `Route` record directly with `.register(route)` (the hook `vessel-web`'s controller scanner uses). Call `.stop()` to shut the socket down.

## `HttpRequest` / `HttpResponse`

Both are immutable value types — nothing here wraps the JDK's own `HttpExchange`.

```java
public interface Handler {
    HttpResponse handle(HttpRequest request);
}
```

`HttpRequest` gives you `method()`, `path()`, `headers()`/`header(name)`, `queryParams()`/`queryParam(name)`, `pathVariables()`/`pathVariable(name)` (all as `Optional<String>` for the singular accessors), and `body()` as a raw `String`.

`HttpResponse` is built via static factories — `ok(body)`, `created(body)`, `noContent()`, `status(status, body)` (both `String`- and `byte[]`-bodied overloads) — and `.withHeader(name, value)` to add headers immutably (returns a new instance).

## Routing

```java
var server = VesselHttpServer.create(8080)
        .get("/users/{id}", request ->
                HttpResponse.ok("user " + request.pathVariable("id").orElseThrow()));
```

Path variables (`{id}`) are extracted and available via `request.pathVariable(name)`. An **exact** route always wins over a **parameterized** one for the same request, regardless of registration order — `/users/me` beats `/users/{id}` when the request is literally `/users/me`.

Two failure modes are modeled as data, not exceptions:

- **Method exists, wrong verb** → `405 Method Not Allowed`, with an `Allow` header listing what would have worked.
- **Path doesn't exist at all** → `404 Not Found`, with a message that lists nearby registered routes and suggests the closest match when one exists:
  ```
  no handler for GET /api/users/42/orders
    routes registered for /api/users:
      GET  /api/users
      GET  /api/users/{id}
      POST /api/users
    did you mean: GET /api/users/{id} ?
  ```

## `HttpStatus`

Full coverage of the HTTP standard — every 1xx through 5xx status the spec defines, each carrying its official reason phrase, plus a `family()` and boolean helpers:

```java
HttpStatus.NOT_FOUND.code();           // 404
HttpStatus.NOT_FOUND.reason();         // "Not Found"
HttpStatus.NOT_FOUND.family();         // HttpStatusFamily.CLIENT_ERROR
HttpStatus.NOT_FOUND.isClientError();  // true
HttpStatus.fromCode(404);              // HttpStatus.NOT_FOUND (UNKNOWN_STATUS if no match)
```

## `@Server` and CORS

```java
@Server(port = 9000, host = "localhost", enableCors = true, corsOrigin = "http://localhost:3000")
@Application
public class MyApp {
    public static void main(String[] args) {
        Vessel.start(MyApp.class);
    }
}
```

`@Server` is declarative metadata — `vessel-http` itself never reads it (no scanning of its own). Turning it into a running server, including letting `application.properties` override the port/host it declares, is `vessel-app`'s job; see [vessel-app.md](vessel-app.md).

Using `VesselHttpServer` directly, enable CORS with a `CorsConfig`:

```java
server.enableCors(new CorsConfig("http://localhost:3000"));
```

An `OPTIONS` preflight request then gets a `204 No Content` back with:
```
Access-Control-Allow-Origin: http://localhost:3000
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Authorization
```

This is a deliberately minimal CORS implementation — just the allowed origin is configurable; methods and headers are fixed.

## Virtual threads

```java
var server = VesselHttpServer.create(8080)
        .get("/hello", request -> HttpResponse.ok("Hello!"))
        .withVirtualThreads()
        .start();
```

By default, the underlying JDK `HttpServer` dispatches every exchange **sequentially on one thread** — that's the JDK's own default when no executor is set. `withVirtualThreads()` opts into `Executors.newVirtualThreadPerTaskExecutor()` instead, so requests are handled concurrently. It must be called before `.start()`.

**A real caveat, not a hypothetical one:** `Container`'s internal caches (singletons, `@PostConstruct`/`@PreDestroy` lookup) are plain, unsynchronized `HashMap`s. This is safe under concurrent dispatch *only* because `Vessel.start()` already calls `container.resolveAll()` before `start()` is ever reached — eagerly building and caching every bean on a single thread first, so every virtual thread sees a fully-built, no-longer-mutated set of caches. Build a `Container` by hand and skip `resolveAll()` before serving concurrent requests, and this becomes a genuine, unsynchronized data race. This is why `withVirtualThreads()` is opt-in rather than a Vessel-wide default.

## Swapping the underlying server

If a future version replaces `com.sun.net.httpserver.HttpServer` with something else (Netty, Undertow), no code written against `HttpRequest`/`HttpResponse`/`Handler` needs to change — that's the entire reason those types exist as Vessel's own abstractions instead of exposing the JDK's `HttpExchange` directly.
