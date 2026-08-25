package io.vessel.web;

import io.vessel.http.Response;
import io.vessel.web.annotation.ExceptionHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExceptionResolverTest {

    static class NarrowException extends RuntimeException {
    }

    static class ControllerWithHierarchy {
        @ExceptionHandler(NarrowException.class)
        public Response<String> handleNarrow(NarrowException e) {
            return Response.ok("narrow");
        }

        @ExceptionHandler
        public Response<String> handleGeneric(Exception e) {
            return Response.ok("generic");
        }
    }

    static class ControllerWithNoHandlers {
    }

    static class ControllerWithMismatchedValue {
        @ExceptionHandler(NarrowException.class)
        public Response<String> handle(IllegalStateException e) {
            return Response.ok("won't compile logically, but reflection doesn't care");
        }
    }

    static class ControllerWithBadSignature {
        @ExceptionHandler
        public Response<String> handle(String notAThrowable) {
            return Response.ok("bad");
        }
    }

    static class ControllerWithDuplicateHandlers {
        @ExceptionHandler(NarrowException.class)
        public Response<String> first(NarrowException e) {
            return Response.ok("first");
        }

        @ExceptionHandler(NarrowException.class)
        public Response<String> second(NarrowException e) {
            return Response.ok("second");
        }
    }

    @Test
    void resolvesTheMostSpecificHandlerForTheExactExceptionType() throws NoSuchMethodException {
        var handler = ExceptionResolver.resolve(ControllerWithHierarchy.class, NarrowException.class);

        assertEquals(ControllerWithHierarchy.class.getDeclaredMethod("handleNarrow", NarrowException.class), handler.orElseThrow());
    }

    @Test
    void fallsBackToAnAncestorHandlerWhenNoExactMatchExists() throws NoSuchMethodException {
        var handler = ExceptionResolver.resolve(ControllerWithHierarchy.class, IllegalStateException.class);

        assertEquals(ControllerWithHierarchy.class.getDeclaredMethod("handleGeneric", Exception.class), handler.orElseThrow());
    }

    @Test
    void returnsEmptyWhenTheControllerDeclaresNoHandlersAtAll() {
        var handler = ExceptionResolver.resolve(ControllerWithNoHandlers.class, RuntimeException.class);

        assertTrue(handler.isEmpty());
    }

    @Test
    void rejectsAnExplicitValueIncompatibleWithTheParameterType() {
        var exception = assertThrows(VesselWebException.class,
                () -> ExceptionResolver.validate(ControllerWithMismatchedValue.class));

        assertTrue(exception.getMessage().contains("is incompatible with its parameter type"));
    }

    @Test
    void rejectsAHandlerWhoseParameterIsNotAThrowable() {
        var exception = assertThrows(VesselWebException.class,
                () -> ExceptionResolver.validate(ControllerWithBadSignature.class));

        assertTrue(exception.getMessage().contains("must declare exactly one parameter of a Throwable subtype"));
    }

    @Test
    void rejectsTwoHandlersDeclaredForTheExactSameType() {
        var exception = assertThrows(VesselWebException.class,
                () -> ExceptionResolver.validate(ControllerWithDuplicateHandlers.class));

        assertTrue(exception.getMessage().contains("duplicate @ExceptionHandler for type 'NarrowException'"));
        assertTrue(exception.getMessage().contains("first"));
        assertTrue(exception.getMessage().contains("second"));
    }
}
