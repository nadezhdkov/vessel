package io.vessel.config;

import io.vessel.core.VesselException;

import java.util.List;

/**
 * Every failure raised while reading, merging or converting configuration
 * values. Always names the property key and, for conversion failures, the
 * raw value that could not be converted — never just "invalid value".
 */
public final class VesselConfigException extends VesselException {

    public VesselConfigException(String message) {
        super(message);
    }

    public VesselConfigException(String message, Throwable cause) {
        super(message, cause);
    }

    static VesselConfigException missingProperty(String key) {
        return new VesselConfigException(
                "no property named '%s' found — checked system properties, environment variables and the properties file, in that priority order"
                        .formatted(key));
    }

    static VesselConfigException malformedPlaceholder(String expression) {
        return new VesselConfigException(
                "'%s' is not a valid @Value placeholder — expected the form '${property.key}'".formatted(expression));
    }

    static VesselConfigException conversionFailed(String key, String rawValue, Class<?> targetType, Throwable cause) {
        return new VesselConfigException(
                "property '%s' has value '%s', which cannot be converted to %s: %s"
                        .formatted(key, rawValue, targetType.getSimpleName(), cause.getMessage()), cause);
    }

    static VesselConfigException unsupportedType(String key, Class<?> targetType) {
        return new VesselConfigException(
                "property '%s' cannot be converted to unsupported type '%s' — supported types are String, Integer, Long, Double, Boolean, Duration and enums"
                        .formatted(key, targetType.getSimpleName()));
    }

    static VesselConfigException invalidEnumValue(String key, String rawValue, Class<?> enumType, List<String> validValues) {
        return new VesselConfigException(
                "property '%s' has value '%s', which is not a constant of enum %s — valid values are: %s"
                        .formatted(key, rawValue, enumType.getSimpleName(), String.join(", ", validValues)));
    }

    static VesselConfigException invalidDuration(String key, String rawValue) {
        return new VesselConfigException(
                "property '%s' has value '%s', which is not a valid duration — expected a number optionally followed by a unit suffix (ns, us, ms, s, m, h, d), e.g. '30s' or '500ms'"
                        .formatted(key, rawValue));
    }

    static VesselConfigException unreadablePropertiesFile(String resourceName, Throwable cause) {
        return new VesselConfigException(
                "found '%s' on the classpath but could not read it as a properties file: %s"
                        .formatted(resourceName, cause.getMessage()), cause);
    }
}
