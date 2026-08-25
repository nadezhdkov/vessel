# vessel-config

Configuration: property sources, type conversion, and `@Value`. Depends on `vessel-core` (for the `ParameterValueResolver` seam `@Value` plugs into).

```java
import io.vessel.config.*;
import io.vessel.config.annotation.Value;
```

## Loading configuration

```java
Environment environment = Configuration.load(); // reads application.properties from the classpath
```

`Configuration.load()` assembles the standard `Environment`: three sources, checked in priority order —

1. **System properties** (`-Dserver.port=8080`) — highest priority
2. **Environment variables** — `SERVER_PORT`, using relaxed-binding: a key like `server.port` is looked up as `SERVER_PORT`, since shells don't allow dots and don't follow the lowercase convention property files do
3. **`application.properties`** on the classpath — lowest priority, and entirely optional; a missing file is treated as an empty source, not an error

`Configuration.load(resourceName)` loads a differently-named properties file instead of the default `application.properties`.

## Reading properties with `Environment`

```java
environment.get("app.name");                          // Optional<String>
environment.get("app.name", "default value");          // String, with a fallback
environment.require("app.name");                       // String, throws if absent
environment.get("server.port", Integer.class);         // converted, throws if absent
environment.get("server.port", Integer.class, 8080);   // converted, with a fallback
```

A missing required key names exactly what was checked:
```
no property named 'server.port' found — checked system properties, environment variables and the properties file, in that priority order
```

### Type conversion

`Environment.get(key, Class<T>)` converts the raw string to `String`, `Integer`, `Long`, `Double`, `Boolean`, `Duration`, or any `enum` type. Conversion failures name the key, the raw value, and the target type:

```
property 'port' has value 'not-a-number', which cannot be converted to Integer: For input string: "not-a-number"
property 'debug' has value 'yes', which cannot be converted to Boolean: expected 'true' or 'false'
property 'level' has value 'VERBOSE', which is not a constant of enum LogLevel — valid values are: DEBUG, INFO, WARN, ERROR
```

`Duration` accepts a number with a unit suffix — `ns`, `us`, `ms`, `s`, `m`, `h`, `d` — not the JDK's own `PT30S` ISO-8601 format, since nobody writes `server.timeout=PT30S` in a properties file by choice. No suffix defaults to milliseconds:
```properties
server.timeout=30s
cache.ttl=500ms
session.duration=2h
```

## `@Value` injection

```java
@Component
public class ServerConfig {

    private final int port;
    private final String host;

    @Inject
    public ServerConfig(@Value("${server.port}") int port, @Value("${server.host}") String host) {
        this.port = port;
        this.host = host;
    }
}
```

`@Value` targets constructor parameters only — no field injection, consistent with the rest of Vessel. It mixes freely with normal `@Inject`-resolved parameters in the same constructor:

```java
@Component
public class AppInfo {

    @Inject
    public AppInfo(GreetingService greetingService, @Value("${app.name}") String name) {
        // greetingService resolved from the bean graph, name resolved from Environment — same call
    }
}
```

The placeholder must be exactly `${property.key}` — no SpEL, no nested placeholders, no default-value syntax like `${key:default}` (`Environment.get(key, type, default)` already covers "value with a fallback" from the Java side). A malformed placeholder is rejected clearly:
```
'server.port' is not a valid @Value placeholder — expected the form '${property.key}'
```

**How this actually gets wired up:** `vessel-core` is a leaf module and can't know about `@Value` directly. `Environment` implements `vessel-core`'s `ParameterValueResolver` interface, and `Vessel.start()` (in `vessel-app`) plugs it into the container:
```java
container.withParameterValueResolver(environment);
```
See [vessel-core.md](vessel-core.md#why-vessel-core-never-imports-vessel-config) for the other side of this. Using `Container` directly without `vessel-app`, you'd need to call `withParameterValueResolver` yourself for `@Value` to do anything.

## Hot-reload without a restart

```java
environment.reload();
```

Re-reads every source's backing state — for the properties-file source, that means re-reading the file from the classpath, so a change written to it after the `Environment` was built becomes visible to the next `get()` call. `Vessel.start()` registers the `Environment` itself into the container, so any `@Component`/`@RestController` can `@Inject` it directly and call `reload()` on demand (e.g. from an admin endpoint):

```java
@RestController
public class ConfigController {
    private final Environment environment;

    public ConfigController(Environment environment) { this.environment = environment; }

    @Post("/admin/reload-config")
    public void reload() { environment.reload(); }
}
```

**What this does *not* do:** a value already baked into a bean via `@Value` at construction time was resolved once and stays whatever it was — `reload()` doesn't re-run constructor injection or rebuild beans. It only changes what a *future* `environment.get(...)` call sees. A full "recreate affected beans" hot-reload (Spring Cloud Config's `@RefreshScope`) is a different, much larger feature this doesn't attempt.

Also worth knowing: this only reflects a genuine change on disk when the resource resolves to a real file on an exploded classpath directory — a resource packaged inside a jar is fixed at build time no matter how many times it's reread.

## `PropertySource`

```java
public interface PropertySource {
    Optional<String> get(String key);
    default void refresh() {} // override only if this source caches something
}
```

This is the plugin interface every source (system properties, environment variables, the properties file) implements. `Environment`'s multi-source constructor is intentionally package-private — only `Configuration` decides the standard priority order (system property > environment variable > properties file); nothing outside `vessel-config` assembles that list by hand. If you need a different set of sources, that's a `vessel-config` extension point, not something wired up from application code today.
