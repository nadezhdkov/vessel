package io.vessel.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds a method parameter to a {@code {name}} segment of the route path.
 * When {@link #value()} is left empty, the parameter's own name is used
 * instead — which requires compiling with {@code -parameters} (already on
 * for every Vessel module) so reflection can see real names instead of
 * {@code arg0}, {@code arg1}, etc.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface PathVariable {

    String value() default "";
}
