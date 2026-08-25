package io.vessel.core;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Raised when {@link Container#get(Class)} needs a type that was never
 * registered — either the requested type itself, or a dependency somewhere
 * in its constructor tree.
 */
public final class MissingDependencyException extends VesselException {

    private final Class<?> missingType;
    private final String requestedBy;
    private final List<Class<?>> path;

    private MissingDependencyException(String message, Class<?> missingType, String requestedBy, List<Class<?>> path) {
        super(message);
        this.missingType = missingType;
        this.requestedBy = requestedBy;
        this.path = path;
    }

    public static MissingDependencyException forMissingType(Class<?> missingType, String requestedBy, Deque<Class<?>> path) {
        var fullPath = new ArrayList<>(path);
        fullPath.add(missingType);

        var pathDescription = fullPath.stream()
                .map(Class::getSimpleName)
                .collect(Collectors.joining(" → "));

        var message = "no bean of type '%s' registered%n  requested by: %s%n  path: %s"
                .formatted(missingType.getSimpleName(), requestedBy, pathDescription);

        return new MissingDependencyException(message, missingType, requestedBy, List.copyOf(fullPath));
    }

    public Class<?> missingType() {
        return missingType;
    }

    public String requestedBy() {
        return requestedBy;
    }

    public List<Class<?>> path() {
        return path;
    }
}
