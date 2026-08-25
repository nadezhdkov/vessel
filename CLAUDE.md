# CLAUDE.md

Context for working in the Vessel repository. Read this before generating or changing code.

## What Vessel is

A didactic web micro-framework for Java 21 — DI container, HTTP server, routing, and configuration, built from scratch. The goal is **not to produce a production framework**, it's to learn Spring Boot's mental model by implementing every layer by hand.

**Status: complete.** Every module (`vessel-core`, `vessel-http`, `vessel-web`, `vessel-config`, `vessel-app`, `vessel-examples`) is implemented, tested, and documented — see [`docs/README.md`](docs/README.md) for a per-module usage guide with real examples. The root README covers the overview, a quick example, the Vessel↔Spring naming table, and the benchmark against real Spring. There is no longer a roadmap of pending milestones — new work here is maintenance, a targeted extension, or a fix, not "continue the next milestone".

## Rules that must never be broken

These are permanent product decisions, not temporary technical limitations — don't relax them without discussing with the user first:

- **Zero runtime dependencies.** No third-party `implementation` in any `vessel-*` module's `build.gradle.kts`. `testImplementation` with JUnit 5 (via `gradle/libs.versions.toml`) is the only exception. If the temptation is to reach for Jackson, Reflections, Classgraph, Guava, etc., the answer is always: implementing it is part of the exercise. Don't suggest adding libraries to "solve it faster". (The standalone projects in `benchmarks/spring-comparison/` are the one documented, deliberate exception — never part of the main build, never touched by `./gradlew build`.)
- **Constructor injection only.** Never implement field or setter injection, even if trivial via `Field#setAccessible`. This is a product decision, not a technical limitation — it makes the dependency visible in the signature, allows `final` fields, and keeps the class testable without the container.
- **No persistence, no security, no templating, no auto-configuration.** No ORM, authentication/authorization, template engine, or "starter" that guesses configuration. Anything outside what already exists in `docs/` — stop and discuss scope with the user before implementing.
- **Own names, not Spring copies.** `@Get` not `@GetMapping`, `Response<T>` not `ResponseEntity<T>`, `Vessel.start()` not `SpringApplication.run()`. See the full mapping table in the root README (the "Vessel → Spring Boot" section).
- **Error messages are a requirement, not polish.** Every `VesselException` (and its per-module subclasses — `VesselHttpException`, `VesselWebException`, `VesselConfigException`, `VesselAppException`) must answer: what was missing, who asked for it, and the path that got there. When implementing any validation, write the error message with the same care as the logic itself. Real examples on every `docs/` page.

## Repository structure

```
vessel-core/      DI container — annotations, graph, scanning, lifecycle. Depends on nothing.
vessel-http/      Pure HTTP primitives — request/response, router, status, @Server. Depends on nothing.
vessel-web/       Controllers, dispatcher, argument resolver, JSON serializer. Depends on core + http.
vessel-config/    Properties, Environment, @Value. Depends on core.
vessel-app/       Bootstrap (Vessel.start()), @Application. Depends on all of the above.
vessel-examples/  Functional example app (HelloApplication/User/UserService/UserController), compiled by the build.
benchmarks/       Two standalone Gradle projects (vessel-startup/, spring-comparison/) — outside the main tree, never in settings.gradle.kts.
```

Dependency graph rule: `vessel-core` and `vessel-http` are the leaves — they must never import from another Vessel module. If a test or implementation in `vessel-core` needs something from `vessel-http` (or any other module), that's a design-error signal — stop and reconsider before proceeding. When a leaf module's feature needs something from an outer module (e.g. `vessel-config`'s `@Value` resolving a constructor parameter in `vessel-core`'s `Container`), the fix is an extension interface defined in the leaf module (e.g. `ParameterValueResolver`) implemented by the outer module — never a direct dependency in the wrong direction. See `docs/vessel-core.md` § "Why vessel-core never imports vessel-config" for the full worked example of this pattern.

Every module has a `module-info.java` (except `vessel-examples`, which runs unnamed on the classpath/module path) — when creating a new public class, remember to update `exports` as needed. Every module's tests run on the classpath (no `module-info.java` under `src/test/java`), so reflection in tests normally doesn't need `opens`.

## Code conventions

- Java 21: use `record` for DTOs, `switch` with pattern matching where it makes sense, `var` in local scope when the type is obvious from context.
- Framework domain classes (`HttpRequest`, `HttpResponse`, `Response<T>`) are immutable.
- `Container` implements `AutoCloseable`; bean destruction order (`@PreDestroy`) is the reverse of creation order.
- Prefer `Optional`/Vessel-specific exceptions (`VesselException` and subtypes) over `null` or generic JDK exceptions in public APIs.
- Code, tests, Javadoc, and comments are in English. `make format-check` fails the build if it finds Portuguese text leaking into `src/`.

## Tests

- Every new annotation needs at least one happy-path test and two error-path tests.
- Error tests must assert the **content** of the message, not just the exception type thrown.
- HTTP tests use the JDK's `java.net.http.HttpClient` against a real server on a random port (`VesselHttpServer.create(0)`) — never mock the transport.
- `vessel-core` and `vessel-config` are tested mostly at the unit level; `vessel-http` mixes unit and integration; `vessel-web` and `vessel-app` are mostly integration (`vessel-app` in particular boots the real server and hits it with a real HTTP client — it's the project's E2E level).

## Commands

`make help` lists every available command (documented) — it's just a thin wrapper over the `./gradlew` commands below, nothing in it is real build logic. See the root `Makefile`.

```bash
./gradlew build                  # full build, every module
./gradlew :vessel-core:test      # a single module's tests
./gradlew :vessel-http:test      # same, for vessel-http
./gradlew :vessel-web:test       # same, for vessel-web
./gradlew :vessel-config:test    # same, for vessel-config
./gradlew :vessel-app:test       # same, for vessel-app
./gradlew :vessel-examples:run   # run the example app (HelloApplication, port 8080)
```

All six optional stretch goals are already implemented: lazy injection (`Supplier<T>`), `@Timed` proxying, virtual threads (`withVirtualThreads()`), configuration hot-reload (`Environment.reload()`), `@Before`/`@After` middleware, and the benchmark against real Spring (`benchmarks/`, comparing `Container` against `AnnotationConfigApplicationContext`). `spring-comparison/` is the only folder in the repository that depends on Spring — intentional and isolated, not a violation of the zero-dependencies rule (that rule applies to the `vessel-*` modules, not to measurement tooling outside the main tree).

Publishing to Maven Central is already configured in `build.gradle.kts` (the `com.vanniktech.maven.publish` plugin, version 0.34.0 — deliberately not the latest, because 0.35.0+ requires Gradle 8.13+ and this project is on wrapper 8.10). Real credentials live in `gradle.properties` (never committed — it's in `.gitignore`). `make sign-check` validates the GPG signature without publishing anything; `make publish-local` publishes only to the local `~/.m2`; `make publish` is the real, irreversible step.

## Before proposing an external dependency or a scope shortcut

Stop and ask the user. This includes: third-party libraries in `vessel-*` modules, field/setter injection, anything related to persistence or security, and any feature not documented in `docs/`. The project's goal is the exercise, not the fastest delivery.
