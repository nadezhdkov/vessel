package io.vessel.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ContainerRegisterAndResolveSimpleTest {

    static class NoDependencies {
        public NoDependencies() {
        }
    }

    @Test
    void resolvesAClassWithNoConstructorParameters() {
        var container = new Container();
        container.register(NoDependencies.class);

        var resolved = container.get(NoDependencies.class);

        assertNotNull(resolved);
        assertInstanceOf(NoDependencies.class, resolved);
    }
}
