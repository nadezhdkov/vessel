package io.vessel.core;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Raised when resolving an interface with more than one {@code @Component}
 * implementation can't be settled: no {@code @Qualifier} was requested and
 * either no candidate is {@code @Primary}, or more than one is.
 */
public final class AmbiguousComponentException extends VesselException {

    private final Class<?> competingType;
    private final List<Class<?>> candidates;

    private AmbiguousComponentException(String message, Class<?> competingType, List<Class<?>> candidates) {
        super(message);
        this.competingType = competingType;
        this.candidates = candidates;
    }

    public static AmbiguousComponentException forNoPrimaryAmongCandidates(
            Class<?> type, List<Class<?>> candidates, String requestedBy, Deque<Class<?>> path) {
        var names = candidates.stream().map(Class::getSimpleName).collect(Collectors.joining(", "));
        var message = ("more than one @Component implements '%s' and none is marked @Primary: %s%n"
                + "  requested by: %s%n"
                + "  path: %s%n"
                + "  hint: annotate one of the candidates with @Primary, or use @Qualifier(\"name\") at the injection point to choose explicitly")
                .formatted(type.getSimpleName(), names, requestedBy, describePath(path, type));
        return new AmbiguousComponentException(message, type, candidates);
    }

    public static AmbiguousComponentException forMultiplePrimaryCandidates(Class<?> type, List<Class<?>> primaryCandidates) {
        var names = primaryCandidates.stream().map(Class::getSimpleName).collect(Collectors.joining(", "));
        var message = ("more than one @Component implementing '%s' is marked @Primary: %s%n"
                + "  hint: only one @Primary is allowed per type")
                .formatted(type.getSimpleName(), names);
        return new AmbiguousComponentException(message, type, primaryCandidates);
    }

    public Class<?> competingType() {
        return competingType;
    }

    public List<Class<?>> candidates() {
        return candidates;
    }

    private static String describePath(Deque<Class<?>> path, Class<?> type) {
        var fullPath = new ArrayList<>(path);
        fullPath.add(type);
        return fullPath.stream().map(Class::getSimpleName).collect(Collectors.joining(" → "));
    }
}
