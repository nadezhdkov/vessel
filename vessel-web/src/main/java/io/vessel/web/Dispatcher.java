package io.vessel.web;

import io.vessel.core.Container;
import io.vessel.http.Handler;
import io.vessel.http.HttpRequest;
import io.vessel.http.HttpResponse;
import io.vessel.http.HttpStatus;
import io.vessel.http.Response;
import io.vessel.web.annotation.ResponseStatus;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Turns a (controller class, method) pair into a {@link Handler}: resolve
 * the controller instance from the container, run {@code @Before} if the
 * controller declares one (a non-null result short-circuits everything
 * else), resolve arguments and invoke the route method, run
 * {@code @After} if declared, and write the result. When the invoked
 * method throws, {@link ExceptionResolver} looks for the most specific
 * {@code @ExceptionHandler} declared on that same controller; when none
 * matches (or the controller declares none at all), a bare 500 is the last
 * resort. If {@code @ExceptionHandler}, {@code @Before}, or {@code @After}
 * itself throws, that same bare-500 fallback applies — a broken hook must
 * not take the whole exchange down with it.
 */
public final class Dispatcher {

    private final Container container;

    public Dispatcher(Container container) {
        this.container = Objects.requireNonNull(container, "container must not be null");
    }

    public Handler handlerFor(Class<?> controllerClass, Method method) {
        Objects.requireNonNull(controllerClass, "controllerClass must not be null");
        Objects.requireNonNull(method, "method must not be null");
        return request -> dispatch(controllerClass, method, request);
    }

    private HttpResponse dispatch(Class<?> controllerClass, Method method, HttpRequest request) {
        var controller = container.get(controllerClass);

        var beforeMethod = MiddlewareResolver.beforeMethodOf(controllerClass);
        if (beforeMethod.isPresent()) {
            var shortCircuit = invokeBefore(controllerClass, controller, beforeMethod.get(), request);
            if (shortCircuit != null) {
                return shortCircuit;
            }
        }

        var args = ArgumentResolver.resolve(method, request);

        HttpResponse response;
        try {
            Object result = method.invoke(controller, args);
            response = writeResult(method, result);
        } catch (InvocationTargetException e) {
            response = handleException(controllerClass, controller, e.getCause());
        } catch (IllegalAccessException e) {
            throw new VesselWebException(
                    "failed to invoke %s.%s: %s".formatted(controllerClass.getSimpleName(), method.getName(), e.getMessage()), e);
        }

        var afterMethod = MiddlewareResolver.afterMethodOf(controllerClass);
        if (afterMethod.isPresent()) {
            response = invokeAfter(controllerClass, controller, afterMethod.get(), request, response);
        }
        return response;
    }

    private HttpResponse invokeBefore(Class<?> controllerClass, Object controller, Method beforeMethod, HttpRequest request) {
        try {
            return (HttpResponse) beforeMethod.invoke(controller, request);
        } catch (Exception failure) {
            return ResponseWriter.write(HttpStatus.INTERNAL_SERVER_ERROR,
                    "the @Before middleware in %s failed: %s"
                            .formatted(controllerClass.getSimpleName(), unwrap(failure).getMessage()));
        }
    }

    private HttpResponse invokeAfter(Class<?> controllerClass, Object controller, Method afterMethod,
                                      HttpRequest request, HttpResponse response) {
        try {
            return (HttpResponse) afterMethod.invoke(controller, request, response);
        } catch (Exception failure) {
            return ResponseWriter.write(HttpStatus.INTERNAL_SERVER_ERROR,
                    "the @After middleware in %s failed: %s"
                            .formatted(controllerClass.getSimpleName(), unwrap(failure).getMessage()));
        }
    }

    private HttpResponse handleException(Class<?> controllerClass, Object controller, Throwable cause) {
        var handlerMethod = ExceptionResolver.resolve(controllerClass, cause.getClass());
        if (handlerMethod.isPresent()) {
            try {
                var handler = handlerMethod.get();
                handler.setAccessible(true);
                var result = handler.invoke(controller, cause);
                return writeResult(handler, result);
            } catch (Exception handlerFailure) {
                return ResponseWriter.write(HttpStatus.INTERNAL_SERVER_ERROR,
                        "the @ExceptionHandler for %s in %s itself failed: %s"
                                .formatted(cause.getClass().getSimpleName(), controllerClass.getSimpleName(),
                                        unwrap(handlerFailure).getMessage()));
            }
        }
        return ResponseWriter.write(HttpStatus.INTERNAL_SERVER_ERROR,
                "unhandled exception in %s: %s".formatted(controllerClass.getSimpleName(), cause.getMessage()));
    }

    /**
     * {@code Method.invoke()} always wraps whatever the invoked method threw
     * in an {@link InvocationTargetException} — every reflective call site in
     * this class (route method, {@code @ExceptionHandler}, {@code @Before},
     * {@code @After}) needs to unwrap it to surface the real failure instead
     * of a generic "InvocationTargetException" message.
     */
    private Throwable unwrap(Throwable failure) {
        return failure instanceof InvocationTargetException invocationFailure && invocationFailure.getCause() != null
                ? invocationFailure.getCause() : failure;
    }

    private HttpResponse writeResult(Method method, Object result) {
        if (result instanceof Response<?> response) {
            return ResponseWriter.write(response.status(), response.body());
        }
        return ResponseWriter.write(defaultStatusOf(method), result);
    }

    private HttpStatus defaultStatusOf(Method method) {
        var annotation = method.getAnnotation(ResponseStatus.class);
        return annotation == null ? HttpStatus.OK : annotation.value();
    }
}
