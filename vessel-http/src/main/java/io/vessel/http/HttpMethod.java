package io.vessel.http;

import java.util.Locale;

public enum HttpMethod {
    GET,
    POST,
    PUT,
    PATCH,
    DELETE,
    HEAD,
    OPTIONS;

    public static HttpMethod fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new VesselHttpException("HTTP method must not be null or blank");
        }
        try {
            return valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new VesselHttpException(
                    "unknown HTTP method: '%s' — supported methods: %s"
                            .formatted(value, String.join(", ", names())));
        }
    }

    private static String[] names() {
        var methods = values();
        var names = new String[methods.length];
        for (int i = 0; i < methods.length; i++) {
            names[i] = methods[i].name();
        }
        return names;
    }
}
