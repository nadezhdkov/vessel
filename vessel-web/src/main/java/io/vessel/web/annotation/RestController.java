package io.vessel.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an HTTP controller. Implies {@code @Component} — {@link io.vessel.web.ControllerScanner}
 * registers every {@code @RestController} it finds directly into the
 * {@code Container} via {@code register(Class)}, so it is resolvable and
 * injectable exactly like any other component, without needing a second
 * annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RestController {
}
