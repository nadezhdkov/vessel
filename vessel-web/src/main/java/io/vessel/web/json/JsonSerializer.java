package io.vessel.web.json;

import io.vessel.web.VesselWebException;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A didactic, reflection-based JSON serializer — no Jackson, by design (see
 * docs/vessel-web.md). Serialization supports {@code String}, primitive
 * wrappers, {@code boolean}, {@code record} (via {@link RecordComponent}),
 * {@code List<T>}, {@code Map<String,T>}, and plain getter-based objects
 * (any {@code getXxx()}/{@code isXxx()} no-arg method, JavaBean-style).
 * Deserialization stays narrower on purpose — {@code record} and
 * primitives only — since {@code @RequestBody} never needed more than that.
 */
public final class JsonSerializer {

    private JsonSerializer() {
    }

    public static String serialize(Object value) {
        var out = new StringBuilder();
        writeValue(value, out);
        return out.toString();
    }

    public static <T> T deserialize(String json, Class<T> targetType) {
        Objects.requireNonNull(json, "json must not be null");
        Objects.requireNonNull(targetType, "targetType must not be null");
        Object tree;
        try {
            tree = JsonParser.parse(json);
        } catch (VesselWebException e) {
            throw new VesselWebException("failed to parse JSON: " + e.getMessage(), e);
        }
        return bind(tree, targetType);
    }

