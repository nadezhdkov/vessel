package io.vessel.app.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a Vessel application entry point — the argument to
 * {@link io.vessel.app.Vessel#start(Class)}. Carries no attributes of its
 * own; it exists purely so {@code Vessel.start()} can fail fast with a
 * clear message when handed a class nobody meant to bootstrap, rather than
 * silently scanning whatever package it happens to sit in.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Application {
}
