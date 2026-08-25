# Contributing to Vessel

Vessel is a didactic Java 21 micro-framework — the point of the project is the exercise of building each layer (DI container, HTTP server, routing, configuration) by hand, not shipping a production-ready framework. Keep that in mind before proposing a change: "this would be faster/easier with library X" is usually not a reason to add X here.

## Before you start

Read [`CLAUDE.md`](CLAUDE.md) first — it documents the permanent product decisions (zero runtime dependencies, constructor-only injection, no persistence/security/templating/auto-configuration, Vessel's own naming instead of Spring's) and the repository's module structure. Anything that conflicts with those rules needs to be discussed in an issue before any code is written, not proposed directly as a PR.

For a guided tour of what each module does and how to use it, see [`docs/README.md`](docs/README.md).

## Project rules that shape every contribution

- **Zero runtime dependencies.** No third-party `implementation` in any `vessel-*` module. `testImplementation` with JUnit 5 is the only exception anywhere in the main build.
- **Constructor injection only.** No field or setter injection, ever — it's a deliberate design choice, not a missing feature.
- **No persistence, no security, no templating, no auto-configuration.** These are permanently out of scope.
- **Vessel's own naming**, not a copy of Spring's annotations/types. See the "Vessel → Spring Boot" table in the root [README](README.md) for the established mapping.
- **Error messages are a requirement.** Every `VesselException` subclass must state what was missing, who asked for it, and the path that led there — write the message with the same care as the logic.

## Development workflow

```bash
make help    # every available command, documented
make build   # full build, every module
make test    # every module's tests
```

Or with Gradle directly:

```bash
./gradlew build
./gradlew :vessel-core:test      # a single module's tests
./gradlew :vessel-examples:run   # run the example app on http://localhost:8080
```

Requires Java 21.

## Making a change

1. **Open an issue first** for anything that isn't a small, obvious fix — especially anything that touches the rules above, adds a new annotation, or changes a public API. This project favors discussing scope before writing code.
2. **Respect the dependency graph.** `vessel-core` and `vessel-http` are leaves — they must never import from another Vessel module. If a leaf module needs functionality from an outer one, the pattern is an extension interface defined in the leaf (see `ParameterValueResolver` in `docs/vessel-core.md`), not a direct dependency in the wrong direction.
3. **Write tests with your change, not after.** Every new annotation needs at least one happy-path test and two error-path tests. Error tests must assert the message's **content**, not just the exception type. HTTP tests hit a real server (`VesselHttpServer.create(0)`) with the JDK's `HttpClient` — never mock the transport.
4. **Match existing conventions:** Java 21 idioms (`record`, pattern-matching `switch`, `var` in obvious local scope), immutable domain classes (`HttpRequest`, `HttpResponse`, `Response<T>`), `Optional`/Vessel exceptions instead of `null`/generic JDK exceptions in public APIs.
5. **Code, tests, Javadoc, and comments are in English.** Run `make format-check` before opening a PR — it fails if it finds non-English text leaking into `src/`.
6. **Update `module-info.java`** if you add a new public class that needs exporting.
7. **Update `docs/`** if your change is user-visible — the wiki pages are meant to stay accurate, not aspirational.

## Submitting a pull request

- Keep PRs focused — one change, one concern. A bug fix doesn't need surrounding cleanup.
- Make sure `./gradlew build` and `make format-check` both pass locally before opening the PR.
- Describe *why* the change is needed, not just what it does — the diff already shows what changed.
- If your change affects the naming table, the module dependency graph, or any documented behavior, update the corresponding `docs/` page and/or the root `README.md` in the same PR.

## Reporting bugs

Open a GitHub issue with: what you expected, what happened instead, and the smallest reproduction you can manage (a failing test is ideal). Since this is an educational project, "this doesn't match how Spring does it" is only a bug if it contradicts something Vessel itself documents — a deliberate simplification is not a bug.

## Security issues

See [`SECURITY.md`](SECURITY.md) — please don't file security-sensitive reports as public issues.
