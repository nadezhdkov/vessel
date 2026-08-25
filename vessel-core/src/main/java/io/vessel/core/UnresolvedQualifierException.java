package io.vessel.core;

import io.vessel.core.annotation.Qualifier;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Raised when a constructor parameter requests
 * {@code @Qualifier("name")} but no registered implementation of that
 * interface carries a matching {@code @Qualifier} on its class.
 */
public final class UnresolvedQualifierException extends VesselException {

    private final Class<?> type;
    private final String requestedQualifier;

    private UnresolvedQualifierException(String message, Class<?> type, String requestedQualifier) {
        super(message);
        this.type = type;
        this.requestedQualifier = requestedQualifier;
    }

    public static UnresolvedQualifierException forQualifier(
            Class<?> type, String requestedQualifier, List<Class<?>> candidates, String requestedBy) {
        var described = candidates.stream()
                .map(UnresolvedQualifierException::describeCandidate)
                .collect(Collectors.joining(", "));
        var message = ("no @Component implementing '%s' has @Qualifier(\"%s\")%n"
                + "  available candidates: %s%n"
                + "  requested by: %s")
                .formatted(type.getSimpleName(), requestedQualifier, described, requestedBy);
        return new UnresolvedQualifierException(message, type, requestedQualifier);
    }

    private static String describeCandidate(Class<?> candidate) {
        var qualifier = candidate.getAnnotation(Qualifier.class);
        return qualifier == null
                ? candidate.getSimpleName()
                : "%s (@Qualifier(\"%s\"))".formatted(candidate.getSimpleName(), qualifier.value());
    }

    public Class<?> type() {
        return type;
    }

    public String requestedQualifier() {
        return requestedQualifier;
    }
}
