package io.vessel.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method on a {@link Configuration} class as a bean factory. The
 * method's return type is the registered type — it may be a concrete class
 * or an interface. Parameters are resolved by the container exactly like
 * constructor parameters (including {@link Qualifier} and {@code List<T>}).
 * A {@code @Bean} definition always overrides a {@code @Component} for the
 * same type registered by the same {@link io.vessel.core.Container#scan(String)}
 * call — an explicit factory method is a more deliberate registration than
 * a scanned class, so it wins.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Bean {
}
