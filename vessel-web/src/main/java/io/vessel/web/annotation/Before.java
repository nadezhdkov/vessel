package io.vessel.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a middleware method that runs before every route method on the same
 * controller — {@code HttpResponse methodName(HttpRequest request)}.
 * Returning {@code null} lets the request continue to the route method (or
 * to {@code @After}, if present); returning a non-null {@code HttpResponse}
 * short-circuits the whole exchange with that response — the route method,
 * and any {@code @After}, never run. Scoped per controller, same as
 * {@code @ExceptionHandler} — no shared/global middleware equivalent to
 * Spring's {@code @ControllerAdvice}. At most one {@code @Before} per
 * controller.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Before {
}
