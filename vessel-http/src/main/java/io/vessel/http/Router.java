package io.vessel.http;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Registers {@link Route}s and matches incoming (method, path) pairs
 * against them. Knows nothing about annotations or scanning — it only ever
 * sees the {@link Route}s it's given, which is exactly what lets M6 build
 * an annotation scanner on top of this without this class changing at all.
 *
 * <p>Exact-path routes are always preferred over path-variable routes for
 * the same request, regardless of registration order (a lookup, not the
 * first match found).
 */
public final class Router {

    private final Map<HttpMethod, Map<String, Handler>> exactRoutes = new EnumMap<>(HttpMethod.class);
    private final Map<HttpMethod, List<Route>> parameterizedRoutes = new EnumMap<>(HttpMethod.class);
    private final List<Route> allRoutes = new ArrayList<>();

    public Router register(Route route) {
        Objects.requireNonNull(route, "route must not be null");
        allRoutes.add(route);
        if (route.hasPathVariables()) {
            parameterizedRoutes.computeIfAbsent(route.method(), unused -> new ArrayList<>()).add(route);
        } else {
            exactRoutes.computeIfAbsent(route.method(), unused -> new LinkedHashMap<>())
                    .put(normalize(route.path()), route.handler());
        }
        return this;
    }

    /** Every route registered so far, in registration order — used for startup logging, not matching. */
    public List<Route> routes() {
        return List.copyOf(allRoutes);
    }

    public RouteResult match(HttpMethod method, String path) {
        Objects.requireNonNull(method, "method must not be null");
        Objects.requireNonNull(path, "path must not be null");

        var normalized = normalize(path);
        var requestSegments = segments(normalized);

        var exactHandler = exactRoutes.getOrDefault(method, Map.of()).get(normalized);
        if (exactHandler != null) {
            return new RouteResult.Matched(exactHandler, Map.of());
        }

        for (Route route : parameterizedRoutes.getOrDefault(method, List.of())) {
            var variables = tryMatch(segments(route.path()), requestSegments);
            if (variables != null) {
                return new RouteResult.Matched(route.handler(), variables);
            }
        }

        var allowedMethods = allowedMethodsFor(normalized, requestSegments);
        if (!allowedMethods.isEmpty()) {
            return new RouteResult.MethodNotAllowed(allowedMethods);
        }

        return new RouteResult.NotFound(notFoundMessage(method, path, requestSegments));
    }

    private Set<HttpMethod> allowedMethodsFor(String normalizedPath, List<String> requestSegments) {
        var allowed = new LinkedHashSet<HttpMethod>();
        for (Route route : allRoutes) {
            boolean matches = route.hasPathVariables()
                    ? tryMatch(segments(route.path()), requestSegments) != null
                    : normalize(route.path()).equals(normalizedPath);
            if (matches) {
                allowed.add(route.method());
            }
        }
        return allowed;
    }

    private Map<String, String> tryMatch(List<String> patternSegments, List<String> requestSegments) {
        if (patternSegments.size() != requestSegments.size()) {
            return null;
        }
        var variables = new LinkedHashMap<String, String>();
        for (int i = 0; i < patternSegments.size(); i++) {
            var patternSegment = patternSegments.get(i);
            if (isVariable(patternSegment)) {
                variables.put(variableName(patternSegment), urlDecode(requestSegments.get(i)));
            } else if (!patternSegment.equals(requestSegments.get(i))) {
                return null;
            }
        }
        return variables;
    }

    private String notFoundMessage(HttpMethod method, String originalPath, List<String> requestSegments) {
        var header = "no handler for %s %s".formatted(method, originalPath);

        var related = allRoutes.stream()
                .filter(route -> prefixMatchScore(segments(route.path()), requestSegments) > 0)
                .toList();
        if (related.isEmpty()) {
            return header;
        }

        var firstSegment = requestSegments.isEmpty() ? "/" : "/" + requestSegments.get(0);
        var methodWidth = related.stream().mapToInt(route -> route.method().name().length()).max().orElse(0);

        var routesList = related.stream()
                .map(route -> ("    %-" + methodWidth + "s  %s").formatted(route.method(), route.path()))
                .collect(Collectors.joining(System.lineSeparator()));

        var bestMatch = related.stream()
                .max((a, b) -> Integer.compare(
                        prefixMatchScore(segments(a.path()), requestSegments),
                        prefixMatchScore(segments(b.path()), requestSegments)))
                .orElseThrow();

        return ("%s%n"
                + "  routes registered for %s:%n"
                + "%s%n"
                + "  did you mean: %s %s ?")
                .formatted(header, firstSegment, routesList, bestMatch.method(), bestMatch.path());
    }

    private static int prefixMatchScore(List<String> patternSegments, List<String> requestSegments) {
        int limit = Math.min(patternSegments.size(), requestSegments.size());
        int score = 0;
        for (int i = 0; i < limit; i++) {
            var patternSegment = patternSegments.get(i);
            boolean matches = isVariable(patternSegment) || patternSegment.equals(requestSegments.get(i));
            if (!matches) {
                break;
            }
            score++;
        }
        return score;
    }

    private static boolean isVariable(String segment) {
        return segment.startsWith("{") && segment.endsWith("}") && segment.length() > 2;
    }

    private static String variableName(String segment) {
        return segment.substring(1, segment.length() - 1);
    }

    private static List<String> segments(String path) {
        var trimmed = normalize(path);
        if (trimmed.equals("/")) {
            return List.of();
        }
        return List.of(trimmed.substring(1).split("/"));
    }

    private static String normalize(String path) {
        if (path.length() > 1 && path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path.isEmpty() ? "/" : path;
    }

    private static String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
