package io.vessel.core;

import java.lang.reflect.Method;

/**
 * Raised when a {@code @Bean} method on a {@code @Configuration} class can't
 * be used to produce a bean — e.g. it returns {@code void}, so there is no
 * type to register it under.
 */
public final class InvalidBeanMethodException extends VesselException {

    private InvalidBeanMethodException(String message) {
        super(message);
    }

    public static InvalidBeanMethodException voidReturnType(Class<?> configurationClass, Method method) {
        var message = ("'%s.%s' cannot be a @Bean — method must return a type, not void%n"
                + "  hint: @Bean determines the bean's type from the method's return type")
                .formatted(configurationClass.getSimpleName(), method.getName());
        return new InvalidBeanMethodException(message);
    }
}
