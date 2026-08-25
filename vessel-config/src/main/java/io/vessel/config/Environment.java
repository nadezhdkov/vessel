package io.vessel.config;

import io.vessel.core.ParameterValueResolver;
import io.vessel.config.annotation.Value;

import java.lang.reflect.Parameter;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Merges an ordered list of {@link PropertySource}s — first match wins — and
 * converts the raw {@code String} each one hands back into the type a caller
 * actually wants. Built by {@link Configuration#load()}; the multi-source
 * constructor stays package-private because nothing outside this package
 * should assemble the source list by hand — {@link Configuration} owns the
 * standard priority order (system property {@literal >} environment variable
 * {@literal >} properties file). Also implements {@link ParameterValueResolver}
 * so {@code Vessel.start()} (M10) can plug it directly into {@code Container}
 * via {@code withParameterValueResolver} — that's the wiring that turns
 * {@link #resolve(Parameter)} from "tested in isolation" into "what actually
 * runs a real {@code @Value}-annotated constructor parameter".
 */
public final class Environment implements ParameterValueResolver {

    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+)(ns|us|ms|s|m|h|d)?");

    private final List<PropertySource> sources;

    Environment(List<PropertySource> sourcesInPriorityOrder) {
        this.sources = List.copyOf(Objects.requireNonNull(sourcesInPriorityOrder, "sources must not be null"));
    }

    public Optional<String> get(String key) {
        Objects.requireNonNull(key, "key must not be null");
        for (PropertySource source : sources) {
            var value = source.get(key);
            if (value.isPresent()) {
                return value;
            }
        }
        return Optional.empty();
    }

    public String get(String key, String defaultValue) {
        return get(key).orElse(defaultValue);
    }

    /**
     * Hot-reload without restarting the process (M11): re-reads every
     * source's backing state via {@link PropertySource#refresh()} — for the
     * standard properties-file source, that means re-reading the file from
     * the classpath, so a change written to it after this {@code Environment}
     * was built becomes visible to the next {@link #get(String)} call. This
     * only affects code that queries {@code Environment} directly at
     * request/call time — a value already baked into a bean via {@code
     * @Value} at construction time was resolved once and stays whatever it
     * was; {@code reload()} does not re-run constructor injection. See
     * docs/vessel-config.md for that boundary.
     */
    public void reload() {
        for (PropertySource source : sources) {
            source.refresh();
        }
    }

    public String require(String key) {
        return get(key).orElseThrow(() -> VesselConfigException.missingProperty(key));
    }

    public <T> T get(String key, Class<T> type) {
        return convert(key, require(key), type);
    }

    public <T> T get(String key, Class<T> type, T defaultValue) {
        return get(key).map(raw -> convert(key, raw, type)).orElse(defaultValue);
    }

    @Override
    public boolean supports(Parameter parameter) {
        return parameter.isAnnotationPresent(Value.class);
    }

    /**
     * Resolves a single {@code @Value}-annotated constructor parameter: reads
     * its placeholder, looks the key up, and converts to the parameter's
     * declared type (primitives are widened to their wrapper automatically).
     */
    @Override
    public Object resolve(Parameter parameter) {
        Objects.requireNonNull(parameter, "parameter must not be null");
        Value annotation = parameter.getAnnotation(Value.class);
        if (annotation == null) {
            throw new VesselConfigException(
                    "parameter '%s' has no @Value annotation to resolve".formatted(parameter));
        }
        String key = extractKey(annotation.value());
        return get(key, wrapperTypeOf(parameter.getType()));
    }

    private String extractKey(String expression) {
        if (expression.length() < 4 || !expression.startsWith("${") || !expression.endsWith("}")) {
            throw VesselConfigException.malformedPlaceholder(expression);
        }
        return expression.substring(2, expression.length() - 1);
    }

    private Class<?> wrapperTypeOf(Class<?> type) {
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == double.class) return Double.class;
        if (type == boolean.class) return Boolean.class;
        return type;
    }

    @SuppressWarnings("unchecked")
    private <T> T convert(String key, String rawValue, Class<T> type) {
        String trimmed = rawValue.strip();
        try {
            if (type == String.class) {
                return (T) rawValue;
            }
            if (type == Integer.class) {
                return (T) Integer.valueOf(Integer.parseInt(trimmed));
            }
            if (type == Long.class) {
                return (T) Long.valueOf(Long.parseLong(trimmed));
            }
            if (type == Double.class) {
                return (T) Double.valueOf(Double.parseDouble(trimmed));
            }
            if (type == Boolean.class) {
                return (T) parseBoolean(key, trimmed);
            }
            if (type == Duration.class) {
                return (T) parseDuration(key, trimmed);
            }
            if (type.isEnum()) {
                return (T) parseEnum(key, trimmed, type);
            }
        } catch (NumberFormatException e) {
            throw VesselConfigException.conversionFailed(key, rawValue, type, e);
        }
        throw VesselConfigException.unsupportedType(key, type);
    }

    private Boolean parseBoolean(String key, String trimmed) {
        if (trimmed.equalsIgnoreCase("true")) {
            return Boolean.TRUE;
        }
        if (trimmed.equalsIgnoreCase("false")) {
            return Boolean.FALSE;
        }
        throw VesselConfigException.conversionFailed(key, trimmed, Boolean.class,
                new IllegalArgumentException("expected 'true' or 'false'"));
    }

    private Duration parseDuration(String key, String trimmed) {
        var matcher = DURATION_PATTERN.matcher(trimmed);
        if (!matcher.matches()) {
            throw VesselConfigException.invalidDuration(key, trimmed);
        }
        long amount = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2);
        return switch (unit == null ? "ms" : unit) {
            case "ns" -> Duration.ofNanos(amount);
            case "us" -> Duration.ofNanos(amount * 1000);
            case "ms" -> Duration.ofMillis(amount);
            case "s" -> Duration.ofSeconds(amount);
            case "m" -> Duration.ofMinutes(amount);
            case "h" -> Duration.ofHours(amount);
            case "d" -> Duration.ofDays(amount);
            default -> throw VesselConfigException.invalidDuration(key, trimmed);
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Enum<?> parseEnum(String key, String trimmed, Class<?> enumType) {
        for (Object constant : enumType.getEnumConstants()) {
            if (((Enum<?>) constant).name().equalsIgnoreCase(trimmed)) {
                return (Enum<?>) constant;
            }
        }
        List<String> validValues = Arrays.stream(enumType.getEnumConstants())
                .map(constant -> ((Enum<?>) constant).name())
                .toList();
        throw VesselConfigException.invalidEnumValue(key, trimmed, enumType, validValues);
    }
}
