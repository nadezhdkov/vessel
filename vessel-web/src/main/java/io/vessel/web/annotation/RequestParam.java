package io.vessel.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds a method parameter to a query string parameter. Same name-fallback
 * rule as {@link PathVariable}: an empty {@link #value()} falls back to the
 * parameter's own name.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface RequestParam {

    String value() default "";
}
