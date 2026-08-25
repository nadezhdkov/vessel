package io.vessel.web.annotation;

import io.vessel.http.HttpStatus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Overrides the default status ({@code HttpStatus.OK}) a controller method
 * responds with when it returns a plain body rather than a
 * {@code Response<T>}. Ignored when the method returns a {@code Response<T>}
 * directly — that object's own status always wins, since it's the more
 * specific, per-invocation source of truth.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ResponseStatus {

    HttpStatus value();
}
