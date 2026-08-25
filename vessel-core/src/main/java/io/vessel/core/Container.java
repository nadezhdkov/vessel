package io.vessel.core;

import io.vessel.core.annotation.Bean;
import io.vessel.core.annotation.Component;
import io.vessel.core.annotation.Configuration;
import io.vessel.core.annotation.Inject;
import io.vessel.core.annotation.PostConstruct;
import io.vessel.core.annotation.PreDestroy;
import io.vessel.core.annotation.Primary;
import io.vessel.core.annotation.Qualifier;
import io.vessel.core.annotation.Scope;
import io.vessel.core.annotation.ScopeType;
import io.vessel.core.annotation.Timed;
import io.vessel.core.graph.BeanDefinition;
import io.vessel.core.graph.CycleDetector;
import io.vessel.core.graph.DependencyGraph;
import io.vessel.core.scan.ClasspathScanner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Resolves instances by walking a dependency graph built from public
 * constructors. M0 requires everything to be registered programmatically;
 * M1 added {@code @Component}/{@code @Inject} and {@link #scan(String)}; M2
 * added {@code @Qualifier}/{@code @Primary}/{@code @Scope} and
 * {@code List<T>} injection; M3 adds {@code @PostConstruct}/{@code @PreDestroy}
 * lifecycle callbacks and {@code @Configuration}/{@code @Bean} factory
 * methods as a second way to register a type, alongside constructors; M10
 * adds {@link ParameterValueResolver} as a plug-in point for resolving a
 * constructor parameter from outside the bean graph entirely (used by
 * {@code vessel-config}'s {@code @Value}), plus {@link #resolveAll()} for
 * eager whole-graph validation at bootstrap; M11 adds {@code Supplier<T>}
 * constructor parameters for lazy resolution — deferring the actual
 * resolution until the supplier is invoked breaks a cycle that would
 * otherwise be a {@link CircularDependencyException} — and {@code @Timed}
 * method interception via a {@code java.lang.reflect.Proxy} wrapped around
 * any built bean that declares it. See docs/vessel-core.md for a guided
 * walkthrough of all of this with examples.
 */
public final class Container implements AutoCloseable {

    private final DependencyGraph graph = new DependencyGraph();
    private final Map<Class<?>, Object> singletons = new LinkedHashMap<>();
    private final Map<Class<?>, Optional<Method>> postConstructCache = new HashMap<>();
    private final Map<Class<?>, Optional<Method>> preDestroyCache = new HashMap<>();
    private ParameterValueResolver parameterValueResolver;
    private boolean closed = false;

    public <T> Container register(Class<T> type) {
        Objects.requireNonNull(type, "type must not be null");
        requireConcreteType(type);

        Constructor<?> constructor = resolvePublicConstructor(type);
        graph.register(new BeanDefinition.FromConstructor(type, constructor, scopeOf(type)));
        singletons.remove(type);
        return this;
    }

    /**
     * Plugs a {@link ParameterValueResolver} into constructor resolution —
     * see that interface's Javadoc for why this exists instead of a direct
     * dependency on whatever module wants to supply parameter values.
     */
    public Container withParameterValueResolver(ParameterValueResolver resolver) {
        this.parameterValueResolver = Objects.requireNonNull(resolver, "resolver must not be null");
        return this;
    }

    public <T> Container register(Class<T> type, T instance) {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(instance, "instance must not be null");

        graph.register(new BeanDefinition.FromInstance(type, instance));
        singletons.put(type, instance);
        invokePostConstructIfPresent(instance);
        return this;
    }

    /**
     * Scans every class under {@code basePackage} (recursively). Every class
     * annotated {@link Component} is registered first; every class annotated
     * {@link Configuration} is processed second, so its {@link Bean} methods
     * always win over a {@code @Component} registered for the same type in
     * this same call — an explicit {@code @Bean} factory method is a more
     * deliberate registration than a scanned class, so it wins.
     * A component is also bound as a candidate for every interface it
     * directly implements; when more than one competes for the same
     * interface, which one wins is decided lazily via {@code @Qualifier} or
     * {@code @Primary} — scanning itself never fails over that.
     */
    public Container scan(String basePackage) {
        Objects.requireNonNull(basePackage, "base package must not be null");

        List<Class<?>> found = ClasspathScanner.scan(basePackage);

        for (Class<?> candidate : found) {
            if (candidate.isAnnotationPresent(Component.class)) {
                registerComponent(candidate);
            }
        }
        for (Class<?> candidate : found) {
            if (candidate.isAnnotationPresent(Configuration.class)) {
                registerConfiguration(candidate);
            }
        }
        return this;
    }

    private void registerComponent(Class<?> type) {
        requireConcreteType(type);

        Constructor<?> constructor = resolvePublicConstructor(type);
        graph.register(new BeanDefinition.FromConstructor(type, constructor, scopeOf(type)));
        singletons.remove(type);
        bindInterfaces(type);
    }

    private void registerConfiguration(Class<?> configurationClass) {
        registerComponent(configurationClass);

        for (Method method : configurationClass.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(Bean.class)) {
                continue;
            }
            if (method.getReturnType() == void.class) {
                throw InvalidBeanMethodException.voidReturnType(configurationClass, method);
            }
            method.setAccessible(true);
            graph.register(new BeanDefinition.FromFactoryMethod(method.getReturnType(), configurationClass, method));
        }
    }

    private void bindInterfaces(Class<?> type) {
        for (Class<?> iface : type.getInterfaces()) {
            graph.bindInterface(iface, type);
        }
    }

    private void requireConcreteType(Class<?> type) {
        if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
            throw InvalidConstructorException.cannotRegisterAbstractType(type);
        }
    }

    private ScopeType scopeOf(Class<?> type) {
        Scope scope = type.getAnnotation(Scope.class);
        return scope == null ? ScopeType.SINGLETON : scope.value();
    }

    private Constructor<?> resolvePublicConstructor(Class<?> type) {
        Constructor<?>[] constructors = type.getConstructors();
        if (constructors.length == 0) {
            throw InvalidConstructorException.noPublicConstructor(type);
        }
        if (constructors.length == 1) {
            return constructors[0];
        }

        List<Constructor<?>> injectCandidates = Arrays.stream(constructors)
                .filter(constructor -> constructor.isAnnotationPresent(Inject.class))
                .toList();

        if (injectCandidates.size() == 1) {
            return injectCandidates.get(0);
        }
        if (injectCandidates.isEmpty()) {
            throw InvalidConstructorException.ambiguousConstructors(type, List.of(constructors));
        }
        throw InvalidConstructorException.multipleInjectConstructors(type, injectCandidates);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> type) {
        Objects.requireNonNull(type, "type must not be null");
        if (closed) {
            throw new VesselException("container closed — cannot resolve '%s' after close()"
                    .formatted(type.getSimpleName()));
        }
        var requestedBy = "container.get(%s.class)".formatted(type.getSimpleName());
        return (T) resolve(type, new ArrayDeque<>(), requestedBy, null);
    }

    /** Every type directly registered so far — via {@link #register}, {@link #scan}, or a {@code @Bean} method. */
    public Set<Class<?>> registeredTypes() {
        return graph.registeredTypes();
    }

    /**
     * Eagerly resolves every registered type — validates the whole graph at
     * startup instead of leaving a wiring mistake to surface on whichever
     * request happens to touch it first. {@code Vessel.start()} calls this
     * right after scanning.
     */
    public Container resolveAll() {
        for (Class<?> type : registeredTypes()) {
            get(type);
        }
        return this;
    }

    private Object resolve(Class<?> type, Deque<Class<?>> path, String requestedBy, String requestedQualifier) {
        if (singletons.containsKey(type)) {
            return singletons.get(type);
        }

        var directDefinition = graph.definitionOf(type);
        if (directDefinition.isPresent()) {
            return build(type, directDefinition.get(), path);
        }

        List<Class<?>> candidates = graph.candidatesFor(type);
        if (!candidates.isEmpty()) {
            Class<?> chosen = chooseCandidate(type, candidates, requestedQualifier, requestedBy, path);
            return resolve(chosen, path, requestedBy, null);
        }

        throw MissingDependencyException.forMissingType(type, requestedBy, path);
    }

    private Object build(Class<?> type, BeanDefinition definition, Deque<Class<?>> path) {
        CycleDetector.checkForCycle(path, type);

        path.addLast(type);
        try {
            Object result = switch (definition) {
                case BeanDefinition.FromInstance fromInstance -> fromInstance.instance();
                case BeanDefinition.FromConstructor fromConstructor -> instantiate(fromConstructor, type, path);
                case BeanDefinition.FromFactoryMethod fromFactoryMethod -> instantiateFromFactory(fromFactoryMethod, path);
            };
            result = applyTimedProxyIfNeeded(result);
            if (!(definition instanceof BeanDefinition.FromConstructor fromConstructor
                    && fromConstructor.scope() == ScopeType.PROTOTYPE)) {
                singletons.put(type, result);
            }
            return result;
        } finally {
            path.removeLast();
        }
    }

    private Class<?> chooseCandidate(Class<?> type, List<Class<?>> candidates, String requestedQualifier,
                                      String requestedBy, Deque<Class<?>> path) {
        if (requestedQualifier != null) {
            List<Class<?>> matches = candidates.stream()
                    .filter(candidate -> requestedQualifier.equals(qualifierNameOf(candidate)))
                    .toList();
            if (matches.size() == 1) {
                return matches.get(0);
            }
            throw UnresolvedQualifierException.forQualifier(type, requestedQualifier, candidates, requestedBy);
        }

        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        List<Class<?>> primaries = candidates.stream().filter(this::isPrimary).toList();
        if (primaries.size() == 1) {
            return primaries.get(0);
        }
        if (primaries.size() > 1) {
            throw AmbiguousComponentException.forMultiplePrimaryCandidates(type, primaries);
        }

        throw AmbiguousComponentException.forNoPrimaryAmongCandidates(type, candidates, requestedBy, path);
    }

    private String qualifierNameOf(Class<?> candidate) {
        Qualifier qualifier = candidate.getAnnotation(Qualifier.class);
        return qualifier == null ? null : qualifier.value();
    }

    private boolean isPrimary(Class<?> candidate) {
        return candidate.isAnnotationPresent(Primary.class);
    }

    private Object instantiate(BeanDefinition.FromConstructor definition, Class<?> type, Deque<Class<?>> path) {
        Constructor<?> constructor = definition.constructor();
        var ownerLabel = type.getSimpleName();
        var requesterPrefix = "%s(constructor".formatted(ownerLabel);
        Object[] args = resolveArguments(constructor, ownerLabel, requesterPrefix, path);

        Object result;
        try {
            result = constructor.newInstance(args);
        } catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
            throw new VesselException(
                    "failed to construct '%s': %s".formatted(ownerLabel, e.getMessage()), e);
        }
        invokePostConstructIfPresent(result);
        return result;
    }

    private Object instantiateFromFactory(BeanDefinition.FromFactoryMethod definition, Deque<Class<?>> path) {
        Class<?> configurationClass = definition.configurationClass();
        Method method = definition.method();
        var ownerLabel = "%s.%s".formatted(configurationClass.getSimpleName(), method.getName());

        Object configInstance = resolve(configurationClass, path,
                "%s (configuration instance for @Bean %s)".formatted(configurationClass.getSimpleName(), method.getName()),
                null);

        var requesterPrefix = "%s(method %s".formatted(configurationClass.getSimpleName(), method.getName());
        Object[] args = resolveArguments(method, ownerLabel, requesterPrefix, path);

        Object result;
        try {
            result = method.invoke(configInstance, args);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new VesselException(
                    "failed to invoke @Bean '%s': %s".formatted(ownerLabel, e.getMessage()), e);
        }
        invokePostConstructIfPresent(result);
        return result;
    }

    private Object[] resolveArguments(Executable executable, String ownerLabel, String requesterPrefix, Deque<Class<?>> path) {
        Class<?>[] paramTypes = executable.getParameterTypes();
        Type[] genericParamTypes = executable.getGenericParameterTypes();
        Annotation[][] paramAnnotations = executable.getParameterAnnotations();
        Parameter[] parameters = executable.getParameters();
        Object[] args = new Object[paramTypes.length];

        for (int i = 0; i < paramTypes.length; i++) {
            if (parameterValueResolver != null && parameterValueResolver.supports(parameters[i])) {
                args[i] = parameterValueResolver.resolve(parameters[i]);
                continue;
            }
            var childRequestedBy = "%s, parameter %d)".formatted(requesterPrefix, i + 1);
            if (paramTypes[i] == List.class) {
                args[i] = resolveList(genericParamTypes[i], ownerLabel, i, path, childRequestedBy);
            } else if (paramTypes[i] == Supplier.class) {
                args[i] = resolveSupplier(genericParamTypes[i], ownerLabel, i);
            } else {
                args[i] = resolve(paramTypes[i], path, childRequestedBy, qualifierValueOf(paramAnnotations[i]));
            }
        }
        return args;
    }

    private String qualifierValueOf(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof Qualifier qualifier) {
                return qualifier.value();
            }
        }
        return null;
    }

    private List<Object> resolveList(Type genericParamType, String ownerLabel, int paramIndex,
                                      Deque<Class<?>> path, String requestedBy) {
        if (!(genericParamType instanceof ParameterizedType parameterized)
                || !(parameterized.getActualTypeArguments()[0] instanceof Class<?> elementType)) {
            throw new VesselException(
                    "'%s' cannot be registered — parameter %d is a List with no concrete element type declared (List<T> requires T to be a plain class/interface)"
                            .formatted(ownerLabel, paramIndex + 1));
        }

        List<Object> resolved = new ArrayList<>();
        for (Class<?> implementation : allImplementationsOf(elementType)) {
            resolved.add(resolve(implementation, path, requestedBy, null));
        }
        return List.copyOf(resolved);
    }

    /**
     * A {@code Supplier<T>} constructor parameter is resolved lazily: instead
     * of recursing into {@code T} right here (which is what makes a genuine
     * cycle a {@link CircularDependencyException}), this hands back a lambda
     * that only calls {@link #get(Class)} when actually invoked — by which
     * point the bean currently under construction has long finished and is
     * sitting in the singleton cache, so the cycle never has a chance to
     * close synchronously. Deliberately calls the public {@link #get(Class)}
     * rather than the internal {@code resolve()} — a deferred call has no
     * meaningful "current path" to share with the one it was created inside.
     */
    private Supplier<Object> resolveSupplier(Type genericParamType, String ownerLabel, int paramIndex) {
        if (!(genericParamType instanceof ParameterizedType parameterized)
                || !(parameterized.getActualTypeArguments()[0] instanceof Class<?> elementType)) {
            throw new VesselException(
                    "'%s' cannot be registered — parameter %d is a Supplier with no concrete element type declared (Supplier<T> requires T to be a plain class/interface)"
                            .formatted(ownerLabel, paramIndex + 1));
        }
        return () -> get(elementType);
    }

    private List<Class<?>> allImplementationsOf(Class<?> elementType) {
        List<Class<?>> candidates = graph.candidatesFor(elementType);
        if (!candidates.isEmpty()) {
            return candidates;
        }
        return graph.isRegistered(elementType) ? List.of(elementType) : List.of();
    }

    /**
     * Wraps a freshly built bean in a {@link Proxy} implementing every
     * interface its runtime class declares, if — and only if — that class
     * has at least one {@code @Timed} method. Runs after
     * {@code @PostConstruct} (called earlier, inside {@code instantiate}/
     * {@code instantiateFromFactory}) so lifecycle callbacks always see the
     * real instance, never the proxy. A caller that resolves this bean by
     * its concrete type (rather than through the interface {@code @Timed}
     * requires) gets back an object that fails a {@code ClassCastException}
     * the moment it's used as that concrete type — a real, deliberate
     * limitation of "simple" JDK dynamic proxies (no CGLIB/ByteBuddy), not a
     * bug. See docs/vessel-core.md for the full explanation.
     */
    private Object applyTimedProxyIfNeeded(Object result) {
        if (result == null) {
            return null;
        }
        Class<?> concreteType = result.getClass();
        List<Method> timedMethods = timedMethodsOf(concreteType);
        if (timedMethods.isEmpty()) {
            return result;
        }
        validateTimedMethods(concreteType, timedMethods);
        return Proxy.newProxyInstance(concreteType.getClassLoader(), concreteType.getInterfaces(),
                timedInvocationHandler(concreteType, result));
    }

    private List<Method> timedMethodsOf(Class<?> concreteType) {
        return Arrays.stream(concreteType.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Timed.class))
                .toList();
    }

    private void validateTimedMethods(Class<?> concreteType, List<Method> timedMethods) {
        Class<?>[] interfaces = concreteType.getInterfaces();
        if (interfaces.length == 0) {
            throw InvalidTimedMethodException.requiresAnInterface(concreteType, timedMethods);
        }
        for (Method method : timedMethods) {
            boolean declaredOnAnInterface = Arrays.stream(interfaces)
                    .flatMap(iface -> Arrays.stream(iface.getMethods()))
                    .anyMatch(ifaceMethod -> ifaceMethod.getName().equals(method.getName())
                            && Arrays.equals(ifaceMethod.getParameterTypes(), method.getParameterTypes()));
            if (!declaredOnAnInterface) {
                throw InvalidTimedMethodException.methodNotOnAnyInterface(concreteType, method);
            }
        }
    }

    private InvocationHandler timedInvocationHandler(Class<?> concreteType, Object target) {
        return (proxy, method, args) -> {
            Method implMethod = concreteType.getMethod(method.getName(), method.getParameterTypes());
            boolean timed = implMethod.isAnnotationPresent(Timed.class);
            long startNanos = timed ? System.nanoTime() : 0;
            try {
                return method.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            } finally {
                if (timed) {
                    long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;
                    System.out.println("[TIMED] %s.%s took %dms"
                            .formatted(concreteType.getSimpleName(), method.getName(), elapsedMillis));
                }
            }
        };
    }

    private void invokePostConstructIfPresent(Object instance) {
        lifecycleMethod(instance.getClass(), PostConstruct.class, postConstructCache)
                .ifPresent(method -> invokeLifecycleMethod(instance, method, "PostConstruct"));
    }

    private void invokePreDestroyIfPresent(Object instance) {
        lifecycleMethod(instance.getClass(), PreDestroy.class, preDestroyCache)
                .ifPresent(method -> invokeLifecycleMethod(instance, method, "PreDestroy"));
    }

    private Optional<Method> lifecycleMethod(Class<?> type, Class<? extends Annotation> annotation,
                                              Map<Class<?>, Optional<Method>> cache) {
        return cache.computeIfAbsent(type, t -> resolveLifecycleMethod(t, annotation));
    }

    private Optional<Method> resolveLifecycleMethod(Class<?> type, Class<? extends Annotation> annotation) {
        List<Method> found = Arrays.stream(type.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(annotation))
                .toList();

        if (found.isEmpty()) {
            return Optional.empty();
        }
        if (found.size() > 1) {
            throw InvalidLifecycleMethodException.multipleMethods(type, annotation.getSimpleName(), found);
        }

        Method method = found.get(0);
        if (method.getParameterCount() != 0 || method.getReturnType() != void.class) {
            throw InvalidLifecycleMethodException.invalidSignature(type, method, annotation.getSimpleName());
        }
        method.setAccessible(true);
        return Optional.of(method);
    }

    private void invokeLifecycleMethod(Object instance, Method method, String annotationSimpleName) {
        try {
            method.invoke(instance);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new VesselException(
                    "failed to invoke @%s on '%s': %s"
                            .formatted(annotationSimpleName, instance.getClass().getSimpleName(), e.getMessage()), e);
        }
    }

    @Override
    public void close() {
        var destructionOrder = new ArrayList<>(singletons.values());
        Collections.reverse(destructionOrder);
        for (Object instance : destructionOrder) {
            invokePreDestroyIfPresent(instance);
        }
        closed = true;
        singletons.clear();
    }
}
