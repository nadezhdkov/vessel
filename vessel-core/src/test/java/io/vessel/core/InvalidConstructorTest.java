package io.vessel.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvalidConstructorTest {

    static class NoPublicConstructor {
        private NoPublicConstructor() {
        }
    }

    static class AmbiguousConstructors {
        public AmbiguousConstructors(String name) {
        }

        public AmbiguousConstructors(String name, int age) {
        }
    }

    interface AbstractContract {
    }

    @Test
    void rejectsATypeWithNoPublicConstructor() {
        var container = new Container();

        var exception = assertThrows(InvalidConstructorException.class,
                () -> container.register(NoPublicConstructor.class));

        assertTrue(exception.getMessage()
                .contains("'NoPublicConstructor' cannot be registered — no public constructor found"));
    }

    @Test
    void rejectsATypeWithMoreThanOnePublicConstructor() {
        var container = new Container();

        var exception = assertThrows(InvalidConstructorException.class,
                () -> container.register(AmbiguousConstructors.class));

        assertTrue(exception.getMessage()
                .contains("'AmbiguousConstructors' cannot be registered — found 2 public constructors"));
        assertTrue(exception.getMessage().contains("AmbiguousConstructors(String)"));
        assertTrue(exception.getMessage().contains("AmbiguousConstructors(String, int)"));
    }

    @Test
    void rejectsAnInterfaceRegisteredWithoutAnInstance() {
        var container = new Container();

        var exception = assertThrows(InvalidConstructorException.class,
                () -> container.register(AbstractContract.class));

        assertTrue(exception.getMessage()
                .contains("'AbstractContract' is an interface — it cannot be registered directly"));
    }
}
