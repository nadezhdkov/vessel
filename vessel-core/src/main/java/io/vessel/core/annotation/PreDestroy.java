package io.vessel.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the no-arg, void method {@link io.vessel.core.Container#close()}
 * calls, in reverse creation order, for every SINGLETON-scoped instance it
 * still has cached. Never called for PROTOTYPE instances — the container
 * stops tracking those the moment they leave {@code get()}, exactly like
 * Spring. At most one per class.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PreDestroy {
}
