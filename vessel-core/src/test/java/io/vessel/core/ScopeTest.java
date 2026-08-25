package io.vessel.core;

import io.vessel.core.annotation.Scope;
import io.vessel.core.annotation.ScopeType;
import io.vessel.core.scan.fixtures.prototype.RequestContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScopeTest {

    @Scope(ScopeType.PROTOTYPE)
    static class PrototypeA {
        public PrototypeA(PrototypeB b) {
        }
    }

    @Scope(ScopeType.PROTOTYPE)
    static class PrototypeB {
        public PrototypeB(PrototypeA a) {
        }
    }

    @Test
    void prototypeScopeCreatesANewInstanceOnEveryGet() {
        var container = new Container();
        container.scan("io.vessel.core.scan.fixtures.prototype");

        var first = container.get(RequestContext.class);
        var second = container.get(RequestContext.class);

        assertNotSame(first, second);
    }

    @Test
    void singletonIsStillTheDefaultAndIsCachedAcrossGets() {
        var container = new Container();

        container.register(Object.class);

        var first = container.get(Object.class);
        var second = container.get(Object.class);

        assertSame(first, second);
    }

    @Test
    void cycleDetectionStillWorksWhenBothTypesArePrototypeScoped() {
        var container = new Container();
        container.register(PrototypeA.class);
        container.register(PrototypeB.class);

        assertThrows(CircularDependencyException.class, () -> container.get(PrototypeA.class));
    }
}
