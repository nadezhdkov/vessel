package io.vessel.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a middleware method that runs after the route method (or after
 * {@code @ExceptionHandler}, if that's what actually produced the response)
 * on the same controller — {@code HttpResponse methodName(HttpRequest request,
 * HttpResponse response)}. Its return value becomes the response actually
 * sent — typically the same {@code response} it was handed, with headers
 * added, or after logging its status. Never runs at all if {@code @Before}
 * short-circuited the exchange. Scoped per controller, same as
 * {@code @ExceptionHandler} (M8). At most one {@code @After} per controller.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface After {
}
