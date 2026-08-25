package io.vessel.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the no-arg, void method the container calls right after building an
 * instance it owns — via constructor, via a {@link Bean} factory method, or
 * right after {@link io.vessel.core.Container#register(Class, Object)}. At
 * most one per class.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PostConstruct {
}
