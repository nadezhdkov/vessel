package io.vessel.core;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Raised when a {@code @PostConstruct}/{@code @PreDestroy} method can't be
 * used as declared: more than one on the same class, or a signature other
 * than no-args/{@code void}.
 */
public final class InvalidLifecycleMethodException extends VesselException {

    private InvalidLifecycleMethodException(String message) {
        super(message);
    }

    public static InvalidLifecycleMethodException multipleMethods(Class<?> type, String annotationSimpleName, List<Method> methods) {
        var names = methods.stream().map(Method::getName).collect(Collectors.joining(", "));
        var message = ("'%s' cannot be used — found %d methods annotated with @%s: %s%n"
                + "  hint: only one @%s method is allowed per class")
                .formatted(type.getSimpleName(), methods.size(), annotationSimpleName, names, annotationSimpleName);
        return new InvalidLifecycleMethodException(message);
    }

    public static InvalidLifecycleMethodException invalidSignature(Class<?> type, Method method, String annotationSimpleName) {
        var message = ("'%s' cannot be used — method '%s' annotated with @%s must be void and take no parameters%n"
                + "  found: %s %s(%d parameter(s))")
                .formatted(type.getSimpleName(), method.getName(), annotationSimpleName,
                        method.getReturnType().getSimpleName(), method.getName(), method.getParameterCount());
        return new InvalidLifecycleMethodException(message);
    }
}
