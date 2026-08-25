# Vessel documentation

A per-module guide to Vessel — what each module does, its full public API, and worked examples. Start with the [root README](../README.md) for the project overview, the quick example, and the Vessel-vs-Spring naming table; come here when you need the details behind a specific module.

Read in dependency order — each page assumes you already know the ones before it:

1. **[vessel-core](vessel-core.md)** — the DI container: registration, scanning, qualifiers, scopes, lifecycle, `@Configuration`/`@Bean`, lazy injection, `@Timed` interception. Depends on nothing.
2. **[vessel-http](vessel-http.md)** — HTTP primitives: `VesselHttpServer`, `HttpRequest`/`HttpResponse`, `Router`, `HttpStatus`, CORS, virtual threads. Depends on nothing.
3. **[vessel-web](vessel-web.md)** — controllers: `@RestController`, routing annotations, JSON (de)serialization, `@ExceptionHandler`, `@Before`/`@After` middleware. Depends on vessel-core + vessel-http.
4. **[vessel-config](vessel-config.md)** — configuration: `PropertySource`, `Environment`, `@Value`, type conversion, hot-reload. Depends on vessel-core.
5. **[vessel-app](vessel-app.md)** — bootstrap: `Vessel.start()`, `@Application`, the full startup sequence. Depends on all of the above.

## Module dependency graph

```
vessel-app
  ├── vessel-core
  ├── vessel-http
  ├── vessel-web
  └── vessel-config

vessel-web
  ├── vessel-core
  └── vessel-http

vessel-config
  └── vessel-core

vessel-http   (no internal dependencies — a leaf)
vessel-core   (no internal dependencies — a leaf)
```

`vessel-core` and `vessel-http` never import from any other Vessel module, by design — see [vessel-core.md](vessel-core.md#why-vessel-core-never-imports-vessel-config) for what that constraint forces when a feature (like `@Value`) needs both.

## Where things live

| I want to... | Go to |
|---|---|
| Register a bean by hand, or via `@Component` | [vessel-core.md § Registering and resolving](vessel-core.md#registering-and-resolving) |
| Pick between several implementations of an interface | [vessel-core.md § Qualifiers and @Primary](vessel-core.md#qualifiers-and-primary) |
| Run code after a bean is built, or before the container closes | [vessel-core.md § Lifecycle](vessel-core.md#lifecycle-postconstruct--predestroy) |
| Break a dependency cycle on purpose | [vessel-core.md § Lazy injection with Supplier\<T\>](vessel-core.md#lazy-injection-with-suppliert) |
| Log how long a method takes | [vessel-core.md § @Timed interception](vessel-core.md#timed-interception) |
| Start an HTTP server without any annotations | [vessel-http.md § Programmatic server](vessel-http.md#programmatic-server) |
| Understand a 404/405 response | [vessel-http.md § Routing](vessel-http.md#routing) |
| Enable CORS | [vessel-http.md § CORS](vessel-http.md#cors) |
| Write a `@RestController` | [vessel-web.md § Controllers](vessel-web.md#writing-a-restcontroller) |
| Handle an exception thrown by a controller | [vessel-web.md § Exception handling](vessel-web.md#exception-handling) |
| Add logging/auth around every route in a controller | [vessel-web.md § Middleware](vessel-web.md#middleware-before--after) |
| Read a property, with a default, converted to a type | [vessel-config.md § Environment](vessel-config.md#reading-properties-with-environment) |
| Inject a config value into a bean | [vessel-config.md § @Value](vessel-config.md#value-injection) |
| Start a whole application | [vessel-app.md § Vessel.start()](vessel-app.md#vesselstart) |

## Conventions used on these pages

- Every code sample on these pages either comes directly from a real, tested file in this repository, or was compiled and run against the built jars to confirm it actually works — nothing here is speculative.
- Error messages are quoted verbatim from the exception-building code, not paraphrased.
- "M0"–"M11" labels you'll see in code comments refer to this project's build-order milestones (core DI → HTTP → web → config → bootstrap → stretch goals). They're historical implementation-order markers, not something you need to track to use the framework.
