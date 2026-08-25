package io.vessel.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ContainerRecursiveResolutionTest {

    static class ServiceC {
        public ServiceC() {
        }
    }

    static class ServiceB {
        final ServiceC c;

        public ServiceB(ServiceC c) {
            this.c = c;
        }
    }

    static class ServiceA {
        final ServiceB b;
        final ServiceC c;

        public ServiceA(ServiceB b, ServiceC c) {
            this.b = b;
            this.c = c;
        }
    }

    @Test
    void resolvesADependencyTreeThreeLevelsDeep() {
        var container = new Container();
        container.register(ServiceA.class);
        container.register(ServiceB.class);
        container.register(ServiceC.class);

        var a = container.get(ServiceA.class);

        assertNotNull(a);
        assertNotNull(a.b);
        assertNotNull(a.b.c);
        assertNotNull(a.c);
    }

    @Test
    void reusesTheSameInstanceWhenReachedThroughTwoDifferentPaths() {
        var container = new Container();
        container.register(ServiceA.class);
        container.register(ServiceB.class);
        container.register(ServiceC.class);

        var a = container.get(ServiceA.class);

        // ServiceA depends on ServiceC directly and indirectly via ServiceB.
        // The container must dedupe: the same ServiceC instance on both paths.
        assertSame(a.b.c, a.c);
    }
}
