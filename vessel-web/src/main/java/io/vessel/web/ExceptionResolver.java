package io.vessel.web;

import io.vessel.web.annotation.ExceptionHandler;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Finds, for a given controller and exception, the most specific
 * {@code @ExceptionHandler} method declared on that same controller class.
 * "Most specific" falls out for free from walking the exception's own
 * class hierarchy upward and returning the first exact match — since
 * exception classes form a single-inheritance chain (no diamonds), the
 * first match found on that walk is always the closest ancestor with a
 * registered handler.
 */
public final class ExceptionResolver {

    private ExceptionResolver() {
    }

    /** Fails fast on a malformed or duplicate {@code @ExceptionHandler} — meant to run once at scan time. */
    public static void validate(Class<?> controllerClass) {
        handlersOf(controllerClass);
    }

    public static Optional<Method> resolve(Class<?> controllerClass, Class<? extends Throwable> exceptionType) {
        var handlers = handlersOf(controllerClass);
        Class<?> current = exceptionType;
        while (current != null) {
            var handler = handlers.get(current);
            if (handler != null) {
                return Optional.of(handler);
            }
            current = current.getSuperclass();
        }
        return Optional.empty();
    }

    private static Map<Class<?>, Method> handlersOf(Class<?> controllerClass) {
        var handlers = new HashMap<Class<?>, Method>();
        for (Method method : controllerClass.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(ExceptionHandler.class)) {
                continue;
            }
            var handledType = handledTypeOf(method);
            var existing = handlers.put(handledType, method);
            if (existing != null) {
                throw new VesselWebException(
                        "duplicate @ExceptionHandler for type '%s' on '%s': '%s' and '%s' both declare it"
                                .formatted(handledType.getSimpleName(), controllerClass.getSimpleName(),
                                        existing.getName(), method.getName()));
            }
        }
        return handlers;
    }

    private static Class<?> handledTypeOf(Method method) {
        var paramTypes = method.getParameterTypes();
        if (paramTypes.length != 1 || !Throwable.class.isAssignableFrom(paramTypes[0])) {
            throw new VesselWebException(
                    "@ExceptionHandler method '%s.%s' must declare exactly one parameter of a Throwable subtype"
                            .formatted(method.getDeclaringClass().getSimpleName(), method.getName()));
        }

        var declaredType = method.getAnnotation(ExceptionHandler.class).value();
        if (declaredType == Throwable.class) {
            return paramTypes[0];
        }
        if (!paramTypes[0].isAssignableFrom(declaredType)) {
            throw new VesselWebException(
                    "@ExceptionHandler(%s.class) on '%s.%s' is incompatible with its parameter type '%s' — the parameter must be able to accept a '%s'"
                            .formatted(declaredType.getSimpleName(), method.getDeclaringClass().getSimpleName(),
                                    method.getName(), paramTypes[0].getSimpleName(), declaredType.getSimpleName()));
        }
        return declaredType;
    }
}
