package io.vessel.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class whose {@link Bean}-annotated methods produce instances
 * programmatically. Discovered by {@link io.vessel.core.Container#scan(String)}
 * independently of {@link Component} — a class doesn't need both.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Configuration {
}
