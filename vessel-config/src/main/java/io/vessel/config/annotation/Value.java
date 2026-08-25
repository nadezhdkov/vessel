package io.vessel.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a constructor parameter to be resolved from an {@code Environment}
 * instead of the DI container's bean graph. Only {@link ElementType#PARAMETER}
 * is a valid target — Vessel only supports constructor injection (see
 * CLAUDE.md's "constructor injection only" rule), so there is no field-level
 * equivalent to allow.
 *
 * <p>{@code value()} must be a placeholder of the form {@code "${property.key}"}.
 * No SpEL, no nested placeholders, no default-value syntax (no
 * {@code "${key:default}"}) — {@link io.vessel.config.Environment}'s
 * three-argument {@code get(key, type, default)} already covers "value with
 * a fallback" from the Java side. See docs/vessel-config.md.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Value {
    String value();
}
