package io.vessel.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Selects whether the container caches one instance for the whole container
 * lifetime ({@link ScopeType#SINGLETON}, the default) or builds a fresh one
 * on every resolution ({@link ScopeType#PROTOTYPE}). Absent = SINGLETON.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Scope {

    ScopeType value() default ScopeType.SINGLETON;
}
