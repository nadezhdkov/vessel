package io.vessel.core;

import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupplierLazyInjectionTest {

    static class EventBus {
        private final Supplier<Handler> handlerSupplier;

        public EventBus(Supplier<Handler> handlerSupplier) {
            this.handlerSupplier = handlerSupplier;
        }

        Handler handler() {
            return handlerSupplier.get();
        }
    }

    static class Handler {
        private final EventBus eventBus;

        public Handler(EventBus eventBus) {
            this.eventBus = eventBus;
        }

        EventBus eventBus() {
            return eventBus;
        }
    }

    static class DirectEventBus {
        public DirectEventBus(DirectHandler handler) {
        }
    }

    static class DirectHandler {
        public DirectHandler(DirectEventBus eventBus) {
        }
    }

    static class RawSupplierUser {
        @SuppressWarnings("rawtypes")
        public RawSupplierUser(Supplier dependency) {
        }
    }

    @Test
    void aSupplierParameterBreaksACycleThatWouldOtherwiseBeUnresolvable() {
        var container = new Container().register(EventBus.class).register(Handler.class);

        var eventBus = container.get(EventBus.class);
        var handler = eventBus.handler();

        assertSame(eventBus, handler.eventBus());
    }

    @Test
    void resolvingFromEitherSideOfTheCycleFirstStillWorks() {
        var container = new Container().register(EventBus.class).register(Handler.class);

        var handler = container.get(Handler.class);

        assertSame(handler, handler.eventBus().handler());
    }

    @Test
    void theSameCycleWithoutAnySupplierStillFailsAsBeforeProvingSupplierIsWhatFixesIt() {
        var container = new Container().register(DirectEventBus.class).register(DirectHandler.class);

        var exception = assertThrows(CircularDependencyException.class, () -> container.get(DirectEventBus.class));

        assertTrue(exception.getMessage().contains("DirectEventBus"));
        assertTrue(exception.getMessage().contains("DirectHandler"));
    }

    @Test
    void aRawSupplierWithNoTypeArgumentIsRejectedWithAClearMessage() {
        var container = new Container().register(RawSupplierUser.class);

        var exception = assertThrows(VesselException.class, () -> container.get(RawSupplierUser.class));

        assertTrue(exception.getMessage().contains("RawSupplierUser"));
        assertTrue(exception.getMessage().contains("Supplier"));
    }
}
