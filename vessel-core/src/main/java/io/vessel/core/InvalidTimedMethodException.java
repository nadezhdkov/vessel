package io.vessel.core;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Raised when a {@code @Timed} method can never actually be intercepted:
 * the declaring class implements no interface at all (nothing for {@code
 * java.lang.reflect.Proxy} to proxy), or the method isn't part of any
 * interface the class does implement (a proxy call can only ever reach
 * methods declared on the interfaces it was built from).
 */
public final class InvalidTimedMethodException extends VesselException {

    private InvalidTimedMethodException(String message) {
        super(message);
    }

    static InvalidTimedMethodException requiresAnInterface(Class<?> type, List<Method> timedMethods) {
        var names = timedMethods.stream().map(Method::getName).collect(Collectors.joining(", "));
        var message = ("'%s' declares @Timed on %s but implements no interface%n"
                + "  java.lang.reflect.Proxy can only intercept calls made through an interface, never a concrete class directly%n"
                + "  hint: make '%s' implement an interface, or remove @Timed")
                .formatted(type.getSimpleName(), names, type.getSimpleName());
        return new InvalidTimedMethodException(message);
    }

    static InvalidTimedMethodException methodNotOnAnyInterface(Class<?> type, Method method) {
        var interfaceNames = java.util.Arrays.stream(type.getInterfaces())
                .map(Class::getSimpleName)
                .collect(Collectors.joining(", "));
        var message = ("'%s' declares @Timed on '%s', but that method isn't part of any interface '%s' implements (%s)%n"
                + "  a proxy can only intercept calls that go through one of those interfaces — this @Timed would silently never fire%n"
                + "  hint: declare '%s' on one of those interfaces too, or remove @Timed")
                .formatted(type.getSimpleName(), method.getName(), type.getSimpleName(), interfaceNames, method.getName());
        return new InvalidTimedMethodException(message);
    }
}
