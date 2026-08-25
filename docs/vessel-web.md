# vessel-web

Controllers, dispatching, argument resolution, and JSON — the layer that connects HTTP (`vessel-http`) to dependency injection (`vessel-core`). Depends on both.

```java
import io.vessel.web.annotation.*;
```

## Writing a `@RestController`

```java
@RestController
public class UserController {

    private final UserService userService;

    @Inject
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Get("/users")
    public List<User> users() {
        return userService.findAll();
    }

    @Get("/users/{id}")
    public Response<User> getUser(@PathVariable long id) {
        return Response.ok(userService.findById(id));
    }

    @Post("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public User createUser(@RequestBody User user) {
        return userService.save(user);
    }
}
```

`@RestController` implies `@Component` — a controller is a normal DI bean, constructor-injected like anything else. `@Get`/`@Post`/`@Put`/`@Delete` map a method to a route; `container.scan(basePackage)` finds `@Component`s, and a separate pass in `ControllerScanner` finds `@RestController`s and registers their routes.

## Parameter binding

| Annotation | Source | Example |
|---|---|---|
| `@PathVariable` | a `{name}` segment of the route | `@Get("/users/{id}") ... (@PathVariable long id)` |
| `@RequestParam` | a query string parameter | `@Get("/search") ... (@RequestParam String q)` |
| `@RequestBody` | the request body, deserialized | `@Post("/users") ... (@RequestBody User user)` |

`@PathVariable`/`@RequestParam` support `String`, `int`/`long`/`double`/`boolean` (primitive or wrapper) — nothing else. Leaving `value()` empty uses the parameter's own name (`@PathVariable long id` looks for `{id}`), which needs `-parameters` at compile time; every Vessel module already compiles with it.

A missing path variable or query parameter fails with a message naming exactly what was expected:
```
no path variable named 'id' for parameter 'id' of UserController.getUser — check that the route path declares '{id}'
```

## Return values and `Response<T>`

A controller method can return a plain value (serialized with `HttpStatus.OK`, or whatever `@ResponseStatus` declares) or a `Response<T>`:

```java
public final class Response<T> {
    public static <T> Response<T> ok(T body) { /* HttpStatus.OK */ }
    public static <T> Response<T> created(T body) { /* HttpStatus.CREATED */ }
    public static <T> Response<T> noContent() { /* HttpStatus.NO_CONTENT, null body */ }
    public static <T> Response<T> status(HttpStatus status, T body) { /* any status */ }
}
```

A `Response<T>`'s own status always wins over `@ResponseStatus` — it's the more specific, per-invocation source of truth.

## JSON

No Jackson, by design — a small reflection-based serializer instead. Serialization supports `String`, primitive wrappers, `boolean`, `record` (via `RecordComponent`), `List<T>`, `Map<String,T>`, and plain getter-based objects (any `getXxx()`/`isXxx()` no-arg method, JavaBean-style):

```java
public record User(long id, String name, String email) {}
```

```java
Response.ok(new User(42, "Alice", "alice@example.com"));
// → {"id":42,"name":"Alice","email":"alice@example.com"}
```

Deserialization (`@RequestBody`) stays narrower on purpose — `record` and primitives only, since that's all a request body ever needed.

## Exception handling

```java
@ExceptionHandler(UserNotFoundException.class)
public Response<ErrorResponse> handleNotFound(UserNotFoundException e) {
    return Response.status(HttpStatus.NOT_FOUND, new ErrorResponse("USER_NOT_FOUND", e.getMessage()));
}
```

`@ExceptionHandler` methods are scoped to the controller that declares them — there is no `@ControllerAdvice` equivalent shared across controllers. When a route method throws, the handler declaring the **most specific** matching exception type wins (walking the thrown exception's own superclass chain); `value()` can be left at its default, in which case the handler targets whatever type its single parameter declares.

`ErrorResponse` is deliberately just two fields:

```java
public record ErrorResponse(String error, String message) {}
```

`status` and `timestamp` are added automatically when the response is written, producing a standardized four-field error body:

```json
{"status":404,"error":"USER_NOT_FOUND","message":"user 42 not found","timestamp":"2026-08-25T03:10:44.306171701Z"}
```

If a controller has no matching handler at all (or none declared), an uncaught exception becomes a bare `500` — the server never crashes from a controller exception. If the `@ExceptionHandler` *itself* throws, that same bare-500 fallback applies; a broken handler can't take the whole exchange down either.

Two `@ExceptionHandler`s for the exact same exception type on one controller is rejected at `scan()` time, not on the first request that hits it:
```
duplicate @ExceptionHandler for type 'IllegalStateException' on 'DuplicateHandlerController': 'first' and 'second' both declare it
```

## Middleware: `@Before` / `@After`

```java
@RestController
public class UserController {

    @Before
    public HttpResponse requireAuth(HttpRequest request) {
        if (request.header("Authorization").isEmpty()) {
            return HttpResponse.status(HttpStatus.UNAUTHORIZED, "missing token");
        }
        return null; // continue to the route method
    }

    @After
    public HttpResponse logStatus(HttpRequest request, HttpResponse response) {
        System.out.println(request.path() + " -> " + response.status().code());
        return response;
    }

    // ... route methods
}
```

- `@Before` — signature `HttpResponse method(HttpRequest)`. Return `null` to let the request continue; return a non-null `HttpResponse` to **short-circuit the whole exchange** — the route method, and any `@After`, never run.
- `@After` — signature `HttpResponse method(HttpRequest, HttpResponse)`. Runs after the route method (or after `@ExceptionHandler`, if that's what actually produced the response) and its return value becomes the response actually sent.

Scoped per controller, same as `@ExceptionHandler` — no shared/global middleware. At most one `@Before` and one `@After` per controller; a second one of either fails at scan time with a message naming both methods. A broken `@Before`/`@After` produces a `500` rather than crashing the exchange, same as a broken `@ExceptionHandler`.

## Under the hood

| Class | Responsibility |
|---|---|
| `ControllerScanner` | Finds `@RestController` classes, registers each into the `Container`, turns `@Get`/`@Post`/`@Put`/`@Delete` methods into routes, validates `@ExceptionHandler`/`@Before`/`@After` up front |
| `Dispatcher` | Runs `@Before` → resolves arguments → invokes the route method (or the matching `@ExceptionHandler`, on failure) → runs `@After` → writes the result |
| `ArgumentResolver` | Converts `@PathVariable`/`@RequestParam`/`@RequestBody` parameters to their target types |
| `ResponseWriter` | Serializes the return value to JSON, sets `Content-Type`, enriches `ErrorResponse` bodies |
| `ExceptionResolver` / `MiddlewareResolver` | Find and validate a controller's `@ExceptionHandler`(s) / `@Before`/`@After` |
