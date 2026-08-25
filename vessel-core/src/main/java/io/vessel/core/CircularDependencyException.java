package io.vessel.core;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Raised when resolving a type would require resolving itself again,
 * directly or through intermediate dependencies.
 */
public final class CircularDependencyException extends VesselException {

    private final List<Class<?>> cyclePath;

    private CircularDependencyException(String message, List<Class<?>> cyclePath) {
        super(message);
        this.cyclePath = cyclePath;
    }

    public static CircularDependencyException forCycle(Deque<Class<?>> path, Class<?> repeated) {
        var fullPath = new ArrayList<>(path);
        fullPath.add(repeated);

        var pathDescription = fullPath.stream()
                .map(Class::getSimpleName)
                .collect(Collectors.joining(" → "));

        var message = "circular dependency detected%n  path: %s".formatted(pathDescription);

        return new CircularDependencyException(message, List.copyOf(fullPath));
    }

    public List<Class<?>> cyclePath() {
        return cyclePath;
    }
}
