package io.vessel.web;

import io.vessel.http.HttpRequest;
import io.vessel.web.annotation.PathVariable;
import io.vessel.web.annotation.RequestBody;
import io.vessel.web.annotation.RequestParam;
import io.vessel.web.json.JsonSerializer;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Converts each parameter of a controller method into the value it needs at
 * call time, based on which of {@link PathVariable}/{@link RequestParam}/
 * {@link RequestBody} annotates it.
 */
public final class ArgumentResolver {

    private ArgumentResolver() {
    }

    public static Object[] resolve(Method method, HttpRequest request) {
        var parameters = method.getParameters();
        var args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            args[i] = resolveParameter(method, parameters[i], request);
        }
        return args;
    }

    private static Object resolveParameter(Method method, Parameter parameter, HttpRequest request) {
        var pathVariable = parameter.getAnnotation(PathVariable.class);
        if (pathVariable != null) {
            var name = effectiveName(pathVariable.value(), parameter);
            var raw = request.pathVariable(name).orElseThrow(() -> new VesselWebException(
                    "no path variable named '%s' for parameter '%s' of %s.%s — check that the route path declares '{%s}'"
                            .formatted(name, parameter.getName(), method.getDeclaringClass().getSimpleName(),
                                    method.getName(), name)));
            return convertScalar(raw, parameter.getType(), name, method, parameter);
        }

        var requestParam = parameter.getAnnotation(RequestParam.class);
        if (requestParam != null) {
            var name = effectiveName(requestParam.value(), parameter);
            var raw = request.queryParam(name).orElseThrow(() -> new VesselWebException(
                    "no query parameter named '%s' for parameter '%s' of %s.%s"
                            .formatted(name, parameter.getName(), method.getDeclaringClass().getSimpleName(), method.getName())));
            return convertScalar(raw, parameter.getType(), name, method, parameter);
        }

        if (parameter.isAnnotationPresent(RequestBody.class)) {
            return JsonSerializer.deserialize(request.body(), parameter.getType());
        }

        throw new VesselWebException(
                "parameter '%s' of %s.%s has no resolvable source — annotate it with @PathVariable, @RequestParam, or @RequestBody"
                        .formatted(parameter.getName(), method.getDeclaringClass().getSimpleName(), method.getName()));
    }

    private static String effectiveName(String declaredValue, Parameter parameter) {
        return declaredValue.isEmpty() ? parameter.getName() : declaredValue;
    }

    private static Object convertScalar(String raw, Class<?> targetType, String name, Method method, Parameter parameter) {
        try {
            if (targetType == String.class) {
                return raw;
            }
            if (targetType == int.class || targetType == Integer.class) {
                return Integer.parseInt(raw);
            }
            if (targetType == long.class || targetType == Long.class) {
                return Long.parseLong(raw);
            }
            if (targetType == double.class || targetType == Double.class) {
                return Double.parseDouble(raw);
            }
            if (targetType == boolean.class || targetType == Boolean.class) {
                return Boolean.parseBoolean(raw);
            }
        } catch (NumberFormatException e) {
            throw new VesselWebException(
                    "cannot convert '%s' value '%s' to %s for parameter '%s' of %s.%s"
                            .formatted(name, raw, targetType.getSimpleName(), parameter.getName(),
                                    method.getDeclaringClass().getSimpleName(), method.getName()));
        }
        throw new VesselWebException(
                "unsupported parameter type '%s' for '%s' of %s.%s — @PathVariable/@RequestParam support String, int/long/double/boolean"
                        .formatted(targetType.getName(), parameter.getName(),
                                method.getDeclaringClass().getSimpleName(), method.getName()));
    }
}
