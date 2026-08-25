package io.vessel.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CircularDependencyTest {

    static class DirectA {
        public DirectA(DirectB b) {
        }
    }

    static class DirectB {
        public DirectB(DirectA a) {
        }
    }

    static class IndirectA {
        public IndirectA(IndirectB b) {
        }
    }

    static class IndirectB {
        public IndirectB(IndirectC c) {
        }
    }

    static class IndirectC {
        public IndirectC(IndirectA a) {
        }
    }

    @Test
    void detectsADirectCycleBetweenTwoTypes() {
        var container = new Container();
        container.register(DirectA.class);
        container.register(DirectB.class);

        var exception = assertThrows(CircularDependencyException.class,
                () -> container.get(DirectA.class));

        assertTrue(exception.getMessage().contains("circular dependency detected"));
        assertTrue(exception.getMessage().contains("path: DirectA → DirectB → DirectA"));
    }

    @Test
    void detectsAnIndirectCycleThroughThreeTypes() {
        var container = new Container();
        container.register(IndirectA.class);
        container.register(IndirectB.class);
        container.register(IndirectC.class);

        var exception = assertThrows(CircularDependencyException.class,
                () -> container.get(IndirectA.class));

        assertTrue(exception.getMessage()
                .contains("path: IndirectA → IndirectB → IndirectC → IndirectA"));
    }
}
