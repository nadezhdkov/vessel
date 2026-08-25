package io.vessel.web;

import io.vessel.http.HttpRequest;
import io.vessel.http.HttpResponse;
import io.vessel.web.annotation.After;
import io.vessel.web.annotation.Before;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Finds and validates a controller's {@code @Before}/{@code @After} methods,
 * mirroring {@link ExceptionResolver}'s shape: eager validation at scan
 * time (called from {@link ControllerScanner}), lazy lookup at dispatch
 * time. Unlike {@code @ExceptionHandler} (keyed by exception type, so
 * multiple are fine), there is exactly one meaningful slot for "runs before"
 * and one for "runs after" per controller — allowing more than one of
 * either would require deciding an execution order between them, which
 * isn't a problem worth solving without a concrete use case, so it's simply
 * not allowed.
 */
public final class MiddlewareResolver {

    private MiddlewareResolver() {
    }

    public static void validate(Class<?> controllerClass) {
        beforeMethodOf(controllerClass);
        afterMethodOf(controllerClass);
    }

    public static Optional<Method> beforeMethodOf(Class<?> controllerClass) {
        var candidates = methodsAnnotatedWith(controllerClass, Before.class);
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        if (candidates.size() > 1) {
            throw new VesselWebException(
                    "duplicate @Before on '%s': only one @Before method is allowed per controller, found: %s"
                            .formatted(controllerClass.getSimpleName(), namesOf(candidates)));
        }
        var method = candidates.get(0);
        var params = method.getParameterTypes();
        if (params.length != 1 || params[0] != HttpRequest.class || method.getReturnType() != HttpResponse.class) {
            throw new VesselWebException(
                    "@Before method '%s.%s' must have the signature 'HttpResponse %s(HttpRequest)' — found '%s %s(%s)'"
                            .formatted(controllerClass.getSimpleName(), method.getName(), method.getName(),
                                    method.getReturnType().getSimpleName(), method.getName(), simpleNamesOf(params)));
        }
        method.setAccessible(true);
        return Optional.of(method);
    }

    public static Optional<Method> afterMethodOf(Class<?> controllerClass) {
        var candidates = methodsAnnotatedWith(controllerClass, After.class);
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        if (candidates.size() > 1) {
            throw new VesselWebException(
                    "duplicate @After on '%s': only one @After method is allowed per controller, found: %s"
                            .formatted(controllerClass.getSimpleName(), namesOf(candidates)));
        }
        var method = candidates.get(0);
        var params = method.getParameterTypes();
        if (params.length != 2 || params[0] != HttpRequest.class || params[1] != HttpResponse.class
                || method.getReturnType() != HttpResponse.class) {
            throw new VesselWebException(
                    "@After method '%s.%s' must have the signature 'HttpResponse %s(HttpRequest, HttpResponse)' — found '%s %s(%s)'"
                            .formatted(controllerClass.getSimpleName(), method.getName(), method.getName(),
                                    method.getReturnType().getSimpleName(), method.getName(), simpleNamesOf(params)));
        }
        method.setAccessible(true);
        return Optional.of(method);
    }

    private static List<Method> methodsAnnotatedWith(Class<?> controllerClass, Class<? extends java.lang.annotation.Annotation> annotation) {
        return Arrays.stream(controllerClass.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(annotation))
                .toList();
    }

    private static String namesOf(List<Method> methods) {
        return methods.stream().map(Method::getName).collect(Collectors.joining(", "));
    }

    private static String simpleNamesOf(Class<?>[] types) {
        return Arrays.stream(types).map(Class::getSimpleName).collect(Collectors.joining(", "));
    }
}
