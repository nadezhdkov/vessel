package io.vessel.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vessel.core.annotation.Inject;

class InjectConstructorTest {

    static class Greeting {
        final String message;

        public Greeting() {
            this("hello");
        }

        @Inject
        public Greeting(String message) {
            this.message = message;
        }
    }

    static class NoInjectHint {
        public NoInjectHint(String name) {
        }

        public NoInjectHint(String name, int age) {
        }
    }

    static class DoubleInject {
        @Inject
        public DoubleInject(String name) {
        }

        @Inject
        public DoubleInject(String name, int age) {
        }
    }

    @Test
    void usesTheConstructorAnnotatedInjectWhenThereIsMoreThanOnePublicConstructor() {
        var container = new Container();
        container.register(String.class, "world");
        container.register(Greeting.class);

        var greeting = container.get(Greeting.class);

        assertEquals("world", greeting.message);
    }

    @Test
    void rejectsMultiplePublicConstructorsWithoutAnInjectHint() {
        var container = new Container();

        var exception = assertThrows(InvalidConstructorException.class,
                () -> container.register(NoInjectHint.class));

        assertTrue(exception.getMessage()
                .contains("'NoInjectHint' cannot be registered — found 2 public constructors"));
        assertTrue(exception.getMessage().contains("annotate one of them with @Inject"));
    }

    @Test
    void rejectsMoreThanOneConstructorAnnotatedInject() {
        var container = new Container();

        var exception = assertThrows(InvalidConstructorException.class,
                () -> container.register(DoubleInject.class));

        assertTrue(exception.getMessage()
                .contains("'DoubleInject' cannot be registered — found 2 constructors annotated with @Inject"));
    }
}