    private static void writeValue(Object value, StringBuilder out) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String string) {
            writeString(string, out);
        } else if (value instanceof Boolean bool) {
            out.append(bool);
        } else if (value instanceof Number number) {
            out.append(number);
        } else if (value instanceof List<?> list) {
            writeArray(list, out);
        } else if (value instanceof Map<?, ?> map) {
            writeMap(map, out);
        } else if (value.getClass().isRecord()) {
            writeRecord(value, out);
        } else {
            writeBean(value, out);
        }
    }

    private static void writeString(String value, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (c < 0x20) {
                        out.append("\\u%04x".formatted((int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }

    private static void writeArray(List<?> list, StringBuilder out) {
        out.append('[');
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                out.append(',');
            }
            writeValue(list.get(i), out);
        }
        out.append(']');
    }

    private static void writeRecord(Object record, StringBuilder out) {
        var components = record.getClass().getRecordComponents();
        out.append('{');
        for (int i = 0; i < components.length; i++) {
            if (i > 0) {
                out.append(',');
            }
            var component = components[i];
            writeString(component.getName(), out);
            out.append(':');
            try {
                var accessor = component.getAccessor();
                accessor.setAccessible(true);
                writeValue(accessor.invoke(record), out);
            } catch (ReflectiveOperationException e) {
                throw new VesselWebException(
                        "failed to read record component '%s' of '%s': %s"
                                .formatted(component.getName(), record.getClass().getSimpleName(), e.getMessage()), e);
            }
        }
        out.append('}');
    }

    private static void writeMap(Map<?, ?> map, StringBuilder out) {
        out.append('{');
        boolean first = true;
        for (var entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new VesselWebException(
                        "cannot serialize a Map with a non-String key '%s' (%s) to JSON — only Map<String, T> is supported"
                                .formatted(entry.getKey(),
                                        entry.getKey() == null ? "null" : entry.getKey().getClass().getName()));
            }
            if (!first) {
                out.append(',');
            }
            first = false;
            writeString(key, out);
            out.append(':');
            writeValue(entry.getValue(), out);
        }
        out.append('}');
    }

    private static void writeBean(Object bean, StringBuilder out) {
        var properties = beanProperties(bean.getClass());
        if (properties.isEmpty()) {
            throw new VesselWebException(
                    "cannot serialize instance of '%s' to JSON — no getters found, and it is not a String, primitive wrapper, boolean, record, List<T>, or Map<String,T>"
                            .formatted(bean.getClass().getName()));
        }
        out.append('{');
        for (int i = 0; i < properties.size(); i++) {
            if (i > 0) {
                out.append(',');
            }
            var property = properties.get(i);
            writeString(property.name(), out);
            out.append(':');
            try {
                writeValue(property.getter().invoke(bean), out);
            } catch (ReflectiveOperationException e) {
                throw new VesselWebException(
                        "failed to read property '%s' of '%s': %s"
                                .formatted(property.name(), bean.getClass().getSimpleName(), e.getMessage()), e);
            }
        }
        out.append('}');
    }

    private record BeanProperty(String name, Method getter) {
    }

    private static List<BeanProperty> beanProperties(Class<?> type) {
        var properties = new ArrayList<BeanProperty>();
        for (Method method : type.getMethods()) {
            if (method.getParameterCount() != 0 || method.getName().equals("getClass")) {
                continue;
            }
            var name = method.getName();
            if (name.length() > 3 && name.startsWith("get") && method.getReturnType() != void.class) {
                properties.add(new BeanProperty(decapitalize(name.substring(3)), method));
            } else if (name.length() > 2 && name.startsWith("is")
                    && (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class)) {
                properties.add(new BeanProperty(decapitalize(name.substring(2)), method));
            }
        }
        properties.sort(Comparator.comparing(BeanProperty::name));
        return properties;
    }

    private static String decapitalize(String value) {
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    @SuppressWarnings("unchecked")
    private static <T> T bind(Object value, Class<T> targetType) {
        if (targetType.isRecord()) {
            if (!(value instanceof Map<?, ?> jsonObject)) {
                throw new VesselWebException(
                        "cannot bind JSON value to record '%s' — expected a JSON object, found %s"
                                .formatted(targetType.getSimpleName(), describeJsonType(value)));
            }
            return (T) bindRecord(jsonObject, targetType);
        }
        return (T) coerceScalar(value, targetType);
    }

    private static Object bindRecord(Map<?, ?> jsonObject, Class<?> targetType) {
        var components = targetType.getRecordComponents();
        var paramTypes = new Class<?>[components.length];
        var args = new Object[components.length];
        for (int i = 0; i < components.length; i++) {
            RecordComponent component = components[i];
            paramTypes[i] = component.getType();
            if (!jsonObject.containsKey(component.getName()) && component.getType().isPrimitive()) {
                throw new VesselWebException(
                        "missing required field '%s' for record '%s' — %s is a primitive type and cannot be null"
                                .formatted(component.getName(), targetType.getSimpleName(), component.getType().getSimpleName()));
            }
            var rawValue = jsonObject.get(component.getName());
            args[i] = component.getType().isRecord() && rawValue != null
                    ? bindRecord((Map<?, ?>) rawValue, component.getType())
                    : coerceScalar(rawValue, component.getType());
        }
        try {
            var constructor = targetType.getDeclaredConstructor(paramTypes);
            constructor.setAccessible(true);
            return constructor.newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new VesselWebException(
                    "failed to construct record '%s' from JSON: %s".formatted(targetType.getSimpleName(), e.getMessage()), e);
        }
    }

    private static Object coerceScalar(Object rawValue, Class<?> targetType) {
        if (rawValue == null) {
            return null;
        }
        if (targetType == String.class) {
            if (!(rawValue instanceof String)) {
                throw new VesselWebException(
                        "cannot bind JSON value to String — found %s".formatted(describeJsonType(rawValue)));
            }
            return rawValue;
        }
        if (targetType == boolean.class || targetType == Boolean.class) {
            if (!(rawValue instanceof Boolean)) {
                throw new VesselWebException(
                        "cannot bind JSON value to boolean — found %s".formatted(describeJsonType(rawValue)));
            }
            return rawValue;
        }
        if (!(rawValue instanceof Number number)) {
            throw new VesselWebException(
                    "cannot bind JSON value to %s — found %s".formatted(targetType.getSimpleName(), describeJsonType(rawValue)));
        }
        if (targetType == int.class || targetType == Integer.class) {
            return number.intValue();
        }
        if (targetType == long.class || targetType == Long.class) {
            return number.longValue();
        }
        if (targetType == double.class || targetType == Double.class) {
            return number.doubleValue();
        }
        throw new VesselWebException(
                "cannot deserialize JSON into unsupported type '%s' — @RequestBody only supports String, int/long/double/boolean (wrapper or primitive), and record"
                        .formatted(targetType.getName()));
    }

    private static String describeJsonType(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Map) {
            return "an object";
        }
        if (value instanceof List) {
            return "an array";
        }
        if (value instanceof String) {
            return "a string";
        }
        if (value instanceof Boolean) {
            return "a boolean";
        }
        if (value instanceof Number) {
            return "a number";
        }
        return value.getClass().getSimpleName();
    }
}
