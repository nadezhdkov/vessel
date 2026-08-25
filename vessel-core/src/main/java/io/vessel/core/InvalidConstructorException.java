package io.vessel.core;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Raised by {@link Container#register(Class)} when a type cannot be
 * registered by constructor resolution alone: it has no public constructor,
 * more than one, or it is abstract/an interface.
 */
public final class InvalidConstructorException extends VesselException {

    private InvalidConstructorException(String message) {
        super(message);
    }

    public static InvalidConstructorException noPublicConstructor(Class<?> type) {
        var message = "'%s' cannot be registered — no public constructor found%n  hint: declare a public constructor to allow injection (M0 does not support reflection over private constructors)"
                .formatted(type.getSimpleName());
        return new InvalidConstructorException(message);
    }

    public static InvalidConstructorException ambiguousConstructors(Class<?> type, List<Constructor<?>> candidates) {
        var signatures = candidates.stream()
                .map(InvalidConstructorException::describeSignature)
                .collect(Collectors.joining(", "));

        var message = "'%s' cannot be registered — found %d public constructors: %s%n  hint: annotate one of them with @Inject to disambiguate, or leave only one public constructor"
                .formatted(type.getSimpleName(), candidates.size(), signatures);
        return new InvalidConstructorException(message);
    }

    public static InvalidConstructorException multipleInjectConstructors(Class<?> type, List<Constructor<?>> injectCandidates) {
        var signatures = injectCandidates.stream()
                .map(InvalidConstructorException::describeSignature)
                .collect(Collectors.joining(", "));

        var message = "'%s' cannot be registered — found %d constructors annotated with @Inject: %s%n  hint: only one constructor per class may be annotated with @Inject"
                .formatted(type.getSimpleName(), injectCandidates.size(), signatures);
        return new InvalidConstructorException(message);
    }

    public static InvalidConstructorException cannotRegisterAbstractType(Class<?> type) {
        var kind = type.isInterface() ? "an interface" : "an abstract class";
        var message = "'%s' is %s — it cannot be registered directly%n  hint: register a concrete implementation (register(Impl.class)) or a ready-made instance (register(%s.class, instance))"
                .formatted(type.getSimpleName(), kind, type.getSimpleName());
        return new InvalidConstructorException(message);
    }

    private static String describeSignature(Constructor<?> constructor) {
        var params = Arrays.stream(constructor.getParameterTypes())
                .map(Class::getSimpleName)
                .collect(Collectors.joining(", "));
        return "%s(%s)".formatted(constructor.getDeclaringClass().getSimpleName(), params);
    }
}
