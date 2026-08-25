# vessel-core

The dependency injection container. Depends on nothing else in Vessel — it's a leaf of the module graph.

```java
import io.vessel.core.Container;
```

## Registering and resolving

There are two ways to get a type into the container, and one way to get an instance out.

```java
var container = new Container();

container.register(GreetingService.class);      // resolved via its public constructor
container.register(Clock.class, Clock.systemUTC()); // a ready-made instance

GreetingService service = container.get(GreetingService.class);
```

`register(Class)` requires the class to have exactly one **public** constructor (or exactly one annotated `@Inject`, if there's more than one public constructor — see [Choosing a constructor](#choosing-a-constructor) below) and to be a concrete, non-abstract class. A class with an implicit constructor inherits the class's own visibility — a package-private class with no explicit constructor has a package-private constructor, not a public one, and `register()` will reject it:

```
'GreetingService' cannot be registered — no public constructor found
  hint: declare a public constructor to allow injection (M0 does not support reflection over private constructors)
```

`get(Class)` resolves the whole dependency tree recursively and caches the result — calling `get()` twice for the same singleton-scoped type returns the identical instance.

## Scanning with `@Component`

Instead of registering classes one by one, scan a package:

```java
@Component
public class GreetingService {
    public GreetingService() {}
}
```

```java
container.scan("io.example.app");
```

`scan()` walks every class under the given package (recursively), registering every `@Component`-annotated one. A `@Component` class is also bound as a candidate for every interface it directly implements — resolving by the interface works automatically once at least one implementation is scanned:

```java
public interface Greeter { String greet(String name); }

@Component
public class FriendlyGreeter implements Greeter {
    public FriendlyGreeter() {}
    public String greet(String name) { return "Hello, " + name; }
}
```

```java
container.scan("io.example.app");
Greeter greeter = container.get(Greeter.class); // resolves FriendlyGreeter
```

## Constructor injection

Vessel only supports constructor injection — no field injection, no setter injection, even though `Field#setAccessible` would make it trivial. Constructor injection makes every dependency visible in the signature, lets fields be `final`, and keeps the class testable without the container at all.

```java
@Component
public class OrderController {

    private final OrderService orderService;

    @Inject
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }
}
```

### Choosing a constructor

- **One public constructor** — always used, `@Inject` is optional.
- **Multiple public constructors** — exactly one must be annotated `@Inject`, or registration fails.
- **Zero public constructors** — registration fails immediately with the message shown above.

## Qualifiers and `@Primary`

When more than one `@Component` implements the same interface, resolving by the interface alone is ambiguous unless you break the tie:

```java
public interface PaymentGateway { String charge(int cents); }

@Component
@Qualifier("stripe")
public class StripeGateway implements PaymentGateway { /* ... */ }

@Component
@Qualifier("paypal")
public class PaypalGateway implements PaymentGateway { /* ... */ }
```

```java
@Component
public class CheckoutService {
    @Inject
    public CheckoutService(@Qualifier("stripe") PaymentGateway gateway) { /* ... */ }
}
```

Alternatively, mark one implementation `@Primary` as the default winner when no `@Qualifier` is given on the injection point:

```java
@Component
@Primary
public class StripeGateway implements PaymentGateway { /* ... */ }
```

Two `@Primary` candidates for the same type, or an unresolved ambiguity with neither `@Qualifier` nor `@Primary`, both fail with a message listing every candidate found.

## Injecting `List<T>`

A constructor parameter of type `List<T>` receives **every** registered implementation of `T`, in scan order:

```java
public interface Validator { void validate(Order order); }

@Component
public class OrderProcessor {
    @Inject
    public OrderProcessor(List<Validator> validators) {
        // one entry per @Component implementing Validator
    }
}
```

## Scopes

```java
@Component
@Scope(ScopeType.PROTOTYPE)
public class RequestContext { /* a fresh instance every time it's resolved */ }
```

`SINGLETON` (the default, when `@Scope` is absent) caches one instance for the container's lifetime. `PROTOTYPE` builds a new instance on every resolution — never cached. There is no custom/pluggable scope (like Spring's `@RequestScope`); only these two.

## Lifecycle: `@PostConstruct` / `@PreDestroy`

```java
@Component
public class ConnectionPool {

    @PostConstruct
    public void open() { /* runs right after construction */ }

    @PreDestroy
    public void close() { /* runs when the container closes */ }
}
```

Both are no-arg, `void`, at most one of each per class. `Container implements AutoCloseable` — closing it runs every `@PreDestroy` in **reverse** creation order (the last bean built is the first one torn down, mirroring how a stack unwinds):

