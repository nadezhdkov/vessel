package io.vessel.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method on a {@code @RestController} as the handler for exceptions
 * thrown by that same controller's route methods — there is no
 * {@code @ControllerAdvice} equivalent; handlers are scoped to the
 * controller that declares them. When more than one handler on a
 * controller could apply, the one declaring the most specific exception
 * type wins.
 *
 * <p>{@link #value()} may be left at its default — the handler then targets
 * whatever type its single parameter declares (which must be a
 * {@link Throwable} subtype either way).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExceptionHandler {

    Class<? extends Throwable> value() default Throwable.class;
}
