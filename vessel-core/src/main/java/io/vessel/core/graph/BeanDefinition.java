package io.vessel.core.graph;

import io.vessel.core.annotation.ScopeType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Describes how a type is produced by the container: from a pre-built
 * instance, by invoking a public constructor whose parameters are resolved
 * recursively, or by invoking a {@code @Bean} factory method on a
 * {@code @Configuration} instance. Interface-to-implementation indirection
 * (M1's {@code Alias}) is not part of this model as of M2 — a type can have
 * several competing implementations, tracked separately by
 * {@link DependencyGraph#bindInterface(Class, Class)}, since resolving which
 * one wins is a per-call decision (@Qualifier/@Primary), not a fixed binding.
 */
public sealed interface BeanDefinition permits BeanDefinition.FromInstance, BeanDefinition.FromConstructor, BeanDefinition.FromFactoryMethod {

    Class<?> type();

    record FromInstance(Class<?> type, Object instance) implements BeanDefinition {
    }

    record FromConstructor(Class<?> type, Constructor<?> constructor, ScopeType scope) implements BeanDefinition {
    }

    /**
     * {@code @Bean} factory methods are always SINGLETON — {@code @Scope}
     * only applies to a {@code @Component}'s own class-level annotation, not
     * to a factory method, so there is nothing here to read it from yet.
     */
    record FromFactoryMethod(Class<?> type, Class<?> configurationClass, Method method) implements BeanDefinition {
    }
}
