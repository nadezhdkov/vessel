package io.vessel.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a candidate for {@link io.vessel.core.Container#scan(String)}.
 * Registration follows the same constructor-resolution rules as
 * {@link io.vessel.core.Container#register(Class)}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Component {
}
