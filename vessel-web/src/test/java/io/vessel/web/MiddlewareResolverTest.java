package io.vessel.web;

import io.vessel.http.HttpRequest;
import io.vessel.http.HttpResponse;
import io.vessel.web.annotation.After;
import io.vessel.web.annotation.Before;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiddlewareResolverTest {

    static class ControllerWithBoth {
        @Before
        public HttpResponse before(HttpRequest request) {
            return null;
        }

        @After
        public HttpResponse after(HttpRequest request, HttpResponse response) {
            return response;
        }
    }

    static class ControllerWithNeither {
    }

    static class ControllerWithDuplicateBefore {
        @Before
        public HttpResponse first(HttpRequest request) {
            return null;
        }

        @Before
        public HttpResponse second(HttpRequest request) {
            return null;
        }
    }

    static class ControllerWithDuplicateAfter {
        @After
        public HttpResponse first(HttpRequest request, HttpResponse response) {
            return response;
        }

        @After
        public HttpResponse second(HttpRequest request, HttpResponse response) {
            return response;
        }
    }

    static class ControllerWithBadBeforeSignature {
        @Before
        public String before(HttpRequest request) {
            return "wrong return type";
        }
    }

    static class ControllerWithBadAfterSignature {
        @After
        public HttpResponse after(HttpRequest request) {
            return null;
        }
    }

    @Test
    void resolvesTheBeforeAndAfterMethodsWhenBothArePresent() throws NoSuchMethodException {
        var before = MiddlewareResolver.beforeMethodOf(ControllerWithBoth.class);
        var after = MiddlewareResolver.afterMethodOf(ControllerWithBoth.class);

        assertEquals(ControllerWithBoth.class.getDeclaredMethod("before", HttpRequest.class), before.orElseThrow());
        assertEquals(ControllerWithBoth.class.getDeclaredMethod("after", HttpRequest.class, HttpResponse.class), after.orElseThrow());
    }

    @Test
    void returnsEmptyWhenTheControllerDeclaresNeither() {
        assertTrue(MiddlewareResolver.beforeMethodOf(ControllerWithNeither.class).isEmpty());
        assertTrue(MiddlewareResolver.afterMethodOf(ControllerWithNeither.class).isEmpty());
    }

    @Test
    void rejectsTwoBeforeMethodsOnTheSameController() {
        var exception = assertThrows(VesselWebException.class,
                () -> MiddlewareResolver.validate(ControllerWithDuplicateBefore.class));

        assertTrue(exception.getMessage().contains("duplicate @Before"));
        assertTrue(exception.getMessage().contains("first"));
        assertTrue(exception.getMessage().contains("second"));
    }

    @Test
    void rejectsTwoAfterMethodsOnTheSameController() {
        var exception = assertThrows(VesselWebException.class,
                () -> MiddlewareResolver.validate(ControllerWithDuplicateAfter.class));

        assertTrue(exception.getMessage().contains("duplicate @After"));
    }

    @Test
    void rejectsABeforeMethodWithTheWrongSignature() {
        var exception = assertThrows(VesselWebException.class,
                () -> MiddlewareResolver.validate(ControllerWithBadBeforeSignature.class));

        assertTrue(exception.getMessage().contains("HttpResponse before(HttpRequest)"));
    }

    @Test
    void rejectsAnAfterMethodWithTheWrongSignature() {
        var exception = assertThrows(VesselWebException.class,
                () -> MiddlewareResolver.validate(ControllerWithBadAfterSignature.class));

        assertTrue(exception.getMessage().contains("HttpResponse after(HttpRequest, HttpResponse)"));
    }
}
