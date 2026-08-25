package io.vessel.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method for duration logging via a {@code java.lang.reflect.Proxy}
 * the {@code Container} wraps around any bean that declares it. Only works
 * on a method reachable through an interface the bean implements — JDK
 * dynamic proxies can never intercept a concrete class directly, only
 * interfaces. See docs/vessel-core.md for a full walkthrough.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Timed {
}