```java
try (var container = new Container()) {
    container.scan("io.example.app");
    container.resolveAll();
} // every @PreDestroy runs here, in reverse order
```

## `@Configuration` and `@Bean`

A second way to register a type — useful when you don't own the class (a third-party type) or need to compute the instance rather than just construct it:

```java
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
```

`@Bean` method parameters are resolved by the container exactly like constructor parameters, including `@Qualifier` and `List<T>`. A `@Bean` always wins over a `@Component` registered for the same type in the same `scan()` call — an explicit factory method is a more deliberate registration than a scanned class.

## Lazy injection with `Supplier<T>`

Two beans that depend on each other directly form a real cycle:

```java
@Component
public class EventBus {
    @Inject
    public EventBus(Handler handler) { /* ... */ } // circular!
}

@Component
public class Handler {
    @Inject
    public Handler(EventBus eventBus) { /* ... */ } // circular!
}
```

```
circular dependency detected
  path: EventBus → Handler → EventBus
```

If one side only needs the other *after* it's fully built — not during its own construction — wrap that side in `Supplier<T>` instead:

```java
@Component
public class EventBus {
    private final Supplier<Handler> handlerSupplier;

    @Inject
    public EventBus(Supplier<Handler> handlerSupplier) {
        this.handlerSupplier = handlerSupplier;
    }

    public void publish(Event e) {
        handlerSupplier.get().handle(e); // resolved lazily, here — not during EventBus's own construction
    }
}
```

The `Supplier` doesn't recurse into `Handler` at the point it's created — it hands back a lambda that only calls `container.get(Handler.class)` when actually invoked, by which point `EventBus` has already finished building and is sitting in the singleton cache. Whichever side gets resolved first, the cycle never closes synchronously.

## `@Timed` interception

Wraps a bean in a `java.lang.reflect.Proxy` that logs how long each `@Timed` method takes:

```java
public interface Greeter { String greet(String name); }

@Component
public class SlowGreeter implements Greeter {
    public SlowGreeter() {}

    @Timed
    @Override
    public String greet(String name) {
        return "Hello, " + name;
    }
}
```

```java
Greeter greeter = container.get(Greeter.class); // a Proxy, not a raw SlowGreeter
greeter.greet("Ada");
```

```
[TIMED] SlowGreeter.greet took 1ms
```

**This only works through an interface.** `java.lang.reflect.Proxy` (the only mechanism used — no CGLIB, no ByteBuddy, no bytecode generation) can never intercept a concrete class directly, only calls that go through an interface. Two consequences:

- A `@Timed` method on a class with no interface fails at build time, not silently:
  ```
  'StandaloneTimedComponent' declares @Timed on work but implements no interface
    java.lang.reflect.Proxy can only intercept calls made through an interface, never a concrete class directly
    hint: make 'StandaloneTimedComponent' implement an interface, or remove @Timed
  ```
- Resolving a `@Timed` bean **by its concrete type** instead of by the interface breaks — the cached object is a `Proxy`, which is never `instanceof` the concrete class:
  ```java
  container.get(Greeter.class);      // fine — a Proxy implementing Greeter
  container.get(SlowGreeter.class);  // ClassCastException — the same Proxy isn't a SlowGreeter
  ```

## Error messages

Every failure names what was missing, who asked for it, and the path that got there:

```
no bean of type 'PaymentGateway' registered
  requested by: CheckoutService(constructor, parameter 1)
  path: CheckoutService → PaymentGateway
```

```
circular dependency detected
  path: A → B → C → A
```

## Why `vessel-core` never imports `vessel-config`

`@Value` (in `vessel-config`) needs to resolve a constructor parameter from an `Environment` instead of the bean graph — but `vessel-core` is a leaf module and can never depend on `vessel-config`. The `ParameterValueResolver` interface is the seam that makes this possible without that dependency ever existing:

```java
public interface ParameterValueResolver {
    boolean supports(Parameter parameter);
    Object resolve(Parameter parameter);
}
```

`vessel-core` only knows the shape of "something that can answer for a parameter" — never the `@Value` annotation itself. `vessel-config`'s `Environment` implements this interface; `Vessel.start()` (in `vessel-app`) is what wires the two together via:

```java
container.withParameterValueResolver(environment);
```

See [vessel-config.md](vessel-config.md#value-injection) for the other side of this.

## Eager validation at startup

```java
container.scan("io.example.app");
container.resolveAll(); // builds and caches every registered type, right now
```

`resolveAll()` eagerly resolves every type `scan()`/`register()` found, so a wiring mistake anywhere in the graph fails at this call — not on whichever request happens to touch the broken part first. `Vessel.start()` calls this automatically as part of bootstrap; call it yourself if you're using `Container` directly without `vessel-app`.
