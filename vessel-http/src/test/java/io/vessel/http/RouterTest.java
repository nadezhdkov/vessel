package io.vessel.http;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouterTest {

    private static final Handler NOOP = request -> HttpResponse.ok("ok");

    @Test
    void anExactRouteWinsOverAParameterizedRouteForTheSameRequest() {
        var router = new Router()
                .register(new Route(HttpMethod.GET, "/users/{id}", request -> HttpResponse.ok("by id")))
                .register(new Route(HttpMethod.GET, "/users/me", request -> HttpResponse.ok("me")));

        var result = router.match(HttpMethod.GET, "/users/me");

        var matched = assertInstanceOf(RouteResult.Matched.class, result);
        var response = matched.handler().handle(dummyRequest());
        assertEquals("me", new String(response.body(), StandardCharsets.UTF_8));
        assertTrue(matched.pathVariables().isEmpty());
    }

    @Test
    void extractsASinglePathVariable() {
        var router = new Router().register(new Route(HttpMethod.GET, "/users/{id}", NOOP));

        var result = router.match(HttpMethod.GET, "/users/42");

        var matched = assertInstanceOf(RouteResult.Matched.class, result);
        assertEquals("42", matched.pathVariables().get("id"));
    }

    @Test
    void extractsMultiplePathVariables() {
        var router = new Router().register(new Route(HttpMethod.GET, "/users/{userId}/orders/{orderId}", NOOP));

        var result = router.match(HttpMethod.GET, "/users/7/orders/99");

        var matched = assertInstanceOf(RouteResult.Matched.class, result);
        assertEquals("7", matched.pathVariables().get("userId"));
        assertEquals("99", matched.pathVariables().get("orderId"));
    }

    @Test
    void routesReturnsEveryRegisteredRouteInRegistrationOrder() {
        var users = new Route(HttpMethod.GET, "/users", NOOP);
        var userById = new Route(HttpMethod.GET, "/users/{id}", NOOP);
        var router = new Router().register(users).register(userById);

        assertEquals(java.util.List.of(users, userById), router.routes());
    }

    @Test
    void returnsMethodNotAllowedWhenThePathExistsForADifferentMethod() {
        var router = new Router()
                .register(new Route(HttpMethod.GET, "/users", NOOP))
                .register(new Route(HttpMethod.POST, "/users", NOOP));

        var result = router.match(HttpMethod.DELETE, "/users");

        var notAllowed = assertInstanceOf(RouteResult.MethodNotAllowed.class, result);
        assertEquals(Set.of(HttpMethod.GET, HttpMethod.POST), notAllowed.allowedMethods());
    }

    @Test
    void returnsMethodNotAllowedWhenAParameterizedPathMatchesADifferentMethod() {
        var router = new Router().register(new Route(HttpMethod.GET, "/users/{id}", NOOP));

        var result = router.match(HttpMethod.DELETE, "/users/42");

        assertInstanceOf(RouteResult.MethodNotAllowed.class, result);
    }

    @Test
    void notFoundListsSiblingRoutesAndTheClosestSuggestionWhenSomeShareAPrefix() {
        var router = new Router()
                .register(new Route(HttpMethod.GET, "/api/users", NOOP))
                .register(new Route(HttpMethod.GET, "/api/users/{id}", NOOP))
                .register(new Route(HttpMethod.POST, "/api/users", NOOP));

        var result = router.match(HttpMethod.GET, "/api/users/42/orders");

        var notFound = assertInstanceOf(RouteResult.NotFound.class, result);
        assertTrue(notFound.message().contains("no handler for GET /api/users/42/orders"));
        assertTrue(notFound.message().contains("routes registered for /api:"));
        assertTrue(notFound.message().contains("GET   /api/users"));
        assertTrue(notFound.message().contains("GET   /api/users/{id}"));
        assertTrue(notFound.message().contains("POST  /api/users"));
        assertTrue(notFound.message().contains("did you mean: GET /api/users/{id} ?"));
    }

    @Test
    void notFoundIsAPlainMessageWhenNothingSharesAPrefix() {
        var router = new Router().register(new Route(HttpMethod.GET, "/api/users", NOOP));

        var result = router.match(HttpMethod.GET, "/health");

        var notFound = assertInstanceOf(RouteResult.NotFound.class, result);
        assertEquals("no handler for GET /health", notFound.message());
    }

    private HttpRequest dummyRequest() {
        return new HttpRequest(HttpMethod.GET, "/users/me", Map.of(), Map.of(), Map.of(), "");
    }
}
