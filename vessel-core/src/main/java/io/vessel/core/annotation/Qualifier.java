package io.vessel.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Names an implementation ({@code @Qualifier("stripe") class StripeGateway ...})
 * or, on a constructor parameter, requests a specific named implementation
 * when more than one {@code @Component} implements the same interface.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.PARAMETER})
public @interface Qualifier {

    String value();
}
