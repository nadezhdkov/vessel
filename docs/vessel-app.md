# vessel-app

Bootstrap — the module that wires every other module together into a running application. Depends on `vessel-core`, `vessel-http`, `vessel-web`, and `vessel-config`.

```java
import io.vessel.app.Vessel;
import io.vessel.app.annotation.Application;
```

## `Vessel.start()`

```java
@Server(port = 8080, host = "localhost")
@Application
public class MyApplication {

    public static void main(String[] args) {
        Vessel.start(MyApplication.class);
    }
}
```

`Vessel.start(AppClass.class)` runs the whole startup sequence in one call and returns the running `VesselHttpServer`:

1. Validates that the class carries **both** `@Application` and `@Server` — see [Required annotations](#required-annotations) below.
2. Loads configuration via `Configuration.load()`, which can override `@Server`'s `port`/`host` (`server.port`/`server.host` properties, checked with the usual system-property > environment-variable > properties-file priority).
3. Creates the `Container`, plugs the loaded `Environment` in as both the `ParameterValueResolver` (for `@Value`) and a directly injectable bean (so any `@Component` can `@Inject` `Environment` itself — see [vessel-config.md](vessel-config.md#hot-reload-without-a-restart)).
4. Creates the `VesselHttpServer`, enabling CORS if `@Server(enableCors = true)`.
5. Scans the application class's own package: `container.scan(basePackage)` for `@Component`s, then `ControllerScanner.scan(...)` for `@RestController`s (which also registers their `@ExceptionHandler`/`@Before`/`@After`).
6. Calls `container.resolveAll()` — validates the **entire** dependency graph before the socket ever binds. A wiring mistake fails here, at startup, never on the first request that happens to touch it.
7. Binds the socket and starts accepting requests.

```
→ @Server detected: port=8080, host=localhost, cors=disabled
→ Configuration loaded from application.properties
→ DI Container initialized: 3 beans registered
→ Routes registered:
    GET  /users
    GET  /users/{id}
    POST /users
Vessel started on http://localhost:8080 in 154ms
```

## Required annotations

Both `@Application` and `@Server` are mandatory on the class passed to `Vessel.start()` — every real example uses them together. Missing either fails immediately with a message naming the class and the missing annotation:

```
'com.example.MyApp' cannot be started — it is missing @Application. Vessel.start() needs that annotation on the class to know it is a real entry point, not just any class
```

```
'com.example.MyApp' cannot be started — it is missing @Server. Vessel.start() needs at least @Server() (its defaults are fine) to know which port and host to bind
```

`@Application` itself carries no attributes — it exists purely so `Vessel.start()` can fail fast on the wrong class instead of silently scanning whatever package it happens to sit in.

## Startup failures release the port

If scanning or `resolveAll()` fails, `Vessel.start()` stops the server before propagating the exception — the JDK's `HttpServer` binds its socket at *creation* time, not at `start()`, so without this the port would stay held until the process exits even though startup never actually succeeded:

```java
var container = new Container().register(BrokenService.class); // depends on an unregistered type
Vessel.start(BrokenApplication.class);
// throws MissingDependencyException — and the socket is released, not leaked
```

```
no bean of type 'UnregisteredDependency' registered
  requested by: BrokenService(constructor, parameter 1)
  path: BrokenService → UnregisteredDependency
```

## What gets scanned

`Vessel.start()` scans exactly one package: `applicationClass.getPackageName()`. Put your `@Application` class at the root of your app's package tree — `scan()` walks subpackages recursively, so everything underneath is found automatically.

## Using `Container`/`VesselHttpServer` without `Vessel.start()`

Everything `Vessel.start()` does is built from public APIs in the other modules — you can assemble the same pieces yourself if you need something the facade doesn't offer (e.g. multiple base packages, custom `PropertySource`s):

```java
var environment = Configuration.load();
var container = new Container().withParameterValueResolver(environment);
var server = VesselHttpServer.create(8080, "localhost");

container.scan("com.example.services");
container.scan("com.example.more");
ControllerScanner.scan("com.example.controllers", container, server);
container.resolveAll();

server.start();
```

This is exactly what `Vessel.start()` does internally — it just isn't exposed as configurable options today.
